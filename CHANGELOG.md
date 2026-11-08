# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
One version covers the whole product (backend `pom.xml` + frontend `package.json`).

## [0.3.0] - 2026-11-08

Sign-in for the dashboard: customer orders and the catalog are no longer open to anyone with the link.

### Added
- **Email + password sign-in** with a split-screen login page in the brand colors, in Arabic and English.
- **Every API and dashboard page needs a signed-in user**; only the WhatsApp webhook (protected by Meta's signature), health and login stay open.
- **First owner account** is created on the first start from `SALESBOT_ADMIN_EMAIL` / `SALESBOT_ADMIN_PASSWORD` (password at least 10 characters).
- **Brute-force guard**: after 5 wrong passwords for one email within 15 minutes, sign-in is paused for that email (429). A wrong email and a wrong password get the same message.
- **Header**: the signed-in user's name and a sign-out button.

### Security
- Session tokens are signed JWTs (HS256, 12 hours, `SALESBOT_JWT_SECRET`) checked by Spring Security on every call.
- The token lives in an `httpOnly` cookie and never reaches page JavaScript; browser calls go through the Next.js proxy, which adds it on the way to the backend.
- Passwords are stored as BCrypt hashes only.
- An expired or rejected session clears the cookie and returns to the login page.
- The proxy refuses changes (POST/PUT/PATCH/DELETE) coming from another site, and talks to the backend server-to-server (the browser's origin is not forwarded).
- `/v3/api-docs` is closed unless `SALESBOT_EXPOSE_API_DOCS=true` (development only).

### Changed
- Pages read their data on the server straight from the backend (`*-queries.ts`); client components only send changes (`*-api.ts`).

## [0.2.0] - 2026-11-07

Premium dashboard design with the Tawasol / Aqarat identity, two themes and two languages.

### Added
- **Two themes**: navy + gold (default) and maroon, same color tokens as the Tawasol web app; switch from the header, remembered per browser.
- **Arabic and English** with full RTL/LTR switching from the header; every screen text lives in typed dictionaries (a missing English key fails the build).
- **Overview page**: total sales, orders today, orders needing follow-up, available products, and the latest orders.
- **App frame**: brand-colored sidebar with icons, gold active marker and version; brand header with page title, bot status, theme and language switchers; slide-in menu on phones.
- **WhatsApp-style simulator**: chat bubbles, timestamps, read ticks, typing indicator.
- Tajawal font, Lucide icons, stat cards, select, card header, status pills.

### Changed
- Language and theme are read from cookies on the server, so the first paint already has the right direction and colors (no flash).
- Numbers use Western digits in Arabic too (1,250 ج.م.), and whole prices hide ".00".
- Customer and product texts pick their own direction, so Arabic data reads correctly inside the English UI.
- List pages share one loading/error flow (`load()` + `ListPage` error state).
- Animations are disabled for people who ask their system for reduced motion.

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

[0.3.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.3.0
[0.2.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.2.0
[0.1.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.1.0
