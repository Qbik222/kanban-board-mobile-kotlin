# Kanban Mobile

Android client (Kotlin, Jetpack Compose) for Kanban boards: boards, columns, cards, deadlines, comments, roles, and member invites. Data comes from a REST API; board updates are pushed over Socket.IO.

## Module layout

| Module | Role |
|--------|------|
| `:app` | Entry point, DI (Hilt), navigation, `BuildConfig` (API base URL, CSRF). |
| `:core:network` | Retrofit, DTOs, interceptors (e.g. Bearer). |
| `:core:session` | Session / token persistence. |
| `:core:realtime` | Socket.IO client for the board room; contract details in [core/realtime/README.md](core/realtime/README.md). |
| `:feature:auth` | Sign-in and registration. |
| `:feature:teams` | Teams and members. |
| `:feature:boards` | Board lists, board screen, settings, AI playground. |

## Requirements

- Android Studio compatible with this repo’s AGP, **JDK 17**.
- Emulator or device running **API 26+**.

## Build and run

```bash
./gradlew :app:assembleDebug
```

The **debug** `buildType` in `app/build.gradle.kts` defaults to `http://10.0.2.2:3500/` (emulator → host loopback). Point the **release** `buildType` at your production API URL.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## License

Internal / educational project — add a `LICENSE` file if you need one.
