# Implementation Plan
## SmartCommerce AI — Phased Build Plan with Deliverables

**Document owner:** Engineering / PM
**Companion to:** PRD, TRD, App Flow Document, Design Brief, Backend Schema
**Status reference:** Eureka Server, API Gateway, and Auth Service skeletons are already scaffolded (Phase 1 and part of Phase 2 below are partially complete).

---

## Phase 0: Setup

**Goal:** Working local dev environment and repo structure before any feature code.

**Tasks:**
- Multi-module Maven repo structure (already done: `eureka-server`, `api-gateway`, `auth-service`)
- `docker-compose.yml` for local dev: PostgreSQL, Redis, Kafka/RabbitMQ, Eureka, all services
- GitHub repo with branch protection on `main`, PR template
- GitHub Actions skeleton: build + test on every PR (deploy step added later in Phase 8)
- Shared coding conventions doc (package structure, DTO naming, error response shape) so every subsequently-built service is consistent

**Deliverables:**
- Repo that clones and runs (`docker-compose up`) with Eureka + Gateway + Auth Service reachable
- CI pipeline green on an empty/skeleton commit

---

## Phase 1: Authentication

**Goal:** End-to-end login works — this unblocks every other service, since nothing else can be tested without a real JWT.

**Tasks:**
- Auth Service: register, login, JWT issuance (**already built**)
- `refresh_tokens` table + `/api/auth/refresh` endpoint (per Backend Schema Section 1.3 — not yet built)
- Google OAuth2 login flow (client registration scaffolded; redirect/success handler still TODO per current code)
- API Gateway: `JwtAuthenticationFilter` to actually validate tokens at the edge (currently permits all — flagged TODO in existing code)
- Logout / logout-all-devices (revoke refresh token rows)

**Deliverables:**
- Postman/Swagger-verified: register → login → access protected route → refresh → logout, full cycle working
- Google login working end-to-end in at least one environment
- Gateway rejects requests with missing/invalid/expired tokens (401)

---

## Phase 2: Database

**Goal:** All service databases exist and match the Backend Schema document exactly, before feature endpoints are built on top of them.

**Tasks:**
- Auth Service: `users`, `user_roles`, `refresh_tokens` (users/user_roles done; refresh_tokens added here)
- User/Vendor Service: `user_profiles`, `addresses`, `vendors` — new service, new database
- Product Service: `categories`, `products`, `product_embeddings` (PgVector extension enabled), `reviews`
- Order Service: `orders`, `order_items`
- Payment Service: `payments`
- Notification Service: `notifications`
- Replace `ddl-auto: update` with Flyway migrations in every service (flagged as a pre-production TODO in the backend skeleton — do this now rather than deferring, since retrofitting migrations onto live data later is painful)

**Deliverables:**
- One Flyway migration set per service, checked into each service's `src/main/resources/db/migration`
- All tables/indexes from the Backend Schema document exist and are verifiable via `\d table_name` in psql
- Redis connection verified for cart (key pattern from schema doc working)

---

## Phase 3: Core UI

**Goal:** Frontend shell and design system exist before feature screens are wired to real data — avoids rebuilding layout/components repeatedly per feature.

**Tasks:**
- React app scaffold (Vite), Tailwind configured with the design tokens from the Design Brief (colors, type scale, spacing scale as theme extension)
- Shared component library: Button, Input, Card, Modal, Toast, Badge, Table — built once per the Design Brief's component style, reused everywhere
- App shell: nav bar (logged-in/out states), footer, routing skeleton matching the App Flow document's navigation map
- Dashboard shell (sidebar + top bar) shared between Vendor and Admin per Design Brief Section 6
- Auth screens (Login, Register) wired to Phase 1's real endpoints

**Deliverables:**
- Component library viewable in isolation (Storybook optional but recommended)
- Working login/register flow in the browser against the real Auth Service
- Empty-state versions of Home, Cart, Order History render correctly (no data yet, but layout is real)

---

## Phase 4: Main Features

**Goal:** The core commerce loop works end-to-end — browse → cart → checkout → order — before layering AI on top.

**Sub-phases, in build order (matches TRD's recommended service order):**

**4a. User/Vendor Service**
- Vendor onboarding (business profile creation)
- Address management
- Deliverable: a `ROLE_VENDOR` user can create a business profile; any user can save an address

**4b. Product Service**
- Category CRUD (admin-seeded initially)
- Product CRUD, scoped to the authenticated vendor
- Product listing/detail read endpoints (public)
- Deliverable: Vendor Dashboard → Add/Edit Product screens fully functional; Product Detail page renders real data

**4c. Cart Service**
- Add/update/remove cart items (Redis-backed)
- Deliverable: Cart screen fully functional against real backend

**4d. Order + Payment Service**
- Order creation from cart contents (with stock re-validation, snapshot fields per schema)
- Payment provider integration (Stripe or equivalent), tokenized — no card data touches our DB
- Order status transitions (Processing → Shipped → Delivered, Cancel)
- Deliverable: full Checkout → Order Confirmation → Order History flow works with real payments (test mode)

**4e. Reviews**
- Review creation (purchase-verified via `order_id`), review listing on Product Detail
- Deliverable: "Write a Review" flow functional for verified purchasers

---

## Phase 5: Integrations

**Goal:** Cross-cutting integrations that connect the services built in Phase 4.

**Tasks:**
- Kafka/RabbitMQ event wiring: `order.placed` → Notification Service, `payment.failed` → Notification Service, `order.placed` → inventory decrement in Product Service
- Notification Service: email sending (order confirmation, shipment update) via a transactional email provider
- AI/RAG Service (first AI feature only, per PRD MVP scope): embedding generation on product create/update → `product_embeddings`, `/api/ai/search` endpoint, Spring AI + PgVector wiring
- Frontend: wire Search Results page to real `/api/ai/search`, with the keyword-search fallback behavior specified in the App Flow document

**Deliverables:**
- Placing an order triggers a real confirmation email
- Natural-language search returns relevant results end-to-end in the browser
- Fallback to keyword search verified by manually killing the AI service and confirming the UI degrades gracefully rather than breaking

---

## Phase 6: Remaining Screens

**Goal:** Fill in the screens from the App Flow document not yet covered — vendor order management, admin panel.

**Tasks:**
- Vendor Order Management screen (update `order_items.item_status`)
- Admin Dashboard, User Management, Vendor Management (enable/disable accounts)
- Wire Admin actions to an `admin_audit_log` per Backend Schema Section 9's recommendation

**Deliverables:**
- Every screen in the App Flow document's navigation map is implemented and reachable
- Admin can disable a user/vendor and the action is logged

---

## Phase 7: Testing

**Goal:** Confidence the system works correctly and won't regress.

**Tasks:**
- Unit tests per service (business logic — pricing calculations, stock checks, JWT validation)
- Integration tests per service (repository layer against a real test DB — Testcontainers recommended)
- Contract/end-to-end tests for the critical path: register → browse → search → cart → checkout → order confirmation
- Manual QA pass against every success/error/empty state listed in the App Flow document — this document should function as the QA checklist
- Load/basic performance check on the AI search endpoint specifically, since it's the most novel/riskiest path

**Deliverables:**
- CI runs full test suite on every PR, blocking merge on failure
- A completed QA checklist (derived directly from App Flow doc states) with no unchecked critical-path items
- Known-issues list for anything deferred, with severity noted

---

## Phase 8: Deployment

**Goal:** Running in a real environment, not just locally.

**Tasks:**
- Dockerize each service (if not already minimal per-service Dockerfiles)
- Stand up staging environment on AWS (ECS per TRD decision), with GitHub Actions deploying on merge to a `staging` branch
- Environment-specific config via Spring profiles + secrets manager (never hardcoded)
- Production environment stood up after staging sign-off; production deploy gated behind manual approval in GitHub Actions
- Centralized logging + basic tracing (Micrometer Tracing) wired per TRD's observability requirement

**Deliverables:**
- Staging environment publicly reachable, full flow testable end-to-end by a non-engineer (e.g., you demoing it)
- Production deploy pipeline exists and has been exercised at least once
- Logs from a real request are traceable across at least 2 services (proves observability wiring works)

---

## Phase 9: Final Polish

**Goal:** Portfolio/production-ready finish.

**Tasks:**
- Responsive QA pass against the Design Brief's mobile breakpoints on real devices/emulators
- Accessibility pass (contrast, focus states, semantic headings — per Design Brief Section 8)
- Copy pass on all error/empty states for tone consistency
- Performance pass: image optimization, API response time spot-checks, bundle size check on the frontend
- README + architecture diagram finalized for portfolio presentation
- Demo script / walkthrough prepared (register → search → buy → vendor manages order → admin moderates) — useful both for portfolio demos and as a living smoke test

**Deliverables:**
- Polished, demoable product across desktop and mobile
- Final README with architecture diagram, setup instructions, and a link to the demo script
- Portfolio-ready write-up summarizing the technical decisions made (this can draw directly from the TRD's "Key Technical Decisions" table)

---

## Summary Timeline View

| Phase | Focus | Depends on |
|---|---|---|
| 0. Setup | Repo, CI, local env | — |
| 1. Authentication | Login works end-to-end | Phase 0 |
| 2. Database | All schemas match Backend Schema doc | Phase 0 |
| 3. Core UI | Design system + app shell | Phase 1 (for auth screens) |
| 4. Main Features | Browse → cart → checkout → order | Phases 1–3 |
| 5. Integrations | Events, notifications, AI search | Phase 4 |
| 6. Remaining Screens | Vendor/Admin management UIs | Phase 4 |
| 7. Testing | Confidence before deploy | Phases 4–6 |
| 8. Deployment | Staging → production | Phase 7 |
| 9. Final Polish | Demo-ready finish | Phase 8 |

This phase order intentionally matches the dependency chain already established across your PRD (MVP scope), TRD (build order: Auth → User/Vendor → Product → Cart/Order/Payment → AI), and Backend Schema (service-by-service breakdown) — nothing here introduces a new sequencing decision, it just turns those into executable steps with checkpoints.
