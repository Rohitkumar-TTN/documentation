# Binge mobile services — Generate OTP

## Purpose

Triggers an OTP SMS (or equivalent) for the given **registered mobile number (RMN)** as part of the **freemium / Binge Anywhere** login path. This endpoint is **public** (no Kong `authorization` required at the controller).

## HTTP

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/pub/api/{apiVersion}/user/authentication/generateOTP` |
| Example | `/pub/api/v1/user/authentication/generateOTP` |

## Implementation

- Controller: `tv.videoready.api.v3.controller.AuthenticationController`  
- Class mapping: `@RequestMapping("/pub/api")`  
- Handler: `@PostMapping("/{apiVersion}/user/authentication/generateOTP")`

Flow (non-dongle platforms):

1. Optional **blocked RMN** check (`block.otp.rmn` list).
2. `loginAuthenticationService.existingUser(platform, mobileNumber, testSubscriber)` — pre-checks subscriber context.
3. `bingeValidationUtilsService.validateApiVersion(apiVersion)` — version gate.
4. `loginAuthenticationService.generateOTPForRMN(...)` — calls OTP stack (transaction id, `newOtpFlow`, `deviceId`, etc.).

For **dongle** platform, `androidStickLoginService.generateOtp` is used instead.

## Request headers

| Header | Required | Notes |
|--------|----------|--------|
| `mobileNumber` | Yes | RMN for OTP. |
| `locale` | No | Default `en`. |
| `platform` | No | Drives branching (for example `dongle` vs default). |
| `deviceType` | No | Passed through for version/pack helpers. |
| `appVersion` | No | Used by `packAdditionHelperUtil.oldVersionUpdate`. |
| `newOtpFlow` | No | Default `6DOTP`; clients may send values such as `4DOTP`. |
| `deviceId` | No | Device fingerprint; logged and passed to OTP generation. |
| `otpTest` | No | Default `false`. |
| `dsn` | No | Device serial (dongle flows). |
| `testSubscriber` | No | Default `false`. |

Clients often also send `anonymousId`, `subscriptionType`, and `platform` values such as `BINGE_ANYWHERE` via gateway or interceptors; the controller signature documents the headers above.

## Response shape

The handler returns a `ResponseDTO` built via `responseUtil.prepareSuccessResponse(response, ApiResponseCode.OTP.GENERATE_OTP_SUCCESS, locale)`. The **`data`** payload is a `Map` from `loginAuthenticationService.generateOTPForRMN` (implementation-specific keys; treat as opaque unless you trace `LoginAuthenticationServiceImpl`).

### Example success envelope (illustrative)

Responses are JSON with at least `code`, `message`, and `data` (exact codes/messages come from `ApiResponseCode` and locale).

```json
{
  "code": 200,
  "message": "OTP generated successfully",
  "data": {
    "transactionId": "<string>",
    "otpLength": 4
  }
}
```

Run against your UAT gateway with a **test RMN** and capture a real payload; do not commit tokens or PII.

## Example curl (sanitized)

Replace `https://<gateway>/binge-mobile-services` with your environment base URL.

```bash
curl -sS -X POST \
  'https://<gateway>/binge-mobile-services/pub/api/v1/user/authentication/generateOTP' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'mobileNumber: <RMN>' \
  -H 'platform: BINGE_ANYWHERE' \
  -H 'newOtpFlow: 4DOTP' \
  -H 'deviceId: <device-id>' \
  -H 'anonymousId: <uuid>' \
  -H 'subscriptionType: GUEST' \
  -H 'locale: en'
```

## Related

- [Validate OTP](./BINGE_MOBILE_VALIDATE_OTP.md) — next step after user receives SMS.  
- [All API documentation (index)](./ENGINEERING_APIS_INDEX.md) — hub: `/flows/engineering-apis`.
