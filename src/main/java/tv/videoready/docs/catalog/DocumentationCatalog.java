package tv.videoready.docs.catalog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry of packaged docs (Markdown files copied from repo {@code /docs} at build time).
 * Use {@link DocCategory} so the hub can show flows and APIs on separate home cards and nav items.
 */
public final class DocumentationCatalog {

    private static final List<DocFlow> FLOWS = Collections.unmodifiableList(Arrays.asList(
            new DocFlow(
                    "engineering-flows",
                    "All flow documentation (index)",
                    "ENGINEERING_FLOWS_INDEX.md",
                    "Single page listing every flow guide with links into each topic.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "engineering-apis",
                    "All API documentation (index)",
                    "ENGINEERING_APIS_INDEX.md",
                    "Single page listing binge-mobile-services API topics with links into each endpoint doc.",
                    DocCategory.API
            ),
            new DocFlow(
                    "onboarding",
                    "Partner onboarding (MR flow)",
                    "PARTNER_ONBOARDING_MR_FLOW.md",
                    "Merge-order checklist, layers, and code anchors for onboarding a new partner.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "deboarding-technical",
                    "Partner deboarding — technical",
                    "PARTNER_DEBOARDING_TECHNICAL.md",
                    "DB, config, CMS, APIs, search, and static content for removing a partner.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "deboarding",
                    "Partner deboarding — overview",
                    "PARTNER_DEBOARDING.md",
                    "Higher-level deboarding scope and notes.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "jhs-billing",
                    "JHS billing — steps & runbook",
                    "JHS_BILLING_STEPS.md",
                    "Billing initiative: Google Doc source of truth + engineering checklist and repo touchpoints.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "binge-media-search-ingestion",
                    "Binge media search — catalog ingestion",
                    "BINGE_MEDIA_SEARCH_INGESTION.md",
                    "Kafka + REST paths to Google Discovery Engine; curl verify/ingest; purge job; ops logging.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "third-party-connector-ingestion",
                    "Third-party connector → data-consumer ingestion",
                    "THIRD_PARTY_CONNECTOR_INGESTION_FLOW.md",
                    "syncData + manualIngestData, RabbitMQ exchange/queues, ext-config keys, MR !2958 / !1334.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "cdms-campaign-coupon",
                    "CDMS — campaigns & unique coupons",
                    "CDMS_COUPON_AND_CAMPAIGN_FLOW.md",
                    "cdms-core: save campaign, partners, generate/add-more coupons, bulk export, make-live; cdms-coupon-generator.",
                    DocCategory.FLOW
            ),
            new DocFlow(
                    "binge-mobile-generate-otp",
                    "Binge mobile — Generate OTP (public)",
                    "BINGE_MOBILE_GENERATE_OTP.md",
                    "POST /pub/api/{v}/user/authentication/generateOTP; freemium RMN OTP; headers and flow.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-validate-otp",
                    "Binge mobile — Validate OTP (public)",
                    "BINGE_MOBILE_VALIDATE_OTP.md",
                    "POST /pub/api/{v}/user/authentication/validateOTP; body OtpLoginRequestCO; session handoff.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-subscriber-details-v4",
                    "Binge mobile — Subscriber details (v4)",
                    "BINGE_MOBILE_SUBSCRIBER_DETAILS_V4.md",
                    "GET /api/v4/subscriber/details; RMN listing / login optimization; AccountDetailsDTO.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-update-user",
                    "Binge mobile — Update existing user",
                    "BINGE_MOBILE_UPDATE_EXIST_USER.md",
                    "POST /api/v3/update/exist/user; UpdateBingeMobileUserCO; freemium LOGIN_EXISTING_ACCOUNT.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-create-user",
                    "Binge mobile — Create new user (deprecated)",
                    "BINGE_MOBILE_CREATE_NEW_USER.md",
                    "POST /api/v1/create/new/user; deprecated create path; AddBingeMobileUserCO.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-verbiage-details",
                    "Binge mobile — Verbiage details",
                    "BINGE_MOBILE_VERBIAGE_DETAILS.md",
                    "GET /api/v1/verbiages/details; PromptVerbiagesDTO; config-driven UI copy.",
                    DocCategory.API
            ),
            new DocFlow(
                    "binge-mobile-v2-subscription-current",
                    "Binge mobile — Current subscription (v2)",
                    "BINGE_MOBILE_V2_SUBSCRIPTION_CURRENT.md",
                    "POST /api/v2/subscription/current; GetSubscriptionCO; PackDetailsDTO; hybrid DTH path.",
                    DocCategory.API
            )
    ));

    private DocumentationCatalog() {
    }

    public static List<DocFlow> all() {
        return FLOWS;
    }

    public static List<DocFlow> byCategory(DocCategory category) {
        return FLOWS.stream()
                .filter(f -> f.getCategory() == category)
                .collect(Collectors.toList());
    }

    public static Optional<DocFlow> findBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        String key = slug.trim().toLowerCase();
        for (DocFlow flow : FLOWS) {
            if (flow.getSlug().equalsIgnoreCase(key)) {
                return Optional.of(flow);
            }
        }
        return Optional.empty();
    }

    public static Optional<DocCategory> categoryOfSlug(String slug) {
        return findBySlug(slug).map(DocFlow::getCategory);
    }
}
