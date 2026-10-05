#!/usr/bin/env python3
"""Load a fully fictional sample data set into a running RouteBook backend.

Everything in scripts/sample_data.json is invented: business names, street
addresses, phone numbers (555-01xx) and every "code". Nothing here is real.

It talks to the public REST API, signing in through identity-service first, so
it works against local dev and the live deployment alike. The account you sign
in with must be a RouteBook writer (its `sub` is in ADMIN_SUBJECTS).

    export ROUTEBOOK_URL=https://routebook-da3w.onrender.com
    export IDENTITY_URL=https://identity-service-c5ab.onrender.com
    export SEED_EMAIL=you@example.com
    export SEED_PASSWORD=...            # or leave unset to be prompted
    python3 scripts/seed_sample.py                # add (safe to re-run)
    python3 scripts/seed_sample.py --remove       # delete the sample routes
    python3 scripts/seed_sample.py --no-attachments

Standard library only. Re-running skips anything that already exists (matched
by route name, stop name, entry title and attachment file name). --remove
deletes the sample routes, which cascades to their stops, entries and
attachments. Locations have no delete endpoint, so --remove leaves them behind;
re-seeding reuses them by address instead of duplicating.
"""
import getpass
import json
import os
import struct
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib
from pathlib import Path

DATA_FILE = Path(__file__).with_name("sample_data.json")
ATTACHMENT_NAME = "sample-gate-photo.png"


class ApiError(RuntimeError):
    pass


def make_png(width=320, height=200):
    """A small generated PNG (diagonal stripes), so no binary lives in the repo."""
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            band = ((x + y) // 20) % 2
            row += bytes((235, 160, 60) if band else (60, 90, 140))
        rows.append(bytes(row))
    raw = zlib.compress(b"".join(rows), 9)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", raw) + chunk(b"IEND", b""))


class Client:
    def __init__(self, base_url, token=None):
        self.base = base_url.rstrip("/")
        self.token = token

    def request(self, method, path, body=None, raw=None, headers=None):
        data, hdrs = None, {"Accept": "application/json"}
        if self.token:
            hdrs["Authorization"] = f"Bearer {self.token}"
        if body is not None:
            data = json.dumps(body).encode()
            hdrs["Content-Type"] = "application/json"
        if raw is not None:
            data = raw
        hdrs.update(headers or {})
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=hdrs)
        try:
            with urllib.request.urlopen(req, timeout=90) as resp:
                text = resp.read().decode()
                return json.loads(text) if text else None
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")[:300]
            raise ApiError(f"{method} {path} -> {e.code}: {detail}") from None
        except urllib.error.URLError as e:
            raise ApiError(f"{method} {path} failed: {e.reason}") from None


def login(identity_url, email, password):
    resp = Client(identity_url).request("POST", "/auth/login", {"email": email, "password": password})
    return resp["accessToken"]


def multipart(field, filename, content_type, payload):
    boundary = uuid.uuid4().hex
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; "
            f"filename=\"{filename}\"\r\nContent-Type: {content_type}\r\n\r\n").encode()
    body += payload + f"\r\n--{boundary}--\r\n".encode()
    return body, {"Content-Type": f"multipart/form-data; boundary={boundary}"}


def ensure_location(api, loc, report):
    for existing in api.request("GET", "/api/locations") or []:
        if (existing["addressLine1"], existing["zipCode"]) == (loc["addressLine1"], loc["zipCode"]):
            return existing["id"]
    created = api.request("POST", "/api/locations", loc)
    report["locations"] += 1
    return created["id"]


def ensure_entry(api, note, target, existing_entries, report, attachments):
    entry = next((e for e in existing_entries if e["title"] == note["title"]), None)
    if entry is None:
        payload = {"title": note["title"], "body": note["body"], "category": note["category"],
                   "routeId": None, "stopId": None}
        payload.update(target)
        entry = api.request("POST", "/api/knowledge-entries", payload)
        report["entries"] += 1
    if note.get("attach") and attachments:
        have = api.request("GET", f"/api/knowledge-entries/{entry['id']}/attachments") or []
        if not any(a["fileName"] == ATTACHMENT_NAME for a in have):
            body, hdrs = multipart("file", ATTACHMENT_NAME, "image/png", make_png())
            api.request("POST", f"/api/knowledge-entries/{entry['id']}/attachments", raw=body, headers=hdrs)
            report["attachments"] += 1


def seed(api, data, attachments=True):
    report = {"routes": 0, "stops": 0, "locations": 0, "entries": 0, "attachments": 0}
    routes = api.request("GET", "/api/routes") or []
    for rdef in data["routes"]:
        route = next((r for r in routes if r["name"] == rdef["name"]), None)
        if route is None:
            route = api.request("POST", "/api/routes", {"name": rdef["name"],
                                                        "description": rdef["description"],
                                                        "driverId": None})
            report["routes"] += 1
        entries = api.request("GET", f"/api/knowledge-entries?routeId={route['id']}") or []
        for note in rdef.get("notes", []):
            ensure_entry(api, note, {"routeId": route["id"]}, entries, report, attachments)

        stops = api.request("GET", f"/api/routes/{route['id']}/stops") or []
        for sdef in rdef["stops"]:
            stop = next((s for s in stops if s["customerName"] == sdef["customerName"]), None)
            if stop is None:
                loc_id = ensure_location(api, sdef["location"], report)
                stop = api.request("POST", f"/api/routes/{route['id']}/stops", {
                    "customerName": sdef["customerName"],
                    "sequenceOrder": sdef["sequenceOrder"], "locationId": loc_id})
                report["stops"] += 1
            s_entries = api.request("GET", f"/api/knowledge-entries?stopId={stop['id']}") or []
            for note in sdef.get("notes", []):
                ensure_entry(api, note, {"stopId": stop["id"]}, s_entries, report, attachments)
    return report


def remove(api, data):
    removed = 0
    names = {r["name"] for r in data["routes"]}
    for route in api.request("GET", "/api/routes") or []:
        if route["name"] in names:
            api.request("DELETE", f"/api/routes/{route['id']}")
            removed += 1
    return {"routes_removed": removed}


def main(argv):
    data = json.loads(DATA_FILE.read_text())
    backend = os.environ.get("ROUTEBOOK_URL", "http://localhost:8080")
    identity = os.environ.get("IDENTITY_URL", "http://localhost:8081")
    email = os.environ.get("SEED_EMAIL") or input("RouteBook writer email: ")
    password = os.environ.get("SEED_PASSWORD") or getpass.getpass("Password: ")
    try:
        api = Client(backend, login(identity, email, password))
        if "--remove" in argv:
            print("Removed:", remove(api, data))
        else:
            print("Added:", seed(api, data, attachments="--no-attachments" not in argv))
    except ApiError as e:
        print(f"Failed: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
