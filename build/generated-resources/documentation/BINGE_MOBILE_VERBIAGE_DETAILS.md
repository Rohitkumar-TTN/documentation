# Binge mobile services — Verbiage details

## Purpose

Returns **config-driven UI copy** (“tick tick” nudges, prompts, fallbacks) for a given **DTH status**, platform, and optional subscriber context. Backed by `VerbiagesService.getPromptVerbiages` (not a subscription or OTP API).

## HTTP

| Item | Value |
|------|--------|
| Method | `GET` |
| Path | `/api/v1/verbiages/details` |

Note: the controller uses `@GetMapping(value = "api/v1/verbiages/details")` **without** a leading slash; Spring resolves this as **`/api/v1/verbiages/details`** at the servlet context root (same as other controllers).

## Implementation

- Controller: `tv.videoready.api.v3.controller.VerbiagesController`  
- Handler: `tickTickPromptVerbiage` → `verbiagesService.getPromptVerbiages(...)`  
- Response: `PromptVerbiagesDTO` with `ApiResponseCode.Generic.SUCCESS`.

There is a sibling endpoint **`GET /api/v1/piVerbiages/details`** for **partner install (PI) page** copy; document separately if needed.

## Request headers

| Header | Required | Notes |
|--------|----------|--------|
| `dthStatus` | Yes | Drives which verbiage bundle loads. |
| `platform` | Yes | `Platform` enum. |
| `baId` | No | Account context. |
| `dsn` | No | Device serial. |
| `deleteAccount` | No | Default `false`. |
| `rmn` | No | Masked / listing context. |
| `subscriberId` | No | |
| `deviceId` | No | |
| `locale` | No | Default `en`. |
| `appVersion` | No | Default in signature is `en` (legacy default; clients should send real semver). |
| `journey`, `action` | No | Fine-grained prompt selection. |
| `device` | No | Default `WEB`. |
| `deviceName` | No | |
| `authorization` | No | Optional; passed into service for token-aware copy. |

## Flow

1. Log request context (baId, dthStatus, platform, etc.).  
2. `getPromptVerbiages` loads / composes `PromptVerbiagesDTO`.  
3. On `CommonApiException`, maps to `ApiResponseCode.TickTick.MA_FALLBACK_FAILURE`.

## Response shape

`data` = `PromptVerbiagesDTO` (`tv.videoready.api.v3.dto.PromptVerbiagesDTO`). Exact keys depend on CMS / config; capture from UAT for field-level docs.

### Example success envelope (illustrative)

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "prompts": [],
    "nudges": {}
  }
}
```

## Example curl (sanitized)

```bash
curl -sS -X GET \
  'https://<gateway>/binge-mobile-services/api/v1/verbiages/details' \
  -H 'Accept: application/json' \
  -H 'dthStatus: Non DTH User' \
  -H 'platform: WEB' \
  -H 'baId: <ba-id>' \
  -H 'subscriberId: <sid>' \
  -H 'deviceId: <device-id>' \
  -H 'locale: en' \
  -H 'authorization: Bearer <ACCESS_TOKEN>'
```

## Related

- PI page verbiages: same controller, path `api/v1/piVerbiages/details`.  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
