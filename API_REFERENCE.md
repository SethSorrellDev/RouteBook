# API Reference

Base URL (local dev): `http://localhost:8080`

All error responses share this shape:

```json
{
  "status": 404,
  "message": "No driver found with id 999",
  "timestamp": "2026-07-28T08:42:17.954606Z",
  "fieldErrors": null
}
```

`fieldErrors` is populated only for Bean Validation failures, mapping field name to message.

---

## Drivers

### `GET /api/drivers`
Returns all drivers.

### `GET /api/drivers/{id}`
Returns one driver, or 404.

### `POST /api/drivers`
```json
{
  "employeeId": "EMP-1002",
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com"
}
```
`email` is optional but must be a valid email format if provided. `employeeId`, `firstName`, `lastName` are required.

---

## Routes

### `GET /api/routes`
### `GET /api/routes/{id}`
### `POST /api/routes`
```json
{
  "name": "Route 14 - Frankfort",
  "description": "Normal route covering Frankfort industrial corridor",
  "driverId": 1
}
```
`driverId` is optional (a route can exist unassigned). If provided, must reference an existing driver or the request returns 404.

---

## Stops

### `GET /api/routes/{routeId}/stops`
Returns all stops for a route, ordered by `sequenceOrder` is *not* guaranteed by the API — sort client-side if order matters. 404 if the route doesn't exist.

### `POST /api/routes/{routeId}/stops`
```json
{
  "customerName": "Nucor Steel",
  "sequenceOrder": 1,
  "locationId": 1
}
```
`locationId` must reference an existing Location (create one via `POST /api/locations` first).

---

## Locations

### `GET /api/locations`
### `POST /api/locations`
```json
{
  "addressLine1": "1300 Somerset Rd",
  "addressLine2": null,
  "city": "Crawfordsville",
  "state": "IN",
  "zipCode": "47933",
  "latitude": 40.0411,
  "longitude": -86.8745
}
```
`latitude`/`longitude` are optional. `addressLine1`, `city`, `state`, `zipCode` are required.

---

## Knowledge Entries

### `GET /api/knowledge-entries?routeId={id}&stopId={id}`
Both query params are optional filters; omit both to get every entry.

### `GET /api/knowledge-entries/{id}`

### `POST /api/knowledge-entries`
```json
{
  "title": "Nucor Steel gate code",
  "body": "Main gate keypad code is 4471#. Resets monthly.",
  "category": "GATE_CODE",
  "routeId": null,
  "stopId": 1
}
```
`category` is one of: `ACCESS`, `GATE_CODE`, `PARKING`, `HAZARD`, `CONTACT`, `OTHER`.

**Exactly one of `routeId`/`stopId` must be non-null.** Both set, or both null, returns a 400 with message `"A knowledge entry must target exactly one of routeId or stopId, not both or neither."`

---

## Attachments

### `GET /api/knowledge-entries/{knowledgeEntryId}/attachments`
Returns all attachments for a knowledge entry, each with a fresh 15-minute presigned `downloadUrl`. 404 if the knowledge entry doesn't exist.

### `POST /api/knowledge-entries/{knowledgeEntryId}/attachments`
Multipart form upload, field name `file`.

```bash
curl -X POST http://localhost:8080/api/knowledge-entries/1/attachments \
  -F "file=@/path/to/photo.jpg;type=image/jpeg"
```

**Allowed content types**: `image/jpeg`, `image/png`, `image/gif`, `image/webp`, `image/heic`, `image/heif`, `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `text/plain`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `video/mp4`, `video/quicktime`, `video/webm`, `video/x-msvideo`.

**Size limits**: 25MB for photos/documents, 250MB for videos. Exceeding the limit returns 400.

### `DELETE /api/attachments/{attachmentId}`
Deletes both the R2 object and the database record. Returns 204 on success, 404 if the attachment doesn't exist.

---

## Notes for API consumers

- All DTOs are flat — related entities are referenced by ID only (e.g. `RouteDto.driverId`, not a nested driver object). Fetch related resources separately if you need their details.
- There is no authentication currently. Every endpoint is open.
- The in-memory H2 database resets on every backend restart — don't rely on data persisting across restarts in a dev environment.
