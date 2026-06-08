# Binge mobile services — Update existing user

## Purpose

**Login / sync for an existing** Binge mobile (freemium) user: updates the server-side profile and device binding using subscriber ids returned from earlier steps (for example after **subscriber details** and OTP). Flow constant in code: `LOGIN_EXISTING_ACCOUNT`.

## HTTP

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/api/{apiVersion}/update/exist/user` |
| Typical version | `v3` → `/api/v3/update/exist/user` |

## Implementation

- Controller: `tv.videoready.api.v3.controller.BingeMobileUserController`  
- Class mapping: `@RequestMapping("/api/{apiVersion}")`  
- Handler: `@PostMapping("/update/exist/user")`  
- Body: `UpdateBingeMobileUserCO`  
- Core logic: `profileMapperUtil.updateBingeMobileUserForFreemium(deviceId, deviceName, anonymousId, platform, updateBingeMobileUserCO, authKongToken, deviceKongToken, myTransactionId)`.

## Request headers

| Header | Required | Notes |
|--------|----------|--------|
| `deviceName` | Yes | Human-readable device label. |
| `anonymousId` | Yes | Device correlation id. |
| `platform` | Yes | Must parse to `Platform` enum (`WEB`, etc.). |
| `deviceId` | No | Still sent by web clients. |
| `authorization` | No* | Often required by gateway; Kong user token. |
| `deviceToken` | No | Device-level token when used. |
| `locale` | No | Default `en`. |

\* Treat as required in secured environments even though the method marks it optional.

## Request body (`UpdateBingeMobileUserCO`)

Commonly used fields (see class for full set):

| Field | Description |
|-------|-------------|
| `subscriberId` | Tata Play / subscriber id (SID). |
| `bingeSubscriberId` | Binge subscriber id. |
| `baId` | Binge Anywhere id. |
| `mobileNumber` | RMN. |
| `login` | Login mode (enum `LOGIN`, for example `OTP`). |
| `dthStatus` | String such as `Non DTH User` (default in DTO). |
| `payment_return_url` | Return URL after payment flows. |
| `packageId` | Pack context when applicable. |
| `dsn`, `referenceId`, `cartId`, `silentLoginEvent`, `temporaryId`, `journeySource`, `couponCode` | Optional extended journey fields. |

Some clients send extra JSON keys (for example UI-only flags). `UpdateBingeMobileUserCO` does **not** declare `@JsonIgnoreProperties(ignoreUnknown = true)`; if deserialization fails, align the payload strictly with the DTO or confirm global Jackson unknown-property settings for this service.

## Flow

1. `validateApiVersion(apiVersion)`.
2. Build `myTransactionId` with `FlowConstants.LOGIN_EXISTING_ACCOUNT` and subscriber / BA ids from body.
3. `updateBingeMobileUserForFreemium` performs freemium profile merge / persistence.
4. Returns `BingeMobileUserResponseDTO` inside the standard success `ResponseDTO`.

## Response shape

Success code `ApiResponseCode.LoginUser.LOGIN_SUCCESS` with `data` = `BingeMobileUserResponseDTO` (see `tv.videoready.api.v3.dto.response.BingeMobileUserResponseDTO`).

### Example success envelope (illustrative)

```json
{
  "code": 200,
  "message": "Login success",
  "data": {
    "subscriberId": "<sid>",
    "baId": "<ba-id>"
  }
}
```

## Example curl (sanitized)

```bash
curl -sS -X POST \
  'https://<gateway>/binge-mobile-services/api/v3/update/exist/user' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'authorization: Bearer <ACCESS_TOKEN>' \
  -H 'deviceId: <device-id>' \
  -H 'device: WEB' \
  -H 'deviceName: Web' \
  -H 'deviceToken: <device-token>' \
  -H 'anonymousId: <uuid>' \
  -H 'platform: WEB' \
  -H 'locale: en' \
  --data-raw '{
    "dthStatus": "Non DTH User",
    "subscriberId": "<sid>",
    "bingeSubscriberId": "<binge-sid>",
    "baId": "<ba-id>",
    "login": "OTP",
    "mobileNumber": "<RMN>",
    "payment_return_url": "https://<your-web-host>/subscription-transaction/status",
    "packageId": ""
  }'
```

## Related

- [Subscriber details v4](./BINGE_MOBILE_SUBSCRIBER_DETAILS_V4.md)  
- [Create new user (deprecated)](./BINGE_MOBILE_CREATE_NEW_USER.md)  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
