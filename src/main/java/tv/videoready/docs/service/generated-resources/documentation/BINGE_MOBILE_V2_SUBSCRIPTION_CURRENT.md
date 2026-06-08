# Binge mobile services — Current subscription (v2)

## Purpose

Returns the **current pack / subscription snapshot** for a subscriber (`PackDetailsDTO` or hybrid binge-mobile path for DTH-with-Binge users). This is the **`/api/v2/subscription/current`** entry point on the **v3** `SubscriptionController` bean (path is literal `v2` in the URL for historical reasons).

Instrumented with **`@Trace`** (New Relic) and Brave tracer tags (`tpr-id`, `baId`, etc.) for observability.

## HTTP

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/api/v2/subscription/current` |

## Implementation

- Controller: `tv.videoready.api.v3.controller.SubscriptionController` (`@RestController("SubscriptionControllerv3")`)  
- Handler: `@PostMapping(value = "/api/v2/subscription/current")`  
- Body: `GetSubscriptionCO` — `accountId`, `baId`, `dthStatus` (default `"Non DTH User"`).  
- Main branches:  
  - If **not** DTH-with-Binge (or old stack): `subscriptionManagementService.currentPack(..., "v2", ...)`, then deboarded-partner list tweaks, live-channel mapping (when enabled), small-screen filtering, etc.  
  - If **DTH-with-Binge**: hybrid subscription via `subscriptionService.getBingeMobileSubscriptionHybrid` and `subscriptionManagementService.currentPackMapper`.

## Request headers (representative)

| Header | Required | Notes |
|--------|----------|--------|
| `authorization` | Yes | Kong `Bearer` token. |
| `x-authenticated-userid` | No | Compared to SID for some paths (invalid token response if mismatch). |
| `deviceId` | No | MDC / tracing; DTH path uses MDC. |
| `platform` | No | `Platform` enum. |
| `locale` | No | Default `en`. |
| `unlocked` | No | Freemium unlocked FS flows. |
| `deviceType`, `device`, `journeySource` | No | Routing / UI context. |
| `appVersion` | No | Feature gating (live channel mapping, etc.). |
| `myPlanScreen` | No | Default `false`. |
| `tpr-id` | No | Trace tag. |
| `deviceName` | No | Used for iPhone / small-screen deboard list behaviour. |

## Request body (`GetSubscriptionCO`)

```json
{
  "accountId": "<binge-subscriber-id>",
  "baId": "<ba-id>",
  "dthStatus": "Non DTH User"
}
```

All three fields are strings in the DTO; `dthStatus` values must align with `DthStatus` usage elsewhere (`DTH_WITH_BINGE`, `Non DTH User`, …).

## Flow (simplified)

1. `subscriberHelperService.validateInput(dthStatus)`  
2. `subscriptionUtil.getDetails(getSubscriptionCO)`  
3. Branch on `dthStatus` → **non-DTH** simple `currentPack` vs **DTH-with-Binge** hybrid path with token/SID validation.  
4. Post-process deboarded partner lists, optional live-channel mapping, platform/device-specific list trimming.  
5. Return `PackDetailsDTO` in standard success envelope.

## Response shape

`data` = `PackDetailsDTO` (`tv.videoready.api.v3.dto.response.PackDetailsDTO`) — packs, partners, eligibility flags, deboarded lists, etc. Use a UAT capture for authoritative JSON.

### Example success envelope (illustrative)

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "baId": "<ba-id>",
    "packName": "<string>",
    "partners": []
  }
}
```

## Example curl (sanitized)

```bash
curl -sS -X POST \
  'https://<gateway>/binge-mobile-services/api/v2/subscription/current' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'authorization: Bearer <ACCESS_TOKEN>' \
  -H 'x-authenticated-userid: <sid>' \
  -H 'deviceId: <device-id>' \
  -H 'platform: WEB' \
  -H 'locale: en' \
  -H 'appVersion: <semver>' \
  --data-raw '{"accountId":"<binge-sid>","baId":"<ba-id>","dthStatus":"Non DTH User"}'
```

## Related

- [Subscriber details v4](./BINGE_MOBILE_SUBSCRIBER_DETAILS_V4.md)  
- [Update existing user](./BINGE_MOBILE_UPDATE_EXIST_USER.md)  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
