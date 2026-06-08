# Binge mobile services — Validate OTP

## Purpose

Validates the OTP entered by the user and completes the **public** authentication leg: on success the service returns session / subscriber material (implementation in `LoginAuthenticationService.validateOtp`) used by the client to continue login or linking.

## HTTP

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/pub/api/{apiVersion}/user/authentication/validateOTP` |
| Example | `/pub/api/v1/user/authentication/validateOTP` |

## Implementation

- Controller: `tv.videoready.api.v3.controller.AuthenticationController`  
- Class mapping: `@RequestMapping("/pub/api")`  
- Handler: `@PostMapping("/{apiVersion}/user/authentication/validateOTP")`  
- Body: `OtpLoginRequestCO` (`mobileNumber`, `otp` required; optional `subscriberId`, `dsn`, `source`, …).

There is a **separate** controller (`DeleteAccountController`) exposing `POST /api/{apiVersion}/user/authentication/validateOTP` under **`/api`** (not `/pub`), with a slightly different header contract (`deviceId` optional). Web clients for Binge Anywhere typically use the **`/pub/api`** route above.

## Request headers

| Header | Required | Notes |
|--------|----------|--------|
| `deviceId` | Yes | Tied to device session. |
| `anonymousId` | Yes | Anonymous device / session correlation. |
| `locale` | No | Default `en`. |
| `platform` | No | Dongle vs default path. |
| `otpTest` | No | Default `false`. |
| `tsmore` | No | Default `false` (typo `tsmore` vs `tsMore` in different controllers — use what your client already sends). |
| `dummyFlag` | No | Test / special flows. |
| `unlocked` | No | Default `false`. |
| `silentLoginEvent` | No | Silent login hints. |
| `appVersion` | No | Version checks. |

## Request body (JSON)

| Field | Required | Description |
|-------|----------|-------------|
| `mobileNumber` | Yes | Same RMN as generate OTP. |
| `otp` | Yes | OTP entered by user. |
| `subscriberId` | No | If already known. |
| `dsn` | No | Device serial where applicable. |
| `source` | No | Attribution. |

## Flow

1. Optional **blocked RMN** check using `loginRequestCO.getMobileNumber()`.
2. **Dongle** platform → `androidStickLoginService.validateOtp(...)`.
3. Else → `bingeValidationUtilsService.validateApiVersion(apiVersion)` then `loginAuthenticationService.validateOtp(loginRequestCO, deviceId, anonymousId, myTransactionId, tsMore, platform, unlocked, dummyFlag)` (see `AuthenticationController.validateOtp` for the exact parameter list).

## Response shape

Success uses `ApiResponseCode.ValidateOTP.OTP_VALIDATION_SUCCESS` with **`data`** as a `Map` built inside `validateOtp` (tokens, subscriber ids, etc. — trace `LoginAuthenticationServiceImpl` for exact keys).

### Example success envelope (illustrative)

```json
{
  "code": 200,
  "message": "OTP validated successfully",
  "data": {
    "subscriberId": "<sid>",
    "baId": "<ba-id>",
    "authToken": "<do-not-log-in-docs>"
  }
}
```

Never paste real **Bearer** tokens or production RMNs into documentation or git.

## Example curl (sanitized)

```bash
curl -sS -X POST \
  'https://<gateway>/binge-mobile-services/pub/api/v1/user/authentication/validateOTP' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'deviceId: <device-id>' \
  -H 'anonymousId: <uuid>' \
  -H 'platform: BINGE_ANYWHERE' \
  -H 'subscriptionType: GUEST' \
  -H 'locale: en' \
  --data-raw '{"mobileNumber":"<RMN>","otp":"<otp>"}'
```

## Related

- [Generate OTP](./BINGE_MOBILE_GENERATE_OTP.md)  
- [Subscriber details v4](./BINGE_MOBILE_SUBSCRIBER_DETAILS_V4.md)  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md)
