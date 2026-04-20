# core:realtime

Socket.IO client for board collaboration. Uses the same origin as `NetworkConfig.apiBaseUrl` as Retrofit (trim trailing `/`).

## Server alignment

- Java client version is declared in the root version catalog (`socketIoClient`). It **must** match the backend `socket.io` / Engine.IO version.
- Emit: `joinBoard` with JSON `{ "boardId": "<id>", "token": "<access_token>" }` where `token` is the raw JWT (no `Bearer ` prefix), consistent with how the token is stored locally after login.

## Events (1:1 with backend)

| Event | Typical payload | Notes |
|-------|-----------------|-------|
| `board:joined` | optional metadata | Connection accepted for board room |
| `board:join_error` | string or object with message | Handle `unauthorized` / `forbidden` in UI |
| `board:updated` | partial board fields or full board JSON | Parser tries full `BoardDetails` then merges metadata |
| `board:deleted` | — | Navigate away from detail |
| `columns:updated` | `{ "columns": [...] }` or array of columns | Replaces columns |
| `card:created` | `Card` JSON or full `BoardDetails` | |
| `card:updated` | `Card` JSON or full `BoardDetails` | |
| `card:moved` | full `BoardDetails` (preferred) or card | Full snapshot replaces board graph |
| `comment:added` | updated `Card` JSON | Upsert card in columns |

Payload strings are forwarded as-is to the feature layer for decoding with the same `kotlinx.serialization` DTOs as REST.
