# Tata Play — engineering documentation

| Document | Purpose |
|----------|---------|
| [PARTNER_ONBOARDING_MR_FLOW.md](./PARTNER_ONBOARDING_MR_FLOW.md) | Partner **onboarding**: MR list, merge order, layers, start→end checklist. |
| [PARTNER_DEBOARDING_TECHNICAL.md](./PARTNER_DEBOARDING_TECHNICAL.md) | Partner **deboarding** (technical): DB, config, CMS, APIs, search. |
| [PARTNER_DEBOARDING.md](./PARTNER_DEBOARDING.md) | Deboarding overview. |
| [JHS_BILLING_STEPS.md](./JHS_BILLING_STEPS.md) | **JHS billing**: Google Doc link, engineering runbook, env checklist, paste-area for exported steps. |
| [BINGE_MEDIA_SEARCH_INGESTION.md](./BINGE_MEDIA_SEARCH_INGESTION.md) | **Binge media search**: catalog → Google Discovery Engine; Kafka, REST curls, purge job (`binge-media-search-ops`). |
| [THIRD_PARTY_CONNECTOR_INGESTION_FLOW.md](./THIRD_PARTY_CONNECTOR_INGESTION_FLOW.md) | **Third-party connector → data-consumer**: `syncData`, `manualIngestData`, RabbitMQ, ext-config; MR !2958 / !1334. |
| [CDMS_COUPON_AND_CAMPAIGN_FLOW.md](./CDMS_COUPON_AND_CAMPAIGN_FLOW.md) | **CDMS** (`cdms-core`): campaigns, partners, unique coupon generate/add-more, bulk export, make-live; `cdms-coupon-generator`. |
| [ENGINEERING_FLOWS_INDEX.md](./ENGINEERING_FLOWS_INDEX.md) | **Hub: all flow docs in one page** — links to every runbook (`/flows/…`). |
| [ENGINEERING_APIS_INDEX.md](./ENGINEERING_APIS_INDEX.md) | **Hub: all API docs in one page** — links to every binge-mobile-services API topic (`/flows/…`). |
| [BINGE_MOBILE_API_INDEX.md](./BINGE_MOBILE_API_INDEX.md) | Legacy pointer; canonical API index is `ENGINEERING_APIS_INDEX.md`. |

## HTML hub (single Java app)

Run the standalone app [`../engineering-docs-hub`](../engineering-docs-hub) to browse these files as HTML (`@Controller` + Thymeleaf). From that directory:

```bash
JAVA_HOME=/path/to/jdk8 ./gradlew bootRun
```

Gradle **4.8** in this wrapper does not work with **JDK 21**; use **JDK 8** (or a supported older JDK) for `./gradlew`. Then open [http://localhost:8095/](http://localhost:8095/).
