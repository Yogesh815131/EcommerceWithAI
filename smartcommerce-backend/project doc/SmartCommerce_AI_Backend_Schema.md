# Backend Schema Document
## SmartCommerce AI — Database Schema, Auth/Session Handling, Permissions & Data Ownership

**Document owner:** Engineering
**Companion to:** TRD (architecture/stack), App Flow Document (screen-to-endpoint mapping)
**Note:** Per the TRD, this is **database-per-service** — each service owns its schema and is the only writer to its own tables. Other services never query another service's database directly; they call its API, or consume its Kafka/RabbitMQ events. Schemas below are grouped by owning service.

---

## 1. Auth Service Database (`smartcommerce_auth`)

### 1.1 `users`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| full_name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NULL (null for OAuth-only accounts) |
| auth_provider | VARCHAR(50) | NULL (e.g. `GOOGLE`; null = local email/password) |
| enabled | BOOLEAN | NOT NULL, DEFAULT true |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Indexes:** UNIQUE index on `email` (also serves as the lookup index for login).

### 1.2 `user_roles`
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | PK (composite with `role`), FK → `users.id` ON DELETE CASCADE |
| role | VARCHAR(50) | PK (composite); one of `ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN` |

**Relationship:** `users` 1—N `user_roles` (a user can hold multiple roles, e.g. a vendor who also shops as a customer).

**Indexes:** Index on `user_id` (covered by composite PK).

### 1.3 `refresh_tokens` (Authentication/Session table)
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL, FK → `users.id` ON DELETE CASCADE |
| token_hash | VARCHAR(255) | NOT NULL, UNIQUE — store a hash of the refresh token, never the raw value |
| issued_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| expires_at | TIMESTAMP | NOT NULL |
| revoked | BOOLEAN | NOT NULL, DEFAULT false |
| revoked_at | TIMESTAMP | NULL |
| device_info | VARCHAR(255) | NULL — optional user-agent string for session visibility |

**Relationship:** `users` 1—N `refresh_tokens` (supports multiple concurrent sessions/devices).

**Indexes:** UNIQUE on `token_hash`; index on `(user_id, revoked)` for fast "list active sessions" and bulk-revoke-on-logout-all queries.

**Session handling model:**
- **Access token:** stateless JWT, short-lived (15–60 min), signed HS256, contains `sub` (email), `userId`, `roles[]`. Never persisted server-side — validity is purely signature + expiry, checked at the API Gateway and optionally re-checked per service.
- **Refresh token:** opaque random string, long-lived (e.g. 30 days), persisted here as a hash (bcrypt or SHA-256) so a database leak doesn't expose usable tokens. Client exchanges it at `/api/auth/refresh` for a new access token. Rotated on every use (old row marked `revoked`, new row inserted) to limit replay risk.
- **Logout:** marks the specific `refresh_tokens` row `revoked = true`. "Logout of all devices" sets `revoked = true` for all rows matching `user_id`.
- **Google OAuth2 users:** still receive the same JWT + refresh token pair after their Google identity is verified — from the rest of the system's perspective, an OAuth login and a password login are indistinguishable after this point.

---

## 2. User/Vendor Service Database (`smartcommerce_users`)

This service owns **profile/business data**; it does not own credentials — it references `user_id` (the Auth Service's `users.id`) as an opaque foreign key across service boundaries (not a DB-level FK, since it's a different database — enforced at the application layer).

### 2.1 `user_profiles`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL, UNIQUE — logical reference to Auth Service `users.id` |
| phone | VARCHAR(20) | NULL |
| avatar_url | VARCHAR(500) | NULL |
| default_shipping_address_id | BIGINT | NULL, FK → `addresses.id` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Indexes:** UNIQUE on `user_id`.

### 2.2 `addresses`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL — logical reference to Auth Service `users.id` |
| line1 | VARCHAR(255) | NOT NULL |
| line2 | VARCHAR(255) | NULL |
| city | VARCHAR(100) | NOT NULL |
| state | VARCHAR(100) | NOT NULL |
| postal_code | VARCHAR(20) | NOT NULL |
| country | VARCHAR(100) | NOT NULL |
| is_default | BOOLEAN | NOT NULL, DEFAULT false |

**Relationship:** `user_profiles` 1—N `addresses` (a user can have multiple saved addresses).

**Indexes:** Index on `user_id` (every address lookup is scoped to a user).

### 2.3 `vendors`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL, UNIQUE — logical reference to Auth Service `users.id`; this is the account that holds `ROLE_VENDOR` |
| business_name | VARCHAR(255) | NOT NULL |
| business_description | TEXT | NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT `ACTIVE` — one of `ACTIVE`, `DISABLED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Relationship:** one `user` (via `user_id`) owns exactly one `vendor` record. A `vendor.id` is what the Product Service's `products.vendor_id` references (logically, cross-service).

**Indexes:** UNIQUE on `user_id`; index on `status` (admin filtering).

---

## 3. Product Service Database (`smartcommerce_products`)

### 3.1 `categories`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR(100) | NOT NULL, UNIQUE |
| parent_category_id | BIGINT | NULL, FK → `categories.id` (self-referencing, for subcategories) |

**Relationship:** self-referencing 1—N (`categories` can nest one level via `parent_category_id`).

### 3.2 `products`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| vendor_id | BIGINT | NOT NULL — logical reference to User/Vendor Service `vendors.id` |
| category_id | BIGINT | NOT NULL, FK → `categories.id` |
| name | VARCHAR(255) | NOT NULL |
| description | TEXT | NULL |
| price | DECIMAL(10,2) | NOT NULL, CHECK (price > 0) |
| stock_quantity | INT | NOT NULL, DEFAULT 0, CHECK (stock_quantity >= 0) |
| status | VARCHAR(20) | NOT NULL, DEFAULT `ACTIVE` — one of `ACTIVE`, `INACTIVE` |
| image_url | VARCHAR(500) | NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Relationship:** `vendor` 1—N `products` (logical, cross-service). `category` 1—N `products`.

**Indexes:** Index on `vendor_id` (vendor's own product list queries); index on `category_id` (category browsing); index on `status` (filtering active-only in shopper-facing queries); composite index on `(category_id, status)` for the common "active products in category X" query.

### 3.3 `product_embeddings` (supports AI/RAG search — owned here, or could live in a dedicated AI Service DB; kept here since it's derived data 1:1 with a product)
| Column | Type | Constraints |
|---|---|---|
| product_id | BIGINT | PK, FK → `products.id` ON DELETE CASCADE |
| embedding | VECTOR(1536) | NOT NULL — PgVector type, dimension depends on embedding model used |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Indexes:** Vector similarity index (e.g., HNSW or IVFFlat via PgVector) on `embedding` — required for NL search performance at scale.

### 3.4 `reviews`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| product_id | BIGINT | NOT NULL, FK → `products.id` ON DELETE CASCADE |
| user_id | BIGINT | NOT NULL — logical reference to Auth Service `users.id` |
| order_id | BIGINT | NOT NULL — logical reference to Order Service `orders.id`, proves purchase |
| rating | SMALLINT | NOT NULL, CHECK (rating BETWEEN 1 AND 5) |
| comment | TEXT | NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Relationship:** `product` 1—N `reviews`. A given `(product_id, user_id, order_id)` should be UNIQUE — one review per purchase, not per product overall, so a repeat buyer can review each purchase.

**Indexes:** Index on `product_id` (product detail page review list); UNIQUE composite on `(product_id, user_id, order_id)`.

---

## 4. Cart Service Database (`smartcommerce_cart`) — Redis-backed, not relational

Per the TRD, cart data lives in Redis (cache-first, ephemeral by design), not PostgreSQL. Logical structure:

- **Key pattern:** `cart:{userId}` → hash of `{productId: quantity}`
- **TTL:** none while items exist (persists until checkout or explicit removal); a short TTL (e.g. 30 days of inactivity) is reasonable to avoid unbounded storage growth from abandoned carts.
- **Ownership:** a cart is always scoped to exactly one `userId` — there is no shared/multi-user cart in v1.

If a relational audit trail of cart activity is later required, a lightweight `cart_events` table (Postgres) could log add/remove/update events, but this is not required for MVP.

---

## 5. Order Service Database (`smartcommerce_orders`)

### 5.1 `orders`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL — logical reference to Auth Service `users.id` |
| status | VARCHAR(20) | NOT NULL, DEFAULT `PROCESSING` — one of `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |
| subtotal | DECIMAL(10,2) | NOT NULL |
| shipping_cost | DECIMAL(10,2) | NOT NULL, DEFAULT 0 |
| tax | DECIMAL(10,2) | NOT NULL, DEFAULT 0 |
| total | DECIMAL(10,2) | NOT NULL |
| shipping_address_id | BIGINT | NOT NULL — logical reference to User/Vendor Service `addresses.id` (snapshot, see note below) |
| shipping_address_snapshot | JSONB | NOT NULL — full address copied at order time, so a later address edit/delete doesn't corrupt historical orders |
| placed_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Indexes:** Index on `user_id` (order history queries, most common access pattern); index on `status` (admin/vendor filtering).

### 5.2 `order_items`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| order_id | BIGINT | NOT NULL, FK → `orders.id` ON DELETE CASCADE |
| product_id | BIGINT | NOT NULL — logical reference to Product Service `products.id` |
| vendor_id | BIGINT | NOT NULL — logical reference to `vendors.id`, denormalized here so Vendor Order Management can query "orders containing my products" without cross-service joins |
| product_name_snapshot | VARCHAR(255) | NOT NULL — copied at order time |
| unit_price_snapshot | DECIMAL(10,2) | NOT NULL — copied at order time, protects order history from later price changes |
| quantity | INT | NOT NULL, CHECK (quantity > 0) |
| item_status | VARCHAR(20) | NOT NULL, DEFAULT `PROCESSING` — allows per-vendor status when an order spans multiple vendors |

**Relationship:** `order` 1—N `order_items`.

**Indexes:** Index on `order_id` (composite PK access pattern); index on `vendor_id` (Vendor Order Management's core query: "line items belonging to my vendor account").

**Why snapshot fields:** Orders must remain historically accurate even if a vendor edits a product's name/price or a user edits their saved address after the fact — this is a standard e-commerce data-integrity pattern, not optional.

---

## 6. Payment Service Database (`smartcommerce_payments`)

### 6.1 `payments`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| order_id | BIGINT | NOT NULL, UNIQUE — logical reference to Order Service `orders.id` |
| user_id | BIGINT | NOT NULL — logical reference to Auth Service `users.id` |
| provider | VARCHAR(50) | NOT NULL — e.g. `STRIPE` |
| provider_charge_id | VARCHAR(255) | NOT NULL, UNIQUE — the payment provider's own transaction ID; this system never stores raw card data (per TRD security requirements) |
| amount | DECIMAL(10,2) | NOT NULL |
| status | VARCHAR(20) | NOT NULL — one of `PENDING`, `SUCCEEDED`, `FAILED`, `REFUNDED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |

**Indexes:** UNIQUE on `order_id` (one payment per order in v1 — no split payments); UNIQUE on `provider_charge_id`.

**Note:** No card numbers, CVVs, or other PANs are ever stored in this or any SmartCommerce database — that data lives entirely with the payment provider (Stripe or equivalent), referenced only by `provider_charge_id`.

---

## 7. Notification Service Database (`smartcommerce_notifications`)

### 7.1 `notifications`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | NOT NULL — logical reference to Auth Service `users.id` |
| type | VARCHAR(50) | NOT NULL — e.g. `ORDER_PLACED`, `ORDER_SHIPPED`, `PAYMENT_FAILED` |
| channel | VARCHAR(20) | NOT NULL — e.g. `EMAIL` |
| payload | JSONB | NOT NULL — rendered content/template data |
| status | VARCHAR(20) | NOT NULL, DEFAULT `PENDING` — one of `PENDING`, `SENT`, `FAILED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() |
| sent_at | TIMESTAMP | NULL |

This table is populated by consuming events off Kafka/RabbitMQ (e.g., `order.placed`, `payment.failed` published by Order/Payment Services) rather than by direct API calls — decoupling notification delivery from the transactional flow that triggered it.

**Indexes:** Index on `(user_id, created_at)` for a potential future "notification history" screen; index on `status` for retry/worker queries.

---

## 8. Permissions Model

Enforced via JWT role claims (`ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`), checked at **two layers**: API Gateway (coarse routing-level checks) and each service's own controllers (fine-grained, authoritative — never trust the gateway check alone, since services must be safe even if called directly in a future internal-network scenario).

| Resource / Action | Customer | Vendor | Admin |
|---|---|---|---|
| Browse/search products | ✅ | ✅ | ✅ |
| Manage own cart | ✅ (own only) | ✅ (own only) | ✅ (own only) |
| Place order | ✅ | ✅ | ✅ |
| View own order history | ✅ (own only) | ✅ (own only) | ✅ (own only) |
| Cancel own order (pre-shipment) | ✅ (own only) | ✅ (own only) | ✅ (own only) |
| Write review (on own purchase) | ✅ (own only) | ✅ (own only) | ✅ (own only) |
| Create/edit/delete own products | ❌ | ✅ (own vendor's products only) | ✅ (any) |
| View/update orders containing own products | ❌ | ✅ (own vendor's line items only) | ✅ (any) |
| View/disable any user account | ❌ | ❌ | ✅ |
| View/disable any vendor account | ❌ | ❌ | ✅ |
| View platform-wide metrics | ❌ | ❌ (own metrics only, via Vendor Dashboard) | ✅ |

## 9. Data Ownership Rules

1. **A row is owned by the `user_id` (or `vendor_id`) it references**, and only that owner — or an Admin — may read or mutate it. This applies to: addresses, cart contents, orders, reviews, and vendor product listings.
2. **Vendors own products, not orders.** A vendor can update the `item_status` of `order_items` that belong to them (via `order_items.vendor_id`), but cannot see or modify other vendors' line items within the same multi-vendor order, and cannot cancel the order outright (only the customer or an Admin can).
3. **Cross-service references are logical, not DB-level foreign keys.** Because each service owns its own database (Section-by-section above), referential integrity across services (e.g., `products.vendor_id` → `vendors.id`) is enforced at the application layer, not via SQL `FOREIGN KEY` constraints — a service must call the owning service's API (or trust an event payload) to validate a reference, since a cross-database FK isn't possible.
4. **Historical/financial records are immutable snapshots.** `order_items` and `orders.shipping_address_snapshot` copy data at time of purchase rather than referencing live rows — a product price change or address edit must never alter a past order's recorded values.
5. **No service stores another service's authoritative data.** E.g., the Order Service denormalizes `vendor_id` and `product_name_snapshot` into `order_items` for query performance, but the Product Service remains the source of truth for current product data — the Order Service's copy is explicitly a point-in-time snapshot, never treated as current.
6. **Admins bypass ownership checks but every bypass is logged.** (Recommend an `admin_audit_log` table — service TBD, likely a shared lightweight logging table or centralized log aggregation — recording `admin_user_id`, `action`, `target_resource`, `timestamp` for any admin action that mutates another user's/vendor's data.)

---

**Next logical step:** API contract document (request/response JSON shapes per endpoint) so the schema above maps directly to what each service exposes.
