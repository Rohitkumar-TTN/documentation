# Partner deboarding — runbook and system map

This document describes how **partner deboarding** is implemented across Tata Play Binge / VideoReady stacks. It is split into:

1. **Live channels deboarding (PTC Play / `PTCPlay`)** — partner that carries **live channel** style content and needs **content pipeline / purge / CMS** handling.
2. **Complimentary partner deboarding (Hallmark+ / `HallmarkMoviesNow`, Fuse+ / `Fuse`)** — partners delivered primarily as **complimentary (bundle) apps** with **subscription, My Plan, flexi exclusions, and registration queues** handling.

**Sources:** Your six merge requests (repository scope) plus the current workspace. The internal GitLab diff URLs were not reachable from the documentation environment; after merge, reconcile this file with the actual MR diffs if anything diverges.

| MR (GitLab) | Repository | Typical role in deboarding |
|-------------|------------|----------------------------|
| [ext-config !17944](https://gitlab.intelligrape.net/videoready-tatasky/ext-config/-/merge_requests/17944/diffs) | `ext-config` | Central **properties**: deboard lists, verbiages, delisted providers, live purge exceptions, queues. |
| [cms-ui !15788](https://gitlab.intelligrape.net/videoready-tatasky/cms-ui/-/merge_requests/15788/diffs) | `cms-ui` | **Editorial / LIT** tooling: provider keys, logos, rails, pages that reference `PTCPlay`, Hallmark, Fuse. |
| [binge-mobile-services !13914](https://gitlab.intelligrape.net/videoready-tatasky/binge-mobile-services/-/merge_requests/13914/diffs) | `binge-mobile-services` | **App APIs**: deboarding popups, My Plan, `deboardedPartners` on subscription, complimentary partner stripping. |
| [cms-api !8599](https://gitlab.intelligrape.net/videoready-tatasky/cms-api/-/merge_requests/8599/diffs) | `cms-api` | **CMS backend**: provider images, filters, queries that exclude or special-case providers. |
| [db-scripts !7353](https://gitlab.intelligrape.net/videoready-tatasky/db-scripts/-/merge_requests/7353/diffs) | `db-scripts` | **Data**: pack / partner / entitlement rows, seeds, or cleanup scripts for deboarded partners. |
| [static-content !377](https://gitlab.intelligrape.net/videoready-tatasky/static-content/-/merge_requests/377/diffs) | `static-content` | **Public copy**: help/FAQ, EULA, terms where partner names appear. |

---

## Partner keys (naming)

Use these consistently across config, CMS, and mobile:

| Partner (product) | Common config / CMS keys | Notes |
|-------------------|-------------------------|--------|
| PTC Play | `PTCPlay`, `PTCPLAY`, `ptcplay` | Live-channel oriented; casing varies by service. |
| Hallmark+ | `HallmarkMoviesNow`, `HALLMARKMOVIESNOW` | Complimentary / OTT bundle partner. |
| Fuse+ | `Fuse`, `FUSE`, `fuseplus` (images) | Complimentary / OTT bundle partner. |

---

## 1. Live channels deboarding — PTC Play

**Goal:** Stop **live and related catalogue** surfacing for PTC Play, avoid bad purge behaviour during transition, and hide or delist the provider in **CMS and filters** where appropriate.

### 1.1 Configuration (`ext-config`)

- **`data-consumer`** (`ext-config/data-consumer/application-*.properties`):
  - `live.content.delete.ignore.partner=ptcplay` — **exempts** PTC Play from automatic **live content delete** logic where that property is consumed (controlled purge during deboarding / migration).
- **`content-detail`**:
  - `ptcPlay.provider.partnerId` — partner id used for content / connector flows.
- **`binge-mobile-services` / `binge-mobile-config`**:
  - PTC Play appears in **deboarding partner lists** (see §3) so apps can treat the user journey consistently; live-specific behaviour still centres on **content services** above.

### 1.2 CMS and API (`cms-api`, `cms-ui`, `ext-config` → `cms-ui`)

- **`ext-config/cms-ui`**: `application.deListedProviderForFiltering` includes **`PTCPLAY`** alongside other delisted providers so **editorial / filtering** UIs do not offer PTC Play where delisting applies.
- **`cms-api`**:
  - `FilterOptionsServiceImpl` excludes **`PTCPLAY`** from certain **OpenAI / filter provider** option sets (hardcoded exclusion list alongside other delisted providers).
  - `VODRepository` contains queries that **exclude `provider = 'PTCPlay'`** where VOD/live catalogue rules require it.
- **`cms-ui`**: static JS maps and LIT editorial flows reference **`PTCPlay` / `PTCPLAY`** (logos, page sections, content sync skips). MR `cms-ui` changes typically adjust **visibility, logos, or editorial eligibility** for deboarded live partner.

### 1.3 Integrations and metrics

- **`cms-ui`** → `intellimetrics/contentSync.js`: `skipMMIdList` includes **`ptcplay`** so **content sync / metrics** do not treat PTC as a normal MM id where skipped.
- **`automaticContentIntegrator/partnerIngestionTimings.js`**: **`PTC_PLAY`** maps ingestion types including **`live`** — aligns PTC with **live ingestion** semantics.

### 1.4 Mobile / subscriber surface

- **`binge-mobile-services`**: constants and display names for **`PTCPlay` / `PTC Play`**; PTC may appear in **`atv.binge.discontinued.app`**, **`deboard.partners.list`**, **`myPlan.deboard.partners.list`**, and related verbiages so **large screen / ATV** and **My Plan** reflect removal.

### 1.5 Static content (`static-content`)

- Update **help / FAQ / legal** JSON or HTML that list **“PTC Play”** in “apps on your plan” copy so customers are not told they still have access after deboarding (see MR `static-content`).

### 1.6 Database (`db-scripts`)

- MR `db-scripts` typically adds **migrations or one-off SQL** to: deactivate partner rows, adjust **pack ↔ partner** mappings, or clean **live event / EPG** references. Validate against **production** runbooks before execute.

---

## 2. Complimentary partner deboarding — Hallmark+ and Fuse+

**Goal:** Remove partners from **complimentary (included) app** surfaces, **My Plan replacement** flows, **flexi / plan** exclusions, and **partner registration queues**, without conflating them with **live purge** rules used for PTC.

### 2.1 Runtime behaviour (`binge-mobile-services`)

- **`ProductHelperService`**:
  - Reads **`deboarded.partner`** from Spring config (and related metadata where applicable).
  - **`setComplimentaryPartner`**: removes the deboarded id from **`complimentaryPartnersList`** before building **non-subscribed** and **complimentary** `PartnerDTO` lists so **UI does not show** the deboarded complimentary app.
  - **`setAtvComplimentaryPartner`**: removes deboarded partner from **`nonSubscribedPartnerList`** and **`subscriberPartnersList`** for **ATV** pack presentation.
- **`VerbiagesServiceImpl`**:
  - Injects **`deboard.partners.list`**, **`deboarding.*` popup** strings, **`myPlan.deboard.partners.list`**, and builds **deboarding popups / My Plan** payloads from **`comvivaBingeSubscriptions.getDeboardedPartners()`** when present.
- **`Constants`**: **`DEBOARDING`**, **`PTCPLAY`**, **`DEBOARDED_PARTNER`** and related keys tie **analytics / static pack** metadata to deboarding.

### 2.2 Configuration (`ext-config` → `binge-mobile-services`)

Representative keys (environment-specific files such as `application-uat.properties`):

- **`deboarded.partner`** — drives **complimentary** stripping (see code above). *Operational note:* downstream code compares this to **individual** partner names in lists; confirm per environment whether this is a **single** active deboard or overridden from **metadata** (`DEBOARDED_PARTNER` / static pack details) when multiple partners deboard in parallel.
- **`deboard.partners.list`**, **`search.deboard.partners.list`**, **`myPlan.deboard.partners.list`** — include **`HallmarkMoviesNow`**, **`Fuse`**, and **`PTCPlay`** for **search / My Plan / cardinality** behaviour.
- **`exclude.partner.flexi.*`**, **`exclude.partner.infinity`** — often list **`HallmarkMoviesNow`**, **`Fuse`** (and **`PTCPlay`**) so **plan picker** does not offer invalid combinations.
- **`rabbitmq.queue.fuse`**, **`rabbitmq.queue.hallmarkMoviesNow`** — **registration / job** queues for partner onboarding; deboarding MRs may **drain, disable, or redirect** these.
- **Provider display names**: `provider.name.fuse`, `provider.name.hallmarkMoviesNow` (and `provider.name.ptcplay` for cross-cutting copy).

### 2.3 CMS delisting (`ext-config/cms-ui`, `cms-api`)

- **`application.deListedProviderForFiltering`**: includes **`HALLMARKMOVIESNOW`**, **`FUSE`** (and **`PTCPLAY`**) so **CMS filters** hide delisted providers.
- **`cms-api`**: provider image URLs for Hallmark / Fuse; **`EditorialSectionServiceImpl`** / DTO mapping for **`HALLMARKMOVIESNOW`** (and related) for **rails and sections**.

### 2.4 Other services

- **`subscriber-cache-manager`**, **`live-event-service`**, **`event-processor`**, **`homescreen-consumer`**, **`ta-worker`**, etc.: **`eligible.partners`** (or equivalent) lists are trimmed in MRs so **cache, TA, and homescreen** do not advertise deboarded complimentary partners.

### 2.5 Static content

- Remove or rewrite **FAQ answers** that still list **Hallmark+**, **Fuse+**, or **PTC Play** as active apps on plan (e.g. help JSON under `static-content/uat|staging|production/...`).

---

## 3. Shared mobile / journey configuration

These apply to **both** tracks where the same app journey is used:

- **`deboarding.popup.verbiages.*`** — titles, subtitles (Android / iOS / large screen), CTA labels.
- **`deboarding.one.app.popup.*`**, **`deboarding.all.apps.popup.*`** — templates with **`[appname]`** placeholder for one vs many deboarded apps.
- **`deboarding.partner.popup.frequency`** (where present) — caps how often users see replacement prompts.
- **`Constants.DEBOARDING`** and subscription fields storing **`deboardedPartners`** — drive **prompts** and **“choose replacement app”** flows.

---

## 4. Deployment checklist (per environment)

1. **`ext-config`**: merge and **promote** properties for **UAT → staging → production** in the order your platform team requires; restart **binge-mobile-services**, **cms-api**, **data-consumer**, and any service reading **`eligible.partners`** or **`deListedProviderForFiltering`**.
2. **`cms-api` / `cms-ui`**: deploy after config so **delisted filters** and **UI assets** align.
3. **`binge-mobile-services`**: deploy with matching **`deboard.partners.list`** and **`deboarded.partner`** (or metadata) so **app** and **API** stay consistent.
4. **`db-scripts`**: run **approved SQL** during the agreed window; verify **Comviva / pack** rows and **Mongo** subscription documents if your runbook includes them.
5. **`static-content`**: publish CDN / static host updates for **help and legal** copy.
6. **Validation:** search API **My Plan**, **complimentary rail**, **live guide / search** (PTC), **plan purchase** (Hallmark/Fuse exclusions), and **CMS editorial filters**.

---

## 5. Traceability back to merge requests

After your MRs merge, add the **ticket id**, **release name**, and **actual file list** from each MR diff here so operations can audit without reading code:

| Area | MR | Post-merge notes |
|------|-----|------------------|
| Live (PTC) | ext-config !17944, cms-api !8599, cms-ui !15788, data-consumer (within ext-config) | |
| Complimentary (Hallmark, Fuse) | ext-config !17944, binge-mobile-services !13914, cms-api !8599 | |
| Copy / legal | static-content !377 | |
| Data | db-scripts !7353 | |

---

## 6. Limitations of this document

- **MR diffs were not fetched** from `gitlab.intelligrape.net` from this environment; content is inferred from **repository layout**, **your partner split (PTC vs Hallmark/Fuse)**, and **checked-in UAT properties / code**.
- **Production** property values may differ from **UAT**; always compare the **target env** file before go-live.

If you paste **export of each MR diff** (or ticket acceptance criteria) into the chat, this file can be updated to line-level accuracy.
