# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
One version covers the whole product (backend `pom.xml` + frontend `package.json`).

## [0.6.0] - 2026-11-08

Sell anything, with photos: products of any kind with their own options and prices, a built-in photo editor, and the bot sends product photos on WhatsApp.

### Added
- **Product photos**: up to 8 per product. Drop them, pick several at once, or take one with the phone camera. The first photo is the main one (gold badge); reorder by dragging or with the arrow buttons, edit or remove any photo. Thumbnails in the product list.
- **Photo editor** in the dashboard: square, portrait (4:5) or original shape; drag to move, mouse wheel or two fingers to zoom; rotate and flip; "whole photo on white" for items that don't fit a square; **auto enhance** (levels + gentle mid-tone lift for dark photos) and brightness / contrast / saturation / warmth sliders. Exports a sharp ~1200px JPEG and drops the camera's hidden data (location, device).
- **Any kind of product**: free options (size, colour, storage, volume, weight, ...) with chip-style values; each value can be sold out on its own, and one option may give each value its own price (e.g. 128GB at 20,000 and 256GB at 23,000). Free specs (brand, warranty, material, ...).
- **Product kinds**: clothes, shoes, phones & electronics, perfume & beauty, food, accessories, furniture, books, other. Picking one suggests its usual options and specs; adding a kind is one entry in `product-templates.ts`.
- **The bot sends product photos** on WhatsApp when the customer asks to see a product or when it first offers one (up to 3 per reply), and the photo shows in the conversation inbox. Photos are uploaded to WhatsApp once and reused (re-uploaded after 25 days or if WhatsApp drops them).
- **API** `POST /api/products/{id}/images`, `DELETE /api/products/{id}/images/{imageId}`, `PUT /api/products/{id}/images/order`, `GET /api/products/images/{imageId}?size=thumb|full`.

### Changed
- The bot is a general online-store seller (no longer "clothes and shoes"); it asks for every option a product has before closing an order.
- Order prices follow the chosen option; an unknown or sold-out choice flags the order for review. Order lines show the chosen options.
- Products list shows "from" the lowest price when prices depend on an option.
- `V6` migration moves existing sizes and colours into the options "المقاس" and "اللون" (and the same for order lines).

### Upgrade note
- Stop the old backend before starting this one: `V6` replaces the sizes/colours columns, so the old version can't run against the upgraded database.

## [0.5.1] - 2026-11-08

Fixes found while trying v0.5.0 end to end.

### Fixed
- **Simulator in a chat you took over**: the customer's message no longer ends in an error while the bot is paused, and your replies from the dashboard now appear in the simulator chat within a few seconds.
- **Unread count in the browser tab title** ("(3) Bayaa") no longer disappears after moving between pages or when the page refreshes itself.
- **Pop-up notifications** now appear at the top, under the header, so they never cover the reply box's send button.

### Added
- **API** `GET /dev/simulator/messages/{customerId}`: messages the shop sent to a simulator chat that it hasn't shown yet.

## [0.5.0] - 2026-11-08

Take over any chat, and never miss a customer: reply from the dashboard and get notified with a sound.

### Added
- **Reply from the dashboard**: write to the customer from the chat page; the message goes out on WhatsApp and shows as "You" in the thread. Enter sends, Shift+Enter adds a line.
- **Bot pause per chat**: the bot stops answering a chat as soon as you reply there (or when you press "Pause bot"); customer messages keep arriving in the inbox. "Hand back to bot" turns it on again, and the bot continues with your replies in its context. Paused chats are marked "You're replying" in the list.
- **WhatsApp 24-hour rule**: if the customer's last message is older than 24 hours, the reply box explains why it is closed instead of failing.
- **Notification bell** in the header: new customer messages and new orders with an unread badge (also in the browser tab title), a pop-up card and a sound. Different chimes for a message, an order, and something that needs you (a message in a chat you took over, or an order that needs review). Sound and device alerts can be switched on or off per browser; device alerts show while the tab is in the background (needs HTTPS or localhost).
- **API** `POST /api/conversations/{id}/messages`, `PUT /api/conversations/{id}/bot`, `GET /api/notifications?after=`.

### Changed
- Every message records who wrote it: customer, bot, or a person from the shop (`V5` migration fills existing messages).
- If you take over a chat while the bot is still thinking, the bot's reply is dropped, so the customer never gets two answers.

### Upgrade note
- Restart the backend right after updating: the `V5` migration and the new code must run together.

## [0.4.0] - 2026-11-08

Conversations inbox: see every chat the bot had, as it happens.

### Added
- **Conversations page**: WhatsApp-like chat list (customer, last message, time, "bot replied" marker, test-chat badge) next to the full thread; on phones the list and the chat are separate screens.
- **Chat thread**: messages in order with day separators, the bot's replies on the shop's side; refreshes by itself every few seconds (paused when the tab is hidden).
- **API** `GET /api/conversations` (latest activity first, one query with the last message and count) and `GET /api/conversations/{id}`.
- `scripts/start-bot.ps1`: starts database, backend and tunnel and points Meta's webhook at the new tunnel URL (for a personal machine or server, not a managed device).

### Changed
- One shared WhatsApp-style message bubble for the simulator and the inbox.
- The simulator's channel id is part of the messaging module's public API (`SimulatorChannel`).

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
- An expired or rejected session clears the cookie and returns to the login page; another site can't sign you out through that page.
- The proxy refuses changes (POST/PUT/PATCH/DELETE) coming from another site (it understands `X-Forwarded-Host`, so it works behind cloudflared/nginx) and talks to the backend server-to-server.
- The session cookie is `Secure` whenever the browser came over HTTPS (also behind a tunnel), and plain HTTP on the LAN still works.
- The brute-force guard keeps bounded memory even when random emails are sprayed.
- `/v3/api-docs` is closed unless `SALESBOT_EXPOSE_API_DOCS=true` (development only).

### Changed
- Pages read their data on the server straight from the backend (`*-queries.ts`); client components only send changes (`*-api.ts`).
- The frontend's backend address is `SALESBOT_BACKEND_URL` (server side only) instead of `NEXT_PUBLIC_API_BASE_URL`.

### Removed
- Backend CORS settings (`SALESBOT_CORS_ALLOWED_ORIGINS`): the browser never calls the backend directly anymore.

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

[0.6.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.6.0
[0.5.1]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.5.1
[0.5.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.5.0
[0.4.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.4.0
[0.3.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.3.0
[0.2.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.2.0
[0.1.0]: https://github.com/amer-rouby/whatsapp-sales-bot/releases/tag/v0.1.0
