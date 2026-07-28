# RouteBook

RouteBook is an institutional-knowledge management system for Cintas Service Sales Representative (SSR) routes — a digital replacement for the sticky notes, texts, and tribal knowledge that carry gate codes, dock hazards, parking rules, and access instructions between drivers when a route changes hands.

## The problem

When an SSR route gets reassigned, the knowledge that made the outgoing driver efficient — "the gate code resets monthly," "don't park in front of the dock doors," "ask for Mike at the front desk" — usually doesn't transfer with it. RouteBook gives that knowledge a permanent, searchable home, attached directly to the route or the specific stop it applies to.

## Tech stack

- **Backend**: Spring Boot 3.5.16, Java 21 (target), Spring Data JPA, Bean Validation
- **Database**: H2 (in-memory, dev) — PostgreSQL planned for production
- **File storage**: Cloudflare R2 (S3-compatible) via AWS SDK v2, for attachment photos/PDFs/documents/videos
- **Build**: Maven

Companion frontend: [routebook-frontend](../routebook-frontend) (React 18 + TypeScript + Vite + Tailwind CSS v4).

## Core features

- **Route/Stop/Driver/Location model** — mirrors real SSR route structure
- **Knowledge entries with exactly one target** — every note attaches to either a Route or a Stop, never both, never neither (enforced at the service layer, with an entity-level backstop)
- **File attachments** — photos, PDFs, Word docs, plain text, spreadsheets, and videos, stored in Cloudflare R2, served via time-limited presigned URLs. Verified end-to-end against a live R2 bucket.
- **Structured error handling** — every API error returns a consistent `{status, message, timestamp, fieldErrors}` shape
- **Content-type and size validation** — 25MB cap for photos/documents, 250MB for videos, with an explicit content-type whitelist

## Documentation

- [SETUP.md](SETUP.md) — getting the backend running locally
- [ARCHITECTURE.md](ARCHITECTURE.md) — layered design, domain model, key decisions
- [API_REFERENCE.md](API_REFERENCE.md) — full endpoint reference with examples

## Project status

| Phase | Scope | Status |
|---|---|---|
| 0 | Scaffold, H2, Actuator health check | Done |
| 1 | Domain model, seed data | Done |
| 2 | REST API (DTOs, controllers) | Done |
| 3 | Service layer, validation, global error handling | Done |
| 4 | Cloudflare R2 file attachments | Done — fully verified end-to-end against a live bucket |
| 5–6 | React frontend | Done — see [routebook-frontend](../routebook-frontend) |
| 7 | Documentation, GitHub publish | In progress |

## Known limitations

- No authentication/authorization yet — anyone with API access can read and write all data
- H2 is in-memory (`ddl-auto=create-drop`); all data resets on every restart. PostgreSQL migration is planned for production.
- No automated test suite yet — the project has been verified through manual `curl` testing and browser walkthroughs, not JUnit tests
