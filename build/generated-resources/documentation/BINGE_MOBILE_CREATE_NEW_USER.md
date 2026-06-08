# Binge mobile services — Create new user (deprecated)

## Purpose

Historically created a **new** Binge mobile (freemium) account via `profileMapperUtil.createBingeAccountForFreemium`. The endpoint is **`@Deprecated`** and may return a **deprecation** payload when `v1.create.user.deprecated` is enabled, or be blocked by `deprecateAPIUtil` for certain platforms.

Prefer the modern journey: **generate OTP → validate OTP → subscriber details → update existing user** (see index).

## HTTP

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/api/{apiVersion}/create/new/user` |
| Documented example | `/api/v1/create/new/user` |

## Implementation

- Controller: `tv.videoready.api.v3.controller.BingeMobileUserController`  
- Handler: `@PostMapping("/create/new/user")` with `@Deprecated`  
- Body: `AddBingeMobileUserCO` (`@JsonIgnoreProperties(ignoreUnknown = true)`)  
- Flags: `v1CreateUserDeprecated` (`${v1.create.user.deprecated}`), `deprecateAPIUtil.isDeprecated("/api/v1/create/new/user", platform.getValue())`.

## Request headers

| Header | Required | Notes |
|--------|----------|--------|
| `deviceId` | Yes | Per Swagger; device fingerprint. |
| `deviceName` | Yes | |
| `anonymousId` | Yes | |
| `platform` | Yes | `Platform` enum. |
| `authorization` | No | Kong user token when used. |
| `deviceToken` | No | Device token when used. |
| `locale` | No | Default `en`. |

## Request body (`AddBingeMobileUserCO`)

| Field | Required | Notes |
|-------|----------|--------|
| `eulaChecked` | Yes (`@NotNull`) | Must be `true` for compliant flows. |
| `mobileNumber` | No* | Often present. |
| `subscriberId` | No* | |
| `login` | No | `LOGIN` enum. |
| `dthStatus` | No | Default `Non DTH User`. |
| `largeDeviceId`, `isPastBingeUser`, `language`, `deviceType`, `baId`, `referenceId`, `packageId`, `cartId`, `journeySource`, `couponCode` | No | Extended fields. |

\* Validation rules beyond `eulaChecked` — confirm with `AddBingeMobileUserCO` and any `@Valid` custom validators.

## Response behaviour

- If deprecated flag is on: success response with message data `"This endpoint is deprecated"` and `ApiResponseCode.Generic.DEPRECATED_ANYWHERE_API`.  
- Else: `BingeMobileUserResponseDTO` with `ApiResponseCode.LoginUser.LOGIN_SUCCESS`.

## Example curl (sanitized)

```bash
curl -sS -X POST \
  'https://<gateway>/binge-mobile-services/api/v1/create/new/user' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'deviceId: <device-id>' \
  -H 'deviceName: Web' \
  -H 'anonymousId: <uuid>' \
  -H 'platform: WEB' \
  -H 'authorization: Bearer <ACCESS_TOKEN>' \
  -H 'locale: en' \
  --data-raw '{
    "mobileNumber": "<RMN>",
    "subscriberId": "<sid>",
    "eulaChecked": true,
    "login": "OTP",
    "dthStatus": "Non DTH User"
  }'
```

## Related

- [Update existing user](./BINGE_MOBILE_UPDATE_EXIST_USER.md) — supported path for returning users.  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
