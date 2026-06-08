# Engineering docs hub

Single **Spring Boot** app that serves Tata Play engineering documentation (Markdown under repo [`/docs`](../docs)) as **HTML** via `@Controller` + Thymeleaf. The **home** screen separates **flow** runbooks from **API** reference; each area has one index page that links to all topics in that area.

## Run locally

From this directory. **Important:** the Gradle **4.8** wrapper does not run on **JDK 21**. Use **JDK 8** (or JDK 11 with this wrapper if it works on your machine), for example:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64   # Linux example
./gradlew bootRun
```

Open [http://localhost:8095/](http://localhost:8095/) — **home** shows two cards only: **Flows** (all runbooks on `/flows/engineering-flows`) and **APIs** (all REST reference on `/flows/engineering-apis`). The header is **Home | Flows | APIs** so flow and API docs are not mixed in one list. Individual topics remain at `/flows/{slug}` (for example `/flows/onboarding`, `/flows/binge-mobile-generate-otp`).

## Add a new document

1. Add or update a `.md` file under the repo root [`docs/`](../docs) folder.
2. Register it in `DocumentationCatalog.java` with a **category**: `DocCategory.FLOW` (runbooks) or `DocCategory.API` (REST reference). Use a unique slug and the exact filename.
3. Add a row to `ENGINEERING_FLOWS_INDEX.md` or `ENGINEERING_APIS_INDEX.md` linking to `/flows/{slug}` on the hub.
4. Rebuild; Gradle copies `../docs/*.md` into the JAR classpath as `documentation/` during `processResources`.

## Deploy

Treat as **internal-only** (VPN / SSO / network policy). Default port: **8095** (`application.properties`). No database or external calls.

## Stack

- Java 8, Spring Boot **1.5.22**, Gradle **4.8** wrapper (same era as sibling Tata Play services), Thymeleaf, CommonMark + GFM tables for Markdown → HTML.

First `./gradlew` run may download the Gradle distribution and Maven dependencies (needs network).
