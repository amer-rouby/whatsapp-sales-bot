# Bayaa — WhatsApp Sales Bot

An AI sales assistant on WhatsApp for online stores. It answers customers in their own dialect (Egyptian, Gulf, MSA), sells **only** from the store's catalog, and records confirmed orders in a dashboard.

![CI](https://github.com/amer-rouby/whatsapp-sales-bot/actions/workflows/ci.yml/badge.svg)

## How it works
```
Customer (WhatsApp) → Meta Cloud API → /webhook (signature check) → event
  → conversation: store message → ask Gemini (catalog + history) → store reply → send text (+ product photos)
                                   └─ order confirmed? → orders (prices from catalog)
Dashboard (Next.js) → /api/products (+ photos), /api/orders, /api/conversations (inbox + shop replies),
                      /api/notifications (bell, polled), /dev/simulator
```

## Structure
```
backend/    Spring Boot 4.1 (Java 17) — modular monolith, Spring Modulith guards module boundaries
frontend/   Next.js 16 + Tailwind v4 — feature-based, shared UI kit, Arabic RTL
docker-compose.yml   PostgreSQL 17 on port 5442
```

### Backend modules (`com.salesbot.*`)
| Module | Role |
|---|---|
| `messaging` | WhatsApp webhook in, sender out (text and photos; uploaded photos are reused via `wa_media_cache`), dashboard simulator. Publishes `InboundMessageReceived`. |
| `conversation` | The reply loop (exactly-once per WhatsApp message), the inbox, and the shop's own replies (pauses the bot per chat, WhatsApp 24-hour rule). `CustomerMessages` is its read API. |
| `notifications` | One newest-first feed of customer messages and orders for the dashboard bell. |
| `ai` | `AiReplyProvider` — Gemini with a model fallback chain, or `echo` for local work. Structured JSON reply + photos to send + order. |
| `catalog` | Products of any kind (options with their own prices and stock, specs) and their photos (validated and re-encoded server-side, stored in PostgreSQL). `CatalogQuery` is what the bot may sell, prices an order line, and gives the main photo. |
| `orders` | Orders API; `OrderPlacement` records confirmed orders with catalog prices, `RecentOrders` feeds notifications. |
| `merchant` | Reserved for multi-store accounts. |
| `shared` | Base entity, ProblemDetail errors, security, async. |

A module's root package is its public API; sub-packages are internal. `ModularityTests` fails the build on violations or cycles.

### Frontend
- `src/app/` routes only, no logic
- `src/features/<name>/` screens; imported only via their `index.ts` (enforced by ESLint)
- `src/shared/ui` reusable components, `src/shared/components/list-page.tsx` config-driven list screens
- `src/shared/api/schema.ts` generated from the backend OpenAPI: `npm run gen:api`
- Colors/fonts live only in `src/app/globals.css` (`@theme`)

## Run locally
```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```
Local settings go in `backend/.env` (gitignored, same keys as `.env.example`, all prefixed `SALESBOT_`).
Required for the dashboard: `SALESBOT_JWT_SECRET` (at least 32 characters, e.g. `openssl rand -hex 32`) and, for the first start,
`SALESBOT_ADMIN_EMAIL` + `SALESBOT_ADMIN_PASSWORD` (at least 10 characters) — that account is created once and is how you sign in.
With no Gemini key the bot runs in `echo` mode and the dashboard simulator at `/simulator` works out of the box.

## Tests
```bash
cd backend && ./mvnw verify      # needs Docker: tests start their own PostgreSQL
cd frontend && npm run lint && npm run build
```
CI runs both on every push and pull request.

## Releases
- `main` changes only through pull requests from a branch.
- Every PR bumps the version (`backend/pom.xml` + `frontend/package.json`) and updates `CHANGELOG.md`.
- After merge: tag `vX.Y.Z` and publish a GitHub release. The version is shown in the dashboard sidebar and at `/actuator/info`.
