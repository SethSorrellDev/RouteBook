"""Tests for seed_sample.py against an in-memory stub of the RouteBook API.

Stdlib only:  python3 -m unittest scripts/test_seed_sample.py

This verifies the seeder's own logic (idempotency, ordering, the exactly-one
of routeId/stopId rule, auth header, removal). It does not exercise the real
Spring backend.
"""
import json
import re
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import seed_sample as ss  # noqa: E402

TOKEN = "stub-token"


class Stub(BaseHTTPRequestHandler):
    state = None

    def log_message(self, *a):
        pass

    def _send(self, code, obj=None):
        body = json.dumps(obj).encode() if obj is not None else b""
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle(self, method):
        st = Stub.state
        path = self.path.split("?")[0]
        query = dict(p.split("=") for p in self.path.split("?")[1].split("&")) if "?" in self.path else {}
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        if method != "GET" and path != "/auth/login" and self.headers.get("Authorization") != f"Bearer {TOKEN}":
            return self._send(403, {"message": "forbidden"})
        ctype = self.headers.get("Content-Type", "")
        body = json.loads(raw) if raw and ctype.startswith("application/json") else None

        if path == "/auth/login":
            ok = body == {"email": "w@example.com", "password": "pw"}
            return self._send(200, {"accessToken": TOKEN}) if ok else self._send(401, {"message": "bad"})
        if path == "/api/locations":
            if method == "GET":
                return self._send(200, st["locations"])
            loc = dict(body, id=len(st["locations"]) + 1)
            st["locations"].append(loc)
            return self._send(201, loc)
        if path == "/api/routes":
            if method == "GET":
                return self._send(200, st["routes"])
            st["seq"] += 1
            r = dict(body, id=st["seq"])
            st["routes"].append(r)
            return self._send(201, r)
        m = re.fullmatch(r"/api/routes/(\d+)", path)
        if m and method == "DELETE":
            rid = int(m.group(1))
            st["routes"] = [r for r in st["routes"] if r["id"] != rid]
            gone = {s["id"] for s in st["stops"] if s["routeId"] == rid}
            st["stops"] = [s for s in st["stops"] if s["routeId"] != rid]
            st["entries"] = [e for e in st["entries"] if e["routeId"] != rid and e["stopId"] not in gone]
            return self._send(204)
        m = re.fullmatch(r"/api/routes/(\d+)/stops", path)
        if m:
            rid = int(m.group(1))
            if method == "GET":
                return self._send(200, [s for s in st["stops"] if s["routeId"] == rid])
            st["seq"] += 1
            s = dict(body, id=st["seq"], routeId=rid)
            st["stops"].append(s)
            return self._send(201, s)
        if path == "/api/knowledge-entries":
            if method == "GET":
                out = st["entries"]
                if "routeId" in query:
                    out = [e for e in out if e["routeId"] == int(query["routeId"])]
                if "stopId" in query:
                    out = [e for e in out if e["stopId"] == int(query["stopId"])]
                return self._send(200, out)
            if (body.get("routeId") is None) == (body.get("stopId") is None):
                return self._send(400, {"message": "exactly one of routeId or stopId"})
            st["seq"] += 1
            e = dict(body, id=st["seq"])
            st["entries"].append(e)
            return self._send(201, e)
        m = re.fullmatch(r"/api/knowledge-entries/(\d+)/attachments", path)
        if m:
            eid = int(m.group(1))
            if method == "GET":
                return self._send(200, [a for a in st["attachments"] if a["knowledgeEntryId"] == eid])
            assert raw.startswith(b"--") and b"\x89PNG" in raw
            name = re.search(rb'filename="([^"]+)"', raw).group(1).decode()
            st["attachments"].append({"fileName": name, "knowledgeEntryId": eid})
            return self._send(201, {"fileName": name})
        self._send(404, {"message": "not found"})

    def do_GET(self): self._handle("GET")
    def do_POST(self): self._handle("POST")
    def do_DELETE(self): self._handle("DELETE")


class SeedSampleTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = HTTPServer(("127.0.0.1", 0), Stub)
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()
        cls.data = json.loads(ss.DATA_FILE.read_text())

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()

    def setUp(self):
        Stub.state = {"routes": [], "stops": [], "locations": [], "entries": [],
                      "attachments": [], "seq": 0}
        self.api = ss.Client(self.url, ss.login(self.url, "w@example.com", "pw"))

    def test_bad_login_raises(self):
        with self.assertRaises(ss.ApiError):
            ss.login(self.url, "w@example.com", "wrong")

    def test_seed_creates_everything(self):
        r = ss.seed(self.api, self.data)
        self.assertEqual((r["routes"], r["stops"], r["locations"]), (2, 6, 6))
        self.assertEqual(r["entries"], 14)
        self.assertEqual(r["attachments"], 1)

    def test_seed_is_idempotent(self):
        ss.seed(self.api, self.data)
        again = ss.seed(self.api, self.data)
        self.assertEqual(again, {"routes": 0, "stops": 0, "locations": 0, "entries": 0, "attachments": 0})
        self.assertEqual(len(Stub.state["entries"]), 14)

    def test_every_entry_targets_exactly_one_parent(self):
        ss.seed(self.api, self.data)
        for e in Stub.state["entries"]:
            self.assertNotEqual(e["routeId"] is None, e["stopId"] is None)

    def test_no_attachments_flag(self):
        r = ss.seed(self.api, self.data, attachments=False)
        self.assertEqual(r["attachments"], 0)

    def test_writes_need_the_token(self):
        anon = ss.Client(self.url)
        with self.assertRaises(ss.ApiError):
            anon.request("POST", "/api/routes", {"name": "x", "description": "", "driverId": None})

    def test_remove_deletes_sample_routes_only(self):
        self.api.request("POST", "/api/routes", {"name": "Real route", "description": "", "driverId": None})
        ss.seed(self.api, self.data)
        out = ss.remove(self.api, self.data)
        self.assertEqual(out, {"routes_removed": 2})
        self.assertEqual([r["name"] for r in Stub.state["routes"]], ["Real route"])
        self.assertEqual(Stub.state["stops"], [])
        self.assertEqual(Stub.state["entries"], [])

    def test_reseed_after_remove_reuses_locations(self):
        ss.seed(self.api, self.data)
        ss.remove(self.api, self.data)
        r = ss.seed(self.api, self.data)
        self.assertEqual(r["locations"], 0)
        self.assertEqual(len(Stub.state["locations"]), 6)

    def test_data_is_clearly_fictional(self):
        text = json.dumps(self.data)
        for real in ("Nucor", "Pace Dairy", "Chipotle", "Cintas", "4471", "84687"):
            self.assertNotIn(real, text)
        for m in re.findall(r"555-\d{4}", text):
            self.assertTrue(m.startswith("555-01"))

    def test_generated_png_is_valid(self):
        png = ss.make_png()
        self.assertTrue(png.startswith(b"\x89PNG\r\n\x1a\n"))
        self.assertTrue(png.endswith(b"IEND\xaeB`\x82"))


if __name__ == "__main__":
    unittest.main()
