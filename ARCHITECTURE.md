# Architecture

## Layered structure

RouteBook follows a standard Spring Boot layered architecture:Controller -> Service -> Repository -> Database
| |
DTOs Business rules,
(request/ validation,
response) exception throwing- **Controllers** are thin — they call a service method and return the result. No business logic lives here.
- **Services** hold all business logic: entity lookups (throwing `ResourceNotFoundException` on miss), the knowledge-entry targeting rule, and R2 upload/delete orchestration.
- **Repositories** are plain Spring Data JPA interfaces — no custom query logic beyond `findByX` derived queries.
- **DTOs are flat**, not nested. `RouteDto` carries `driverId: Long`, not an embedded `DriverDto`. This keeps the wire format simple and predictable; the frontend fetches related data separately if it needs it.

## Domain modelDriver 1---* Route 1---* Stop *---1 Location
|
*
KnowledgeEntry *---1 Stop
|
* (mutually exclusive with the above)
1 Route

KnowledgeEntry --- AttachmentFive entities:

- **Driver** — an SSR, intentionally minimal (no auth concerns here)
- **Route** — a named route, owned by one Driver, made of ordered Stops
- **Stop** — a customer location on a Route, linked to a Location, with a `sequenceOrder`
- **Location** — a physical address with optional lat/lon
- **KnowledgeEntry** — the actual knowledge payload (title, body, category), targeting exactly one of Route or Stop
- **Attachment** — a file (photo/PDF/doc/video) belonging to exactly one KnowledgeEntry, with its bytes in R2 and only metadata in the database

## The Route/Stop XOR design

A `KnowledgeEntry` must target exactly one of `Route` or `Stop` — never both, never neither. This is enforced in two places:

1. **`KnowledgeEntryService.create()`** — the authoritative check. Throws `InvalidKnowledgeEntryTargetException` (mapped to HTTP 400) if `routeId` and `stopId` are both null or both non-null.
2. **`KnowledgeEntry.validateExactlyOneTarget()`** — a `@PrePersist`/`@PreUpdate` hook on the entity itself, as a defense-in-depth backstop in case some future code path saves an entry without going through the service layer.

The database columns (`route_id`, `stop_id`) are both nullable — there's no database-level constraint enforcing the XOR, since standard JPA/Hibernate doesn't have a clean way to express "exactly one of these two foreign keys is set." The two application-level checks are the enforcement mechanism.

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps every exception type to a consistent `ErrorResponse` shape:

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `InvalidKnowledgeEntryTargetException` | 400 |
| `UnsupportedFileTypeException` | 400 |
| `FileTooLargeException` | 400 |
| `MethodArgumentNotValidException` (Bean Validation) | 400, with `fieldErrors` populated |
| `IllegalStateException` (entity-level XOR backstop) | 400 |
| Anything else | 500, with a deliberately generic message |

Every response body is `{status, message, timestamp, fieldErrors}` — `fieldErrors` is `null` except for Bean Validation failures, where it maps field name to validation message.

## Cloudflare R2 integration

R2 is S3-compatible, so `R2Config` configures a standard AWS SDK v2 `S3Client` and `S3Presigner` pointed at R2's endpoint (`https://<account-id>.r2.cloudflarestorage.com`), using `Region.of("auto")` and path-style access — both required for R2 specifically, per Cloudflare's documented SDK configuration.

- **Upload**: `AttachmentService.upload()` validates content-type (against an explicit whitelist covering photos, PDFs, Word docs, plain text, spreadsheets, and common video formats) and size (25MB for photos/documents, 250MB for videos) *before* making any R2 call, so invalid uploads never reach the network. Files are read fully into memory (`RequestBody.fromBytes()`) rather than streamed, because `MultipartFile`'s input stream doesn't support mark/reset, which the AWS SDK requires for request retries — streaming caused real upload failures during development until this was fixed.
- **Download**: rather than proxying file bytes through the Spring app, `AttachmentService` generates a presigned GET URL (15-minute expiry) for each attachment, and the frontend links directly to R2. This keeps the backend out of the bandwidth/memory path for file serving.
- **Storage keys**: each object's R2 key is `knowledge-entries/{knowledgeEntryId}/{uuid}-{sanitized-filename}` — the UUID prefix prevents collisions between uploads with the same original filename.

## Design decisions worth noting

- **H2 with `ddl-auto=create-drop`** for now — appropriate for an in-development portfolio project where the schema is still evolving; a real production deployment would move to PostgreSQL with Flyway/Liquibase migrations.
- **No authentication yet** — deliberately deferred; the current focus is data model and core CRUD/file-handling correctness. Would be added via Spring Security before any real deployment.
- **Flat DTOs over nested** — chosen so the API's wire format doesn't grow deeply nested as more relations are added, and so the frontend has full control over when to fetch related data.
