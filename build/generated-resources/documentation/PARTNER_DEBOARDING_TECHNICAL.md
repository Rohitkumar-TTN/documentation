# Partner Deboarding — Technical Design & Implementation Guide

| Field | Value |
|--------|--------|
| **Audience** | Engineering, QA, Release Management, Operations |
| **Classification** | Internal / Confluence-ready |
| **Primary tickets (from repo history)** | TAPL-7974 (PTC Play), TAPL-9396 (Hallmark+, Fuse+, shared), TSATV2-1665 (staging / ATV cleanup) |
| **Related MRs (GitLab)** | [ext-config !17944](https://gitlab.intelligrape.net/videoready-tatasky/ext-config/-/merge_requests/17944/diffs), [cms-ui !15788](https://gitlab.intelligrape.net/videoready-tatasky/cms-ui/-/merge_requests/15788/diffs), [binge-mobile-services !13914](https://gitlab.intelligrape.net/videoready-tatasky/binge-mobile-services/-/merge_requests/13914/diffs), [cms-api !8599](https://gitlab.intelligrape.net/videoready-tatasky/cms-api/-/merge_requests/8599/diffs), [db-scripts !7353](https://gitlab.intelligrape.net/videoready-tatasky/db-scripts/-/merge_requests/7353/diffs), [static-content !377](https://gitlab.intelligrape.net/videoready-tatasky/static-content/-/merge_requests/377/diffs) |

**Traceability note:** This document was produced by analysing the **local clones** in the workspace (commit messages and file contents aligned to the tickets above). The GitLab MR diff pages were **not** retrieved from the network in this environment. Before publishing to Confluence, reconcile line-level details with the **merged MR diffs** on GitLab.

**Related (onboarding):** [Partner onboarding — MR flow & system map](./PARTNER_ONBOARDING_MR_FLOW.md) (start→end checklist and the same layers in reverse).

**Browse as HTML:** Run [`engineering-docs-hub`](../engineering-docs-hub) (`./gradlew bootRun`) — [deboarding technical](http://localhost:8095/flows/deboarding-technical) · [deboarding overview](http://localhost:8095/flows/deboarding) · [onboarding](http://localhost:8095/flows/onboarding).

---

## 1. Executive overview

### 1.1 Business intent

Tata Play Binge must **gracefully remove** partners that are no longer offered:

- **Live channel partner:** **PTC Play** — treated as **live-channel** footprint (pack/subscription, live-channel mapping, CMS provider surfaces, optional content-consumer safeguards).
- **Complimentary OTT partners:** **Hallmark+** (`HallmarkMoviesNow`) and **Fuse+** (`Fuse`) — primarily **bundle / complimentary-app** footprint (Comviva members, pack definitions, pool config, flexi lists, CMS logos).

### 1.2 Strategy summary (how content is kept off the platform)

| Layer | Mechanism |
|--------|-----------|
| **Data** | SQL removes Comviva member / pack linkage and **scrubs** comma-separated partner lists in `comviva_product`, `pack_details`, `comviva_binge_subscriptions`, `cart_details`, `comviva_entitlements_future`. PTC hybrid users additionally get **`partner_deborded`** / **`deboarded_partners`** set for app prompts. |
| **Config** | `ext-config` removes partners from **eligible**, **platform**, **VIP**, **automotive**, **pool**, **flexi complementary**, **deboard** allow-lists; adjusts **registration blocklists** and **partner ID** exclusions (e.g. `donot.send.partner.registration`). |
| **API** | `binge-mobile-services` strips deboarded complimentary partners using **`deboarded.partner`** and exposes **`deboard.partners.list`** / **`search.deboard.partners.list`** on subscription payloads; **live channel mapping** can filter eligible channel IDs by partner. |
| **CMS** | `cms-api` / `cms-ui` remove provider from **automation defaults**, **editorial image mapping**, **filter options**, **dropdowns**, and **logo JS** so operators cannot target deboarded providers in routine tooling. |
| **Client / ATV** | Partner entries and **logos** removed from **binge-service** `partner-mapping` JSON and ATV third-party logo properties (staging rollout in TSATV2-1665). |
| **Static copy** | `static-content` FAQ / help JSON updated so marketing text no longer lists deboarded apps. |

---

## 2. Scope split (per product requirement)

### 2.1 Live channel partner deboarding — **PTC Play**

**Repository footprint:** `db-scripts`, `ext-config`, `cms-api`, `cms-ui`, `binge-mobile-services`, `static-content`, and selected downstream consumers (`data-consumer`, `search-connector`, `ta-worker`, etc. via `ext-config`).

**Rationale:** PTC Play is positioned as the **live-channel** partner in this initiative: subscription APIs apply **live channel ID** logic and DB migration sets **`deboarded_partners = 'PTCPlay'`** for affected hybrid subscribers.

### 2.2 Complimentary partner deboarding — **Hallmark+ and Fuse+**

**Repository footprint:** `db-scripts`, `ext-config`, `cms-ui`, `binge-mobile-services`, `static-content`.

**Rationale:** Hallmark and Fuse are removed from **complimentary** pools, **VIP strings**, **platform eligible lists**, **pool.provider.list**, **Fiber position enums**, and **PartnerMapper** — typical of **bundle / complimentary** partners rather than live EPG-only integrations.

---

## 3. End-to-end flow (sequence)

The following sequence is **logical**; exact service names vary by deployment topology.

```mermaid
sequenceDiagram
    participant Ops as Release / DBA
    participant DB as MySQL (Comviva / Binge)
    participant CFG as ext-config rollout
    participant API as binge-mobile-services
    participant CMS as cms-api + cms-ui
    participant APP as Binge Apps / ATV
    participant CDN as static-content CDN

    Ops->>DB: Run db-scripts (pack + subscription cleanup)
    DB-->>Ops: Members / pack_partner / partners_included updated
    Ops->>CFG: Promote ext-config per environment
    CFG->>API: Services restart; new eligible / deboard lists
    Ops->>CMS: Deploy cms-api + cms-ui
    CMS-->>APP: Editorial + filters hide providers
    Ops->>API: Deploy binge-mobile-services
    API-->>APP: current subscription + deboardedPartnersList
    Ops->>CDN: Publish static-content JSON
    APP->>API: My Plan / search uses deboard lists + live channel map
```

---

## 4. Repository-by-repository purpose

| Repository | MR (reference) | Purpose of changes |
|------------|----------------|---------------------|
| **db-scripts** | !7353 | **Authoritative data removal:** delete Comviva rows / `pack_partner`, normalize `partners_included`, scrub subscription & cart & future entitlement strings; PTC-specific hybrid **`deboarded_partners`** / `partner_deborded` flag. |
| **ext-config** | !17944 | **Runtime toggles:** eligible partner lists, flexi complementary apps, deboard/search lists, pool list, automotive providers, `donot.send.partner.registration`, `block.registration.partner.list`, FAQ copy in brand JSON, `data-consumer` live purge behaviour, CMS delist keys, etc. |
| **cms-api** | !8599 | **Backend CMS behaviour:** remove PTC from automation constants; comment out **PTCPlay** editorial image wiring; `FilterOptionsServiceImpl` excludes **PTCPLAY** from provider filter options; `VODRepository` excludes PTC where applicable. |
| **cms-ui** | !15788 | **Operator UI:** remove **PTCPlay** from mappings, dropdowns, chip config, freemium shuffle, LIT action-init, asset count tables, SEO enum, notification logos; remove **HALLMARKMOVIESNOW** / **FUSE** from `providerLogo.js`. |
| **binge-mobile-services** | !13914 | **Subscriber APIs:** remove Hallmark/Fuse from `PartnerMapper` and `FiberPositionEnum`; consume **`deboarded.partner`**, **`deboard.partners.list`**, **`search.deboard.partners.list`**; complimentary partner stripping in `ProductHelperService`; verbiages in `VerbiagesServiceImpl`; `SubscriptionController` v4 current pack lists. |
| **static-content** | !377 | **Customer-facing copy:** FAQ answers (e.g. Firestick `helpFaqProd.json`) no longer list PTC Play, Fuse+, Hallmark+. |

---

## 5. Live channel partner deboarding — **PTC Play**

### 5.1 Overview

PTC Play deboarding ensures:

1. **Catalogue / editorial** tooling no longer treats PTC as a selectable provider.
2. **Hybrid subscribers** who had PTC in their pack are flagged for **replacement / My Plan** flows via **`deboarded_partners`**.
3. **Live channel** surfaces can be reconciled with **live channel mapping** APIs (where app version and platform qualify).

### 5.2 Database changes (`db-scripts`)

**Path / module:** `dml/PTCPlay/`

| Script | Intent |
|--------|--------|
| `ptcplay_deboarding_userMigration.sql` | Deletes `partner_logo`, `comviva_members_extension`, `comviva_component_member`, `pack_partner` for **partner_id 42**. Updates **`comviva_binge_subscriptions`**: sets **`partner_deborded = 1`**, **`deboarded_partners = 'PTCPlay'`** for **ACTIVATED** hybrid plans containing PTC. Scrubs **`static_pack_details`**, **`subscribed_partner`**, **`cart_details`**, **`comviva_entitlements_future`**. |
| `ptcplay_deboarding_subscription.sql` | Removes **ptcplay** token from **`partners_included`** in **`comviva_product`** and **`pack_details`** via `REPLACE` on comma-delimited lists. |

**Operational note:** Column name `partner_deborded` appears in scripts as stored in DB (typo vs English “deboarded”); applications must use the **actual schema** name.

### 5.3 Configuration changes (`ext-config`)

Representative patterns from commit **TAPL-9396** / **TSATV2-1665** (staging and service-specific folders):

- **`binge-mobile-config` / `binge-mobile-services`:** Remove **PTCPlay** from `partner.platform.mappings`, `auth.partner.mappings`, `fs.partner.platform.mappings`, `eligible.partners.for.platform.*`, `partners.for.super.249`, `partners.for.installer.pack`, `android.partner.bitrate`, `auto.providers*`, `deboard.partners.list`, `search.deboard.partners.list`, `partner.pack.mapping` (ptcplay entry), etc.
- **`donot.send.partner.registration`:** Append **42** (PTC partner id) so registration events are not sent for a removed partner.
- **`action-listener`:** Add **PTCPlay** to **`block.registration.partner.list`** to block new registrations.
- **`data-consumer`:** Adjust **live content delete / ignore partner** behaviour for controlled purge around PTC (exact property name per env file — verify in MR).
- **`cms-ui` (application-*.properties):** Extend **`application.deListedProviderForFiltering`** with **`PTCPLAY`** so CMS filters treat PTC as delisted.

### 5.4 API changes (`binge-mobile-services`)

| Module / class | Change |
|----------------|--------|
| `tv.videoready.api.v4.controller.SubscriptionController` | Injects **`deboard.partners.list`** and **`search.deboard.partners.list`**. For certain DTH flows, when response lists are empty, falls back to legacy SonyLiv/Bullet defaults; when response is null, sets lists from **config**. Applies **`filterLiveChannelsByPartnerMap`** when live channel mapping is enabled. |
| `tv.videoready.api.v3.service.impl.VerbiagesServiceImpl` | Uses **`deboarded.partner`** and deboarding popup / My Plan verbiage keys from config; uses subscription’s **`getDeboardedPartners()`** for multi-app prompts. |
| `tv.videoready.api.v3.utils.constants.ProductHelperService` | **`setComplimentaryPartner` / `setAtvComplimentaryPartner`:** removes partner matching **`deboarded.partner`** from complimentary and non-subscribed lists. |

### 5.5 CMS API & UI changes

**cms-api**

| File | Change |
|------|--------|
| `.../AutomationConstants.java` | Removes **PTCPlay** from **`DEFAULT_PROVIDERS`**. |
| `.../EditorialSectionServiceImpl.java` | Comments out **`provider.images.ptc`** injection and **PTCPlay** case in editorial image URL switch (falls through / no dedicated image). |
| `tv.videoready.service.impl.FilterOptionsServiceImpl` | Hardcoded exclusion set includes **`PTCPLAY`** so OpenAI / filter provider options omit PTC. |
| `.../repository/content/VODRepository.java` | Native queries exclude **`provider = 'PTCPlay'`** where applicable. |

**cms-ui**

| Area | Files (examples) | Change |
|------|------------------|--------|
| SEO | `SeoProvider.java` | Remove **`PTC_PLAY`**. |
| Editorial | `editorial/core.js`, `content-list-manager.js`, `v2/action-init.js` | Remove **PTCPLAY** enum / mapping / LIT subpage branch. |
| Chips / freemium | `chipConfiguration/dashboard.js`, `freemium/shuffleRail.js` | Remove **PTCPlay** chip mapping. |
| Reports | `AssetCountServiceImpl.java`, `report/assetCount.js` | Remove **PTCPlay** from ordered provider lists. |
| Notifications | `bingeMobileNotification/notification-init.js` | Remove **PTCPLAY** logo. |
| Provider logos | `content/providerLogo.js` | Remove **`logosUrl["PTCPLAY"]`**. |
| Templates | `layouts/contentFragment.html`, `intelliMetrics/partnerResponse.html` | Remove **PTCPLAY** `<option>` rows and static help table row referencing PTC Play. |

### 5.6 Static content

- **`static-content`:** e.g. `production/firestick/helpFaqProd.json` — FAQ strings updated to **drop “PTC Play”** from the enumerated app list (commit **TAPL-9396**).

### 5.7 Search / browse / live channel impact

- **Search / subscription API:** `search.deboard.partners.list` includes **PTCPlay** so client search and filtering can treat PTC as deboarded when config is aligned.
- **Live channels:** `SubscriptionController` integrates **`liveChannelMappingConnector`** and **`filterLiveChannelsByPartnerMap`** for supported app versions — **eligible live channel IDs** are derived after pack mapping; removing PTC from partner maps prevents PTC-backed channels from being offered where mapping is driven off partner lists.

### 5.8 Validation, filtering, edge cases

| Topic | Detail |
|-------|--------|
| **Hybrid-only PTC flagging** | `ptcplay_deboarding_userMigration.sql` scopes **`deboarded_partners`** updates to **`d.hybrid_plan = 1`** — non-hybrid subscribers follow list-scrubbing only. |
| **Case sensitivity in SQL** | Scripts use **`LOWER(...)`** with patterns like `%,ptcplay%` **and** separate passes for mixed-case tokens where required. |
| **Empty partner string** | `REPLACE` patterns must leave no dangling commas; validate on staging clone before prod. |
| **Config newline typos** | Staging diff once concatenated **`Bulletmarketing.drawer...`** to `search.deboard.partners.list` — **verify** production files for accidental line merges when porting MRs. |

---

## 6. Complimentary partner deboarding — **Hallmark+ & Fuse+**

### 6.1 Overview

Hallmark and Fuse are removed from:

- **Comviva relational model** (member id **34** / **35** in provided scripts — confirm per environment).
- **Complimentary** and **VIP** partner strings in `ext-config`.
- **Tick-tick pool** configuration (`pool.provider.list` shrinks to remaining pool partners, e.g. **APPLETV** only after deboard).
- **CMS operator logos** for Hallmark / Fuse keys.

### 6.2 Database changes (`db-scripts`)

**Path / module:** `dml/Hallmark/`, `dml/Fuse_prod/`

| Script | Intent |
|--------|--------|
| `23_04_2026_deboard_hallmark_from_pack.sql` | Deletes **`partner_logo`**, **`comviva_members_extension`**, **`comviva_component_member`**, **`pack_partner`** for Hallmark member **34**; scrubs **`partners_included`** in **`comviva_product`** / **`pack_details`** for `hallmarkmoviesnow` / `HallmarkMoviesNow`. |
| `23_04_2026_deboard_hallmark_from_subscription.sql` | Scrubs **`subscribed_partner`**, **`complimentary_partner`**, **`cart_details.partners`**, **`comviva_entitlements_future`** fields for Hallmark tokens (lower and mixed case). |
| `23_04_2026_deboard_fuse_from_pack.sql` | Same pattern for **Fuse** (member **35**; `partner_logo` row uses **`provider_name = 'Fuse'`** guard in workspace copy). |
| `23_04_2026_deboard_fuse_from_subscription.sql` | Same subscription/cart/future cleanup for **fuse** / **Fuse**. |

**Note:** A follow-up commit adjusted **`partner_logo.id`** targets (environment drift). Always **verify ids** in target DB before execute.

### 6.3 Configuration changes (`ext-config`)

From **TAPL-9396** / **TSATV2-1665**:

- Remove **HallmarkMoviesNow**, **Fuse** from **`eligible.partners`**, **`partnerProviders`**, **`partners.for.premium.vip`**, **`partners.for.mega.349`**, **`atv.flexi.complementary.app`**, **`complementary.category.mapping`**, **`hide.partner.web`**, **`android.partner.bitrate`**, **`auto.providers`**, **`pool.provider.list`**, platform eligible lists, etc.
- **`deboard.partners.list` / `search.deboard.partners.list`:** add **HallmarkMoviesNow**, **Fuse** (and **PTCPlay** where combined rollout).
- **`deboarded.partner`:** set to active single partner for complimentary stripping (e.g. **`PTCPlay`** during a PTC wave — **only one** token is compared in `removeIf` against each complimentary name; multi-partner waves may rely on **DB `deboarded_partners`** on subscription — confirm with latest `VerbiagesServiceImpl` / metadata overrides).

### 6.4 API changes (`binge-mobile-services`)

| File | Change |
|------|--------|
| `tv.videoready.api.enums.PartnerMapper` | Remove **`HallmarkMoviesNow`**, **`Fuse`** enum entries. |
| `tv.videoready.api.v3.enums.FiberPositionEnum` | Remove **`FUSE`**, **`HALLMARKMOVIESNOW`** position constants (downstream ordering / analytics). |

**Complimentary stripping (existing pattern):**

- `ProductHelperService#setComplimentaryPartner` — if **`deboarded.partner`** is set, removes that name from **`complimentaryPartnersList`** before building DTOs.

### 6.5 CMS UI changes (`cms-ui`)

| File | Change |
|------|--------|
| `static/javascripts/content/providerLogo.js` | Remove **`logosUrl["HALLMARKMOVIESNOW"]`** and **`logosUrl["FUSE"]`**. |

### 6.6 Static content

- Same **`helpFaqProd.json`** commit removes **“Fuse+”** and **“Hallmark+”** from the Sony LIV FAQ answer list.

### 6.7 Search / browse impact

- **`search.deboard.partners.list`** includes Hallmark and Fuse → search facet / client behaviour hides or deprioritizes those partners when wired to this list.
- **ATV `partner-mapping-staging.json` (TSATV2-1665):** removes browsable entries for Hallmark, Fuse, and PTC — **Android TV launcher / partner browser** no longer shows those tiles.

### 6.8 Edge cases

| Topic | Detail |
|-------|--------|
| **Partial token match** | SQL uses bounded comma patterns; edge case **single-token field** equal to `fuse` — may need extra `WHERE` if not covered. |
| **Case variants** | Duplicate `UPDATE` blocks handle **`fuse`** vs **`Fuse`** etc. |
| **Pool tick-tick** | Removing partners from **`pool.provider.list`** avoids offering dead pool slots; ensure **remaining** pool partners still satisfy product rules. |

---

## 7. Cross-cutting: **TSATV2-1665** (ATV / partner mapping / display names)

**Repository:** `ext-config`  
**Intent:** Staging cleanup removing **Fuse**, **Hallmark**, **PTC** from:

- **`partner.name.mapping`** JSON blobs across **amazon-prime-integration-service**, **binge-ops-suite**, **dnd-integration-service**, **it-connector**, **lit-subscription-service**, **login-service**, **partner-registration-engine**, **profile-mgmt**, **pubnub-router**, **sms-api**, **temp-login-service**, **it-connector-comviva**, **it-consumer-service**, etc.
- **`binge-service/atv-device-config-staging.properties`:** removes `thirdparty.fuse.logo`, `thirdparty.hallmarkMoviesNow.logo`, `thirdparty.ptc.logo.atv`, and related **square** logo URLs.
- **`binge-service/partner-mapping-staging.json`:** removes partner objects for **HallmarkMoviesNow**, **Fuse**, **PTCPlay**.

**Impact:** Large-screen / ATV experiences and any service that relied on **`partner.name.mapping`** for display strings stops surfacing deboarded partners after config refresh.

---

## 8. Deployment order & dependencies

Recommended **safe order** (adjust to your release calendar):

| Step | Component | Reason |
|------|-----------|--------|
| 1 | **db-scripts** (maintenance window) | Establishes correct **Comviva** and subscription truth before apps read new state. |
| 2 | **ext-config** + service restarts | APIs and consumers read lists / flags consistently. |
| 3 | **cms-api** + **cms-ui** | Prevents operators re-publishing rails with invalid providers. |
| 4 | **binge-mobile-services** | Aligns DTO mapping (`PartnerMapper`, Fiber enum) with config. |
| 5 | **static-content** CDN | Customer-visible copy matches actual catalogue. |
| 6 | **Caches / search reindex** (if applicable) | As per platform runbook — not in MR scope but often required. |

**Parallelism:** `cms-api` / `cms-ui` can often ship with `binge-mobile-services` in the same release train, but **DB must lead** or apps may briefly show inconsistent state.

---

## 9. Verification checklist (QA)

| # | Check |
|---|--------|
| 1 | Hybrid subscriber with legacy PTC: **`deboarded_partners`** populated; My Plan shows replacement journey. |
| 2 | New purchase: pack **does not** list Hallmark / Fuse / PTC in `partners_included`. |
| 3 | **ATV:** Partner hub / mapping JSON has **no** Hallmark, Fuse, PTC entries post-config. |
| 4 | **CMS:** Provider dropdown / content filters — **PTCPLAY** / Hallmark / Fuse not selectable where delisted. |
| 5 | **Search API / client:** Deboarded partners absent from search results for configured lists. |
| 6 | **static-content:** FAQ strings contain **no** deboarded partner names. |
| 7 | **Live channel mapping** (supported versions): eligible channel IDs exclude PTC-backed mapping when partner removed. |

---

## 10. Glossary

| Term | Meaning |
|------|---------|
| **Deboard** | Remove partner from commercial offering and technical allow-lists. |
| **`deboarded_partners`** | Column on **`comviva_binge_subscriptions`** — drives multi-partner replacement UX. |
| **`deboarded.partner`** | Single-partner Spring property consumed for complimentary list stripping. |
| **`deboard.partners.list`** | Config list used for cardinality / pack counting adjustments in services. |

---

## 11. Document control

| Version | Date | Author | Notes |
|---------|------|--------|-------|
| 1.0 | 2026-05-26 | Engineering (AI-assisted from workspace git + SQL) | Initial Confluence-ready draft; validate against merged MRs. |

---

*End of document.*
