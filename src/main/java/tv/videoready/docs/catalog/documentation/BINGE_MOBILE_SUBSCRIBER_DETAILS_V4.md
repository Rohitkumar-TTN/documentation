# Binge mobile services — Subscriber details (v4)

## Purpose

Returns **freemium “subscriber listing”** data for an RMN: accounts / SIDs the user can pick or switch between after OTP, before persisting the final device–user binding. This is **not** the same as “create user”; it is a **read** API used in the login optimization path.

## HTTP

| Item | Value |
|------|--------|
| Method | `GET` |
| Path | `/api/v4/subscriber/details` |

## Implementation

- Controller: `tv.videoready.api.v5.controller.LoginOptimizationController`  
- Handler: `@GetMapping("/api/v4/subscriber/details")`  
- Service: `loginOptimizationService.fetchListingViaRMN(rmn, myTransactionId, platform, authenticatedUserId, switchAccount)`  
- Response DTO: `AccountDetailsDTO` wrapped in standard `ResponseUtil.prepareSuccessResponse(..., ApiResponseCode.FetchSubscriber.SUBSCRIBER_FETCHED_SUCCESSFULLY, locale)`.

## Authentication

Clients typically send a **Kong / gateway** token:

| Header | Required | Notes |
|--------|----------|--------|
| `authorization` | Yes (in practice) | `Bearer <access-token>` from validate-OTP / session exchange. |
| `mobileNumber` | Yes | RMN to list subscribers for. |
| `platform` | Yes | `Platform` enum (for example `WEB`, `BINGE_ANYWHERE`, …). |
| `x-authenticated-userid` | No | When present, used with listing / switch-account logic. |
| `source` | No | Default empty string. |
| `locale` | No | Default `en`. |
| `appVersion` | No | Logging / future gating. |
| `switch` | No | Boolean; **switch account** behaviour when supported. |

## Flow

1. Build `myTransactionId` with flow constant `FREEMIUM_SID_LISTING`.
2. Call `fetchListingViaRMN` with RMN, platform, optional authenticated user id, and `switchAccount`.
3. Return `AccountDetailsDTO` as `data` in the generic success envelope.

## Response shape

Success envelope with `data` = `AccountDetailsDTO` (structure defined in `tv.videoready.api.v5.dto.response.AccountDetailsDTO`). Capture a UAT response with a test account to document field-level details for your squad.

### Example success envelope (illustrative)

```json
{
  "code": 200,
  "message": "Subscriber fetched successfully",
  "data": {
    "subscriberList": [],
    "defaultSid": null
  }
}
```

## Example curl (sanitized)

```bash
curl -sS -X GET \
  'https://<gateway>/binge-mobile-services/api/v4/subscriber/details' \
  -H 'Accept: application/json' \
  -H 'authorization: Bearer <ACCESS_TOKEN>' \
  -H 'mobileNumber: <RMN>' \
  -H 'platform: WEB' \
  -H 'deviceType: WEB' \
  -H 'anonymousId: <uuid>' \
  -H 'subscriptionType: GUEST' \
  -H 'locale: en'
```

## Related

- [Update existing user](./BINGE_MOBILE_UPDATE_EXIST_USER.md) — persists profile after user picks an account.  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
