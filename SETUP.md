# Setup

## Prerequisites

- **Java**: JDK 21 or newer. Developed and tested against OpenJDK 25 as the runtime, with `maven.compiler.release=21` set in `pom.xml` so the code stays on Java 21 language features. If your distro's package repos don't offer JDK 21 directly (this happened on Fedora 44/Asahi Remix, aarch64), a newer JDK works fine as a runtime as long as the POM's release target stays at 21.
- **Maven**: 3.9+ (bundled Maven Wrapper — `./mvnw` — works without a system-wide Maven install)
- **Cloudflare R2 account** — only required if you want file attachments to actually work; the rest of the API runs fine without it (see "Running without R2" below)

## Clone and build

```bash
git clone <repo-url> RouteBook
cd RouteBook
./mvnw compile
```

## Environment variables

Four environment variables are required at runtime for the Cloudflare R2 integration. **These must never be committed to a file in this repo** — always set them as shell environment variables.

```bash
export R2_ACCESS_KEY_ID="your-r2-access-key-id"
export R2_SECRET_ACCESS_KEY="your-r2-secret-access-key"
export R2_BUCKET_NAME="your-bucket-name"
export R2_ENDPOINT="https://<your-account-id>.r2.cloudflarestorage.com"
```

To get these values:
1. Create a free Cloudflare account and enable R2 Object Storage (requires a payment method on file, even on the free tier)
2. Create a bucket
3. Under R2 → Manage API Tokens, create a token with **Object Read & Write** permission, scoped to your bucket
4. Copy the Access Key ID and Secret Access Key immediately — the secret is shown only once
5. Your Account ID is shown on the R2 Overview page; the endpoint is `https://<account-id>.r2.cloudflarestorage.com`

### Running without R2

If you just want to test the core API (drivers, routes, stops, knowledge entries) without setting up Cloudflare, the four variables just need to exist and be non-empty — placeholder values work fine, since the S3 client only fails when it actually tries to reach R2:

```bash
export R2_ACCESS_KEY_ID="placeholder"
export R2_SECRET_ACCESS_KEY="placeholder"
export R2_BUCKET_NAME="placeholder"
export R2_ENDPOINT="https://placeholder.r2.cloudflarestorage.com"
```

Every endpoint except the attachment upload/download/delete routes will work normally.

## Running locally

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. On startup, `DataSeeder` populates the in-memory H2 database with one driver, one route, three stops, and three knowledge entries — this happens on every restart since the schema is recreated fresh each time (`ddl-auto=create-drop`).

## H2 console

While the app is running, visit `http://localhost:8080/h2-console`. On the login screen:

- **JDBC URL**: `jdbc:h2:mem:routebook` (must be entered manually — the default saved profile points elsewhere)
- **User Name**: `sa`
- **Password**: leave blank

## Testing

There is currently no automated test suite — `RouteBookApplicationTests.java` is the default Spring Initializr context-load test and nothing more. The API has been verified through manual `curl` testing (documented informally during development) and through the companion React frontend. Adding JUnit/MockMvc tests is a natural next step.

## Common issues

- **Port 8080 already in use**: usually a previous `spring-boot:run` still running in another terminal. Find and stop it with `sudo fuser -k 8080/tcp`, or locate the process with `sudo lsof -i :8080` / `sudo ss -ltnp | grep 8080`.
- **App fails to start with a property placeholder error**: one of the four `R2_*` environment variables isn't set in the current shell session — they don't persist across terminal windows/tabs.
- **Lombok "cannot find symbol" errors for getters/setters**: on newer JDKs, Lombok requires both a version pin (≥1.18.42) and explicit `annotationProcessorPaths` in the `maven-compiler-plugin` config — both are already set in this project's `pom.xml`, but worth knowing if you see this pattern elsewhere.
