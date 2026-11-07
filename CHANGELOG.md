# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
One version covers the whole product (backend `pom.xml` + frontend `package.json`).

## [0.1.0] - 2026-11-07

First working version: an AI sales assistant on WhatsApp that sells only from the store's catalog and records confirmed orders.

### Added
- **WhatsApp Cloud API webhook**: Meta verification handshake, HMAC-SHA256 signature check, fast 200 reply with async processing.
- **Exactly-once replies**: webhook retries and concurrent deliveries are de-duplicated atomically in PostgreSQL.
- **Gemini replies in the customer's dialect** (Egyptian / Gulf / MSA), driven by an editable prompt file.
- **Model fallback chain**: on quota (429) or outage (5xx/timeout) the next Gemini model answers; a friendly fallback message is sent if all fail.
- **Catalog-only selling**: products (price, sizes, colors, stock) are injected into every request; anything not in the catalog is reported as unavailable, sold-out items get an alternative.
- **Order capture**: structured JSON output records the order when the customer confirms the summary; prices come from the catalog, unknown items flag the order for review, repeated confirmations do not duplicate it.
- **Dashboard (Next.js)**: products (list / add / edit / delete), orders (list + status), and a chat simulator that runs messages through the real pipeline alongside live WhatsApp.
- **Architecture guards**: Spring Modulith module-boundary test, ESLint feature-boundary rules, OpenAPI-generated TypeScript types.
- **Release version shown in the dashboard** and at `/actuator/info`.

[0.1.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.1.0
