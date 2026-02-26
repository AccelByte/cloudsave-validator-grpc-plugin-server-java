# cloudsave-validator-grpc-plugin-server-java

An Extend Override app for the **CloudSave validator** written in Java. AGS calls this gRPC server to validate game and player records before they are written to or after they are read from CloudSave.

This is a template project — clone it, replace the sample logic in the service implementation, and deploy.

## Build & Test

```bash
./gradlew build                      # Build with Gradle
./gradlew test                       # Run tests
docker compose up --build            # Run locally with Docker
./gradlew generateProto              # Regenerate proto code
```

## Architecture

AGS invokes this app's gRPC methods instead of its default logic:

```
Game Client → AGS → [gRPC] → This App → Response → AGS
```

The sample implementation validates game records by key suffix (e.g. `map` records are deserialized and checked against custom rules), enforces time-based access control on `daily_msg` records, limits `id_card` binary records to a single write, and checks file size on `event_banner` uploads.

### Key Files

| Path | Purpose |
|---|---|
| `src/main/java/net/accelbyte/cloudsave/validator/App.java` | Entry point — starts gRPC server, wires interceptors and observability |
| `src/main/java/net/accelbyte/cloudsave/validator/service/CloudsaveValidatorService.java` | **Service implementation** — your custom logic goes here |
| `src/main/proto/cloudsaveValidatorService.proto` | gRPC service definition (AccelByte-provided, do not modify) |
| `docker-compose.yaml` | Local development setup |
| `.env.template` | Environment variable template |

### Validation Notes

CloudSave calls different gRPC methods depending on the operation: `BeforeWrite*` hooks validate data before persistence, `AfterRead*` hooks can redact or gate access after retrieval. Each hook receives the record key and payload, so validation logic is dispatched by key suffix pattern matching. The sample demonstrates validation for game records, player records, admin records, and binary records.

## Rules

See `.agents/rules/` for coding conventions, commit standards, and proto file policies.

## Environment

Copy `.env.template` to `.env` and fill in your credentials.

| Variable | Description |
|---|---|
| `AB_BASE_URL` | AccelByte base URL (e.g. `https://test.accelbyte.io`) |
| `AB_CLIENT_ID` | OAuth client ID |
| `AB_CLIENT_SECRET` | OAuth client secret |
| `AB_NAMESPACE` | Target namespace |
| `PLUGIN_GRPC_SERVER_AUTH_ENABLED` | Enable gRPC auth (`true` by default) |

## Dependencies

- [AccelByte Java SDK](https://github.com/AccelByte/accelbyte-java-sdk) (`net.accelbyte.sdk:sdk`) — AGS platform SDK and gRPC plugin utilities
