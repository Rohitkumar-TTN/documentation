# JHS billing — Hotstar recon & monthly counter (runbook)

| Field | Value |
|--------|--------|
| **Audience** | Engineering, SRE, billing / partner recon |
| **Google Doc** | [JHS billing steps (edit)](https://docs.google.com/document/d/1fNQCfQxiAGPBOXnOwBKX07udw18dQ8mWApf6h8f6dys/edit?tab=t.0) |
| **Primary DB (temp / staging)** | **`tatasky_tmp`** — execute mutating SQL **only** via **SRE** on approved windows. |

---

## 0. Security — credentials and secrets

- **Do not commit** database passwords, API keys, or CSV exports with PII into git.
- **Credential contact:** staging **writer** access for `tatasky_tmp` is coordinated with **Apoorva** / **SRE** (account name used for this flow: `apoorva_tmp`). Obtain the current password **only** via your org vault / SRE ticket — **never** paste it into this repo, Slack, or tickets in plain text.
- If a password was ever shared in an insecure channel, treat it as **compromised**: **rotate** with DBA/SRE and revoke old material.

**DB label (for humans):** *tatasky writer staging* → use the **`tatasky_tmp`** connection SRE approves for this runbook.

---

## 1. Naming convention (must do before every run)

Snapshot / billing tables are **suffix-dated**. Before running any script:

1. Replace **`<SUBSCRIPTION_SUFFIX>`** with the active snapshot, e.g. `dec2024`, `31_march`, `30_april_2026` (match the **actual** table names in `tatasky_tmp` for the billing month).
2. Replace **`<HISTORY_SUFFIX>`** the same way for `partner_subscription_history_*`.
3. Replace joined tables consistently: `subscriber_device_<SUFFIX>`, `comviva_binge_subscriptions_<SUFFIX>`, `comviva_binge_subscriber_<SUFFIX>` (same suffix as subscription tables for that cut).
4. Partner recon tables follow patterns such as:
   - `hotstar_billable_report_<DAY>_<Month>_<YEAR>` (example shape: `hotstar_billable_report_30_April_2026`)
   - `in_partner_not_in_tech_<month>_<year>` (example: `in_partner_not_in_tech_april_2026`)

**Date format:** align all literals to the billing policy (e.g. `'YYYY-MM-DD HH:mm:ss'`). **Change** `<CYCLE_ANCHOR>` / `<BILLING_MONTH_START>` below to the dates SRE/product sign off for **this** cycle.

---

## 2. Phase A — Reset `monthly_billing_counter` (via SRE on `tatasky_tmp`)

**Owner:** SRE executes on **`tatasky_tmp`** after peer review.

Use table name `partner_subscription_<SUBSCRIPTION_SUFFIX>` (example below used `dec2024`; swap suffix).

```sql
-- Counter = 0 for rows billed before cycle anchor
UPDATE partner_subscription_<SUBSCRIPTION_SUFFIX>
SET monthly_billing_counter = 0
WHERE monthly_billing_counter >= 1
  AND billing_start_date < '<CYCLE_ANCHOR>';

-- Counter = 1 for activations on/after cycle anchor (billing_start_date)
UPDATE partner_subscription_<SUBSCRIPTION_SUFFIX>
SET monthly_billing_counter = 1
WHERE billing_start_date >= '<CYCLE_ANCHOR>';

-- Counter = 1 for first_activation on/after cycle anchor
UPDATE partner_subscription_<SUBSCRIPTION_SUFFIX>
SET monthly_billing_counter = 1
WHERE first_activation_date >= '<CYCLE_ANCHOR>';

-- Subscribed rows still at 0 → set to 1
UPDATE partner_subscription_<SUBSCRIPTION_SUFFIX>
SET monthly_billing_counter = 1
WHERE monthly_billing_counter = 0
  AND subscription_status = 'SUBSCRIBED';
```

**Example anchor (replace every cycle):** `2024-12-01 00:00:00` was used historically; use the value agreed for the current JHS month.

---

## 3. Phase B — Validation extracts (then CSV via Jenkins)

After Phase A, run the **two** reporting queries below. **Before execution:** rename all `*_31_march` (or any frozen example suffix) to the **current month snapshot** tables (`partner_subscription_<SUBSCRIPTION_SUFFIX>`, `partner_subscription_history_<HISTORY_SUFFIX>`, matching `subscriber_device_*`, `comviva_binge_*`).

**Partner filter:** examples use `partner = 'hotstar'`; change if the recon partner changes.

### 3.1 Query 1 — billable base (non-transactional)

```sql
SELECT *
FROM (
    (
        SELECT
            sd.subscriber_id AS `Subscriber id`,
            ps.reference_id,
            unique_identifier,
            activation_date,
            deactivation_date,
            platform,
            first_activation_date,
            billing_start_date,
            billing_end_date,
            CASE
                WHEN monthly_billing_counter = 0 THEN 'NO'
                WHEN monthly_billing_counter = 1 THEN 'YES'
                ELSE 'YES-DOUBLE'
            END AS `Billable`,
            'NO' AS `isTransactionalType`
        FROM partner_subscription_<SUBSCRIPTION_SUFFIX> ps
        INNER JOIN subscriber_device_<SUBSCRIPTION_SUFFIX> sd
            ON ps.reference_id = sd.reference_id
        WHERE partner = 'hotstar'
          AND monthly_billing_counter > 0
    )
    UNION
    (
        SELECT
            cb.binge_subscriber_id AS `Subscriber id`,
            ps.reference_id,
            unique_identifier,
            activation_date,
            deactivation_date,
            'comviva' AS platform,
            first_activation_date,
            billing_start_date,
            billing_end_date,
            CASE
                WHEN monthly_billing_counter = 0 THEN 'NO'
                WHEN monthly_billing_counter = 1 THEN 'YES'
                ELSE 'YES-DOUBLE'
            END AS `Billable`,
            'NO' AS `isTransactionalType`
        FROM partner_subscription_<SUBSCRIPTION_SUFFIX> ps
        INNER JOIN comviva_binge_subscriptions_<SUBSCRIPTION_SUFFIX> cbs
            ON ps.reference_id = cbs.reference_id
        INNER JOIN comviva_binge_subscriber_<SUBSCRIPTION_SUFFIX> cb
            ON cbs.binge_subscriber_id = cb.binge_subscriber_id
        WHERE partner = 'hotstar'
          AND monthly_billing_counter > 0
    )
) AS t1
GROUP BY t1.unique_identifier;
```

### 3.2 Query 2 — transactional flows (last month window)

Adjust `CURDATE()` windows if the business window is not “last calendar month”.

```sql
SELECT *
FROM (
    (
        SELECT
            sd.subscriber_id AS `Subscriber id`,
            ps.reference_id,
            unique_identifier,
            activation_date,
            deactivation_date,
            platform,
            first_activation_date,
            billing_start_date,
            billing_end_date,
            'YES' AS `Billable`,
            'YES' AS `isTransactionalType`
        FROM partner_subscription_history_<HISTORY_SUFFIX> ps
        INNER JOIN subscriber_device_<SUBSCRIPTION_SUFFIX> sd
            ON ps.reference_id = sd.reference_id
        WHERE partner = 'hotstar'
          AND flow_name IN (
              'modify', 'UPGRADE', 'MODIFY_COMBO', 'DOWNGRADE',
              'DEVICE_UPGRADE', 'PARTNER_UPDATE'
          )
          AND ps.date_created BETWEEN CURDATE() - INTERVAL 1 MONTH AND CURDATE()
          AND activation_date BETWEEN CURDATE() - INTERVAL 1 MONTH AND CURDATE()
          AND first_activation_date IS NOT NULL
    )
    UNION
    (
        SELECT
            cb.binge_subscriber_id AS `Subscriber id`,
            ps.reference_id,
            unique_identifier,
            activation_date,
            deactivation_date,
            'comviva' AS platform,
            first_activation_date,
            billing_start_date,
            billing_end_date,
            'YES' AS `Billable`,
            'YES' AS `isTransactionalType`
        FROM partner_subscription_history_<HISTORY_SUFFIX> ps
        INNER JOIN comviva_binge_subscriptions_<SUBSCRIPTION_SUFFIX> cbs
            ON ps.reference_id = cbs.reference_id
        INNER JOIN comviva_binge_subscriber_<SUBSCRIPTION_SUFFIX> cb
            ON cbs.binge_subscriber_id = cb.binge_subscriber_id
        WHERE partner = 'hotstar'
          AND flow_name IN (
              'modify', 'UPGRADE', 'MODIFY_COMBO', 'DOWNGRADE',
              'DEVICE_UPGRADE', 'PARTNER_UPDATE'
          )
          AND ps.date_created BETWEEN CURDATE() - INTERVAL 1 MONTH AND CURDATE()
          AND activation_date BETWEEN CURDATE() - INTERVAL 1 MONTH AND CURDATE()
          AND first_activation_date IS NOT NULL
    )
) AS t1
GROUP BY t1.unique_identifier;
```

### 3.3 Export CSV (Jenkins)

- **Job:** [db-scripts-export-csv (non-prod Jenkins)](https://non-production-jenkins.internal.videoready.tv/job/db-scripts-export-csv/)
- **Database:** `tatasky_tmp`
- Run for **both** queries above; **download** the resulting CSVs through the job artefact path SRE documents per run.

---

## 4. Phase C — After partner sends their file

### 4.1 Create import table

Name pattern: `in_partner_not_in_tech_<month>_<year>` (example: `in_partner_not_in_tech_april_2026`).

```sql
CREATE TABLE in_partner_not_in_tech_<MONTH>_<YEAR> (
    recon VARCHAR(2) DEFAULT NULL,
    hid VARCHAR(32) DEFAULT NULL,
    partner_subscription_id VARCHAR(69) DEFAULT NULL,
    status VARCHAR(7) DEFAULT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    partial VARCHAR(255) DEFAULT NULL,
    INDEX hid (hid),
    INDEX partner_subscription_id (partner_subscription_id),
    INDEX status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
```

### 4.2 Load partner CSV

Import the partner-supplied CSV into the table created in §4.1 (tooling: `LOAD DATA`, DBA-approved import pipeline, or Jenkins import — follow SRE standard).

### 4.3 Populate `partial` for matching

**Billable report table** name must match the month’s report, e.g. `hotstar_billable_report_30_April_2026` → use **`hotstar_billable_report_<REPORT_SUFFIX>`**.

```sql
UPDATE hotstar_billable_report_<REPORT_SUFFIX>
SET partial = SUBSTRING(unique_identifier, 1, LENGTH(unique_identifier) - 15)
WHERE unique_identifier LIKE '%_Tatasky.IN%'
   OR unique_identifier LIKE '%Tatasky.P%'
   OR unique_identifier LIKE '%Tatasky.S%';

UPDATE in_partner_not_in_tech_<MONTH>_<YEAR>
SET partial = SUBSTRING(partner_subscription_id, 1, LENGTH(partner_subscription_id) - 15)
WHERE partner_subscription_id LIKE '%_Tatasky.IN%'
   OR partner_subscription_id LIKE '%Tatasky.P%'
   OR partner_subscription_id LIKE '%Tatasky.S%';
```

---

## 5. Phase D — Classify mismatches (`reason`)

Use the same **subscription snapshot** suffix as the billing month for joins (example names below use `_30_april` / `_April_2026` — replace everywhere).

### 5.1 Unique identifier mismatch

```sql
UPDATE in_partner_not_in_tech_<MONTH>_<YEAR> ps
JOIN hotstar_billable_report_<REPORT_SUFFIX> hs
    ON ps.partial = hs.partial
SET ps.reason = 'Unique Identifier mismatch';
```

### 5.2 Deactivation within billing cycle (rule example)

```sql
UPDATE in_partner_not_in_tech_<MONTH>_<YEAR> AS pt
JOIN partner_subscription_<SUBSCRIPTION_SUFFIX> AS ps
    ON pt.partner_subscription_id = ps.unique_identifier
SET pt.reason = 'Deactivation with in billing cycle'
WHERE ps.billing_start_date < '<BILLING_MONTH_START>';
```

Replace `<BILLING_MONTH_START>` with the first instant of the billing month under scrutiny (example shape: `'2026-04-01 00:00:00'`).

### 5.3 Wrong UUID pattern

**Inspect:**

```sql
SELECT *
FROM in_partner_not_in_tech_<MONTH>_<YEAR>
WHERE partner_subscription_id NOT LIKE '%Tatasky%';
```

**Update:**

```sql
UPDATE in_partner_not_in_tech_<MONTH>_<YEAR>
SET reason = 'WrongUUID'
WHERE partner_subscription_id NOT LIKE '%Tatasky%'
  AND reason IS NULL;
```

### 5.4 Remaining `reason IS NULL` — Mongo `partner_transaction`

For rows still **`reason IS NULL`**:

1. Take `partner_subscription_id` from `in_partner_not_in_tech_<MONTH>_<YEAR>`.
2. In **MongoDB**, open collection **`partner_transaction`** and query by **`unique_identifier`** equal to that `partner_subscription_id` (confirm exact field name in your environment’s document schema).

**Manual analysis (sampled IDs):**

| Check | Action |
|-------|--------|
| **Expired / billing edge** | If billing cycle end falls **inside** the billing month being recon’d, set `reason` to **`Deactivation with in billing cycle`** (after validating dates in MySQL snapshot + partner file). |
| **Active but “new entry”** | If Mongo shows behaviour consistent with **“Not found in Tech as new entry created”** (new row created, prior row updated, deactivation not sent), set that literal as `reason`. |

Persist updates with controlled `UPDATE ... SET reason = '...' WHERE partner_subscription_id = ?` (or batch rules SRE approves).

---

## 6. Order of execution (checklist)

1. [ ] Confirm table suffixes for **this** month in `tatasky_tmp`.
2. [ ] Replace all placeholders (`<CYCLE_ANCHOR>`, `<BILLING_MONTH_START>`, table suffixes).
3. [ ] SRE executes Phase A `UPDATE`s on `tatasky_tmp`.
4. [ ] Run Phase B queries; export CSVs via Jenkins (`tatasky_tmp`).
5. [ ] Partner returns file → §4 create table → import → partial updates.
6. [ ] Run §5.1 → §5.3 in order; review counts.
7. [ ] Mongo + manual classification for remaining null `reason` (§5.4).
8. [ ] Archive CSVs and query logs per compliance (not in public git).

---

## 7. Definition of Done

- [ ] Counters and extracts match the **signed-off** billing month.
- [ ] Both Jenkins CSV artefacts stored per retention policy.
- [ ] `in_partner_not_in_tech_*` has **no unexpected** null `reason` rows (or documented exceptions).
- [ ] Google Doc / ticket updated with **actual** suffixes and dates used.

---

## 8. Browse in HTML

Run [`engineering-docs-hub`](../engineering-docs-hub) and open **http://localhost:8095/flows/jhs-billing** (`./gradlew bootRun` with JDK 8 for this repo’s Gradle wrapper).
