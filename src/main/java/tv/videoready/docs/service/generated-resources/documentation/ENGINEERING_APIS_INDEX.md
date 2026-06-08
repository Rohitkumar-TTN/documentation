# All API documentation (index)

This page lists **REST API reference** topics for **`binge-mobile-services`** only. It is separate from [flow / runbook documentation](./ENGINEERING_FLOWS_INDEX.md) (onboarding, deboarding, CDMS, ingestion, etc.).

In the **engineering docs hub**, use route [`/flows/engineering-apis`](/flows/engineering-apis). Each endpoint below has its own page at `/flows/{slug}`.

| Topic | Open in hub |
|-------|----------------|
| Generate OTP (public) | [binge-mobile-generate-otp](/flows/binge-mobile-generate-otp) |
| Validate OTP (public) | [binge-mobile-validate-otp](/flows/binge-mobile-validate-otp) |
| Subscriber details (v4) | [binge-mobile-subscriber-details-v4](/flows/binge-mobile-subscriber-details-v4) |
| Update existing user | [binge-mobile-update-user](/flows/binge-mobile-update-user) |
| Create new user (deprecated) | [binge-mobile-create-user](/flows/binge-mobile-create-user) |
| Verbiage details | [binge-mobile-verbiage-details](/flows/binge-mobile-verbiage-details) |
| Current subscription (v2) | [binge-mobile-v2-subscription-current](/flows/binge-mobile-v2-subscription-current) |

## Out of scope

- **PubNub** subscribe URLs are client real-time transport, not `binge-mobile-services` HTTP handlers.

## Typical login-related call order

1. Generate OTP → 2. Validate OTP → 3. Subscriber details (v4) → 4. Update existing user (or deprecated create) → 5. Subscription current / verbiages as needed.
