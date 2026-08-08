# App Flow Document
## SmartCommerce AI — Complete Screen & Interaction Specification (v1 / MVP scope)

**Document owner:** UX
**Purpose:** Exhaustive screen-by-screen specification so an AI coding agent (or engineer) can build the frontend without needing to infer behavior.
**Scope:** MVP only — Auth, Catalog + NL Search, Cart, Checkout, Orders, Vendor Dashboard, Admin Panel. (Per PRD, the other six AI features are post-MVP and not included here.)

---

## 0. Conventions Used Throughout

- **Success state**: what the user sees/where they land when the action completes as expected.
- **Error state**: what the user sees when the action fails (validation error, server error, network error) — each screen lists the specific errors relevant to it.
- **Empty state**: what's shown when a screen/list has no data yet.
- **Loading state**: applies to every async action below by default — show a spinner/skeleton in place of the content being fetched; disable the triggering button while in flight. Not repeated per-screen unless it has unique loading behavior.
- Every "Error state" below assumes a generic fallback also exists: **"Something went wrong. Please try again."** with a Retry button, shown when an error doesn't match a specific case listed.

---

## 1. Global Elements (present across all screens unless noted)

### 1.1 Top Navigation Bar
**Elements:** Logo (links to Home) · Search bar · Cart icon with item count badge · Account menu (avatar or "Sign In" if logged out) · [Vendor/Admin only] "Dashboard" link

**Button behaviors:**
- Logo → navigates to Home (`/`)
- Search bar → on submit (Enter or search icon click), navigates to Search Results (`/search?q=...`)
- Cart icon → navigates to Cart page (`/cart`)
- Account menu (logged out) → click opens dropdown: "Sign In" / "Register"
- Account menu (logged in) → click opens dropdown: "My Orders" / "Profile" / "Sign Out"
  - "Sign Out" → clears JWT from client storage, redirects to Home, nav bar reverts to logged-out state
- "Dashboard" link (vendor/admin only, shown based on role claim in JWT) → navigates to `/vendor/dashboard` or `/admin/dashboard` respectively

### 1.2 Footer
Static links (About, Contact, Terms, Privacy) — no dynamic behavior in v1.

### 1.3 Global Error Handling
- **401 Unauthorized on any API call** → clear local JWT, redirect to Sign In (`/login`) with a banner: "Your session has expired. Please sign in again."
- **Network failure (no connectivity)** → toast notification: "You appear to be offline. Check your connection and try again." Do not navigate away from current screen.
- **500-level server error** → toast: "Something went wrong on our end. Please try again in a moment."

---

## 2. Authentication Flows

### 2.1 Sign In (`/login`)
**Elements:** Email field · Password field · "Sign In" button · "Sign in with Google" button · "Don't have an account? Register" link · "Forgot password?" link (post-MVP, render disabled/hidden in v1 since no reset flow is built)

**User actions:**
- Enter email + password → click "Sign In" → `POST /api/auth/login`
- Click "Sign in with Google" → redirects to Google OAuth consent screen → on success, backend issues JWT, redirects to Home
- Click "Register" link → navigates to `/register`

**Success state:** JWT stored client-side (memory + secure storage, not localStorage for XSS safety — use httpOnly cookie if backend supports it, otherwise in-memory + refresh flow). Redirect to the page the user was on before being prompted to log in, or Home if none.

**Error states:**
- Invalid email/password → inline error under form: "Invalid email or password." Fields remain filled except password, which clears.
- Empty required field on submit → inline validation: "Email is required" / "Password is required" — shown on blur and on submit attempt, submit button disabled until both fields non-empty.
- Account exists but was created via Google only (no password set) → "This email is registered via Google. Please use 'Sign in with Google'."

**Empty state:** N/A (form screen, no list data).

### 2.2 Register (`/register`)
**Elements:** Full name field · Email field · Password field · Confirm password field · "Create Account" button · "Sign up with Google" button · "Already have an account? Sign In" link

**User actions:**
- Fill form → click "Create Account" → `POST /api/auth/register`
- Click "Sign In" link → navigates to `/login`

**Success state:** Account created, JWT returned and stored, redirect to Home with a one-time toast: "Welcome, {fullName}!"

**Error states:**
- Email already registered → inline error under email field: "An account with this email already exists." + link "Sign in instead" → navigates to `/login`
- Password < 8 characters → inline error under password field: "Password must be at least 8 characters."
- Passwords don't match → inline error under confirm-password field: "Passwords do not match."
- Any required field empty → inline validation per field, submit disabled until valid.

**Empty state:** N/A.

---

## 3. Home Screen (`/`)

**Purpose:** Entry point — surfaces categories and featured/popular products; primary path into search.

**Elements:** Hero search bar (larger version of nav search, with placeholder text "Try: waterproof hiking boots under $100") · Category tiles (grid) · "Popular Right Now" product grid (static/basic ranking in v1 — no personalization, since Recommendation Engine is post-MVP)

**User actions:**
- Type query in hero search bar → submit → navigates to `/search?q=...`
- Click a category tile → navigates to `/category/{categoryId}`
- Click a product card → navigates to `/product/{productId}`

**Success state:** Content loads and renders normally.

**Error state:**
- Failed to load featured products → section shows: "We couldn't load featured products right now." with a Retry button scoped to that section only (rest of page unaffected).

**Empty state:**
- No products in catalog yet (fresh install) → "Products are coming soon — check back shortly." shown in place of the product grid; category tiles still render if categories exist, otherwise hidden entirely.

---

## 4. Search Results (`/search?q={query}`)

**Purpose:** Core AI feature surface — natural-language product search.

**Elements:** Search bar (pre-filled with current query, editable) · Result count text ("42 results for 'waterproof hiking boots under $100'") · Filter sidebar (category, price range — basic filters only in v1) · Product grid · Pagination controls

**User actions:**
- Edit query and re-submit → re-fetches `GET /api/ai/search?q=...`, updates URL and results
- Apply a filter → re-fetches with filter params appended, updates result count
- Click a product card → navigates to `/product/{productId}`
- Click pagination control → fetches next/previous page, scrolls to top of results

**Success state:** Results render as a grid of product cards (image, name, price, vendor name, star rating placeholder).

**Error states:**
- AI search backend unavailable/times out → fallback message: "Smart search is temporarily unavailable — showing basic results instead." and system falls back to a plain keyword search against the Product Service (graceful degradation, not a dead end).
- Malformed/empty query submitted → prevent submission client-side; search bar shows subtle shake/border highlight, no navigation occurs.

**Empty state:**
- Query returns zero results → "No products matched 'waterproof hiking boots under $100'. Try a different search or browse categories." with a "Browse Categories" button → navigates to Home.

---

## 5. Category Listing (`/category/{categoryId}`)

**Elements:** Category name as page title · Filter sidebar (price, vendor) · Sort dropdown (Price: Low–High, Price: High–Low, Newest) · Product grid · Pagination

**User actions:**
- Change sort → re-fetches with sort param, grid re-renders
- Apply filter → same pattern as Search Results
- Click product card → navigates to `/product/{productId}`

**Success/Error/Empty states:** Same pattern as Search Results (Section 4), with empty state copy: "No products in this category yet."

---

## 6. Product Detail (`/product/{productId}`)

**Elements:** Image gallery · Product name, price, vendor name (link) · Stock status ("In Stock" / "Only 3 left" / "Out of Stock") · Quantity selector · "Add to Cart" button · "Buy Now" button · Description (vendor-authored, or AI-generated tag if applicable post-MVP — not in v1) · Reviews section (list of existing reviews; review *summarizer* is post-MVP, so this just lists raw reviews) · "Write a Review" button (visible only if user has purchased this product and is logged in)

**User actions:**
- Change quantity → updates local state only, no API call until Add to Cart
- Click "Add to Cart" → `POST /api/cart/items` → item added
- Click "Buy Now" → adds to cart, then immediately navigates to `/checkout`
- Click vendor name → navigates to `/vendor/{vendorId}` (public vendor storefront page — basic listing of that vendor's products; not detailed further here, out of primary v1 focus but included as a simple filtered product grid reusing Section 5's pattern)
- Click "Write a Review" → opens review modal (rating stars + text field) → `POST /api/products/{productId}/reviews`

**Success states:**
- Add to Cart → toast: "Added to cart" with a "View Cart" action in the toast; cart icon badge count increments without full page reload.
- Buy Now → navigates directly to Checkout with this item pre-loaded.
- Review submitted → modal closes, toast: "Review submitted", review appears in list immediately (optimistic update) or after refetch.

**Error states:**
- Add to Cart when out of stock → button is disabled and labeled "Out of Stock" instead of "Add to Cart"; no API call possible.
- Add to Cart fails server-side (e.g., race condition, stock ran out between page load and click) → toast: "Sorry, this item just sold out." Button updates to disabled/Out of Stock state.
- Review submission fails validation (empty text or no rating selected) → inline error in modal: "Please select a rating and write a comment."
- Product not found (bad/stale productId) → full-page state: "This product is no longer available." with "Browse Products" button → Home.

**Empty state:**
- No reviews yet → "No reviews yet. Be the first to review this product." (Write a Review button still shown if eligible).

---

## 7. Cart (`/cart`)

**Elements:** Line items (image, name, price, quantity stepper, remove button, per-item subtotal) · Order subtotal · "Proceed to Checkout" button · "Continue Shopping" link

**User actions:**
- Change quantity stepper → `PATCH /api/cart/items/{itemId}` → updates subtotal
- Click remove (trash icon) → `DELETE /api/cart/items/{itemId}` → item removed from list, subtotal recalculates
- Click "Proceed to Checkout" → navigates to `/checkout`
- Click "Continue Shopping" → navigates to Home

**Success state:** Cart reflects current state after each action; totals update without full page reload.

**Error states:**
- Quantity update exceeds available stock → inline error under that line item: "Only {n} left in stock." Quantity reverts to max available.
- Remove/update fails (network) → toast: "Couldn't update cart. Please try again." Item state reverts to last known good value.

**Empty state:**
- Cart has no items → "Your cart is empty." illustration + "Browse Products" button → Home. "Proceed to Checkout" button is hidden entirely (not just disabled).

---

## 8. Checkout (`/checkout`)

**Elements:** Shipping address form (or saved address selector if returning user) · Order summary (line items, subtotal, shipping, tax, total) · Payment method form (card fields via payment provider's embedded component) · "Place Order" button

**User actions:**
- Fill/select shipping address
- Enter payment details (handled by embedded payment provider component, not custom fields, per TRD security requirement — raw card data never touches our own backend)
- Click "Place Order" → `POST /api/orders` → on success, payment provider charge is processed

**Success state:** Redirect to Order Confirmation (`/orders/{orderId}/confirmation`) showing order number, estimated delivery, and a summary. Cart is cleared.

**Error states:**
- Payment declined → inline error near payment section: "Your payment was declined. Please check your details or try another payment method." Order is NOT created; user remains on Checkout with cart intact.
- Shipping address incomplete → inline field-level validation, "Place Order" disabled until required fields filled.
- Item went out of stock during checkout → error banner at top: "{Product name} is no longer available and has been removed from your order." Item removed from summary, totals recalculate, user must review and re-submit.
- Network/server failure during order placement → toast: "We couldn't place your order. You haven't been charged — please try again." (Explicit reassurance since this is a payment flow.)

**Empty state:** N/A (checkout is unreachable with an empty cart — "Proceed to Checkout" is hidden on an empty Cart page per Section 7).

---

## 9. Order Confirmation (`/orders/{orderId}/confirmation`)

**Elements:** Success icon · Order number · Estimated delivery · Order summary · "View Order Details" button · "Continue Shopping" button

**User actions:**
- Click "View Order Details" → navigates to `/orders/{orderId}`
- Click "Continue Shopping" → navigates to Home

**Success/Error/Empty states:** This screen only renders after a successful order placement (Section 8's success path) — no independent error/empty states of its own. Direct navigation to this URL without a valid completed order redirects to `/orders` (Order History).

---

## 10. Order History (`/orders`)

**Elements:** List of past orders (order number, date, status badge, total, "View Details" link) · Status filter dropdown (All, Processing, Shipped, Delivered, Cancelled)

**User actions:**
- Click a status filter → re-fetches filtered list
- Click "View Details" → navigates to `/orders/{orderId}`

**Success state:** List renders, most recent order first.

**Error state:** Failed to load → "We couldn't load your orders." with Retry button.

**Empty state:** No orders yet → "You haven't placed any orders yet." + "Start Shopping" button → Home.

### 10.1 Order Detail (`/orders/{orderId}`)
**Elements:** Order status timeline (Placed → Processing → Shipped → Delivered) · Line items · Shipping address · Payment summary · "Cancel Order" button (only visible if status is "Processing," not yet shipped)

**User actions:**
- Click "Cancel Order" → confirmation dialog ("Are you sure you want to cancel this order?" Yes/No) → on Yes, `PATCH /api/orders/{orderId}/cancel`

**Success state:** Status updates to "Cancelled" in place, "Cancel Order" button disappears.

**Error states:**
- Cancel attempted after order already shipped (race condition) → dialog error: "This order has already shipped and can no longer be cancelled." Status refreshes to actual current state.
- Order not found / doesn't belong to current user → full-page: "Order not found." with link back to `/orders`.

**Empty state:** N/A (single-order view).

---

## 11. Vendor Dashboard (role: Vendor only, `/vendor/dashboard`)

Access control: any authenticated user without `ROLE_VENDOR` who navigates here directly is redirected to Home.

### 11.1 Dashboard Home (`/vendor/dashboard`)
**Elements:** Summary cards (Total Products, Pending Orders, Total Sales this month) · "Add Product" button · Recent orders table (last 10)

**User actions:**
- Click "Add Product" → navigates to `/vendor/products/new`
- Click a recent order row → navigates to `/vendor/orders/{orderId}`

**Success state:** Cards and table populate with real data.

**Error state:** Any card/table fails to load independently → that section shows "Unable to load" with its own Retry, rest of dashboard unaffected.

**Empty state:** New vendor with no products/orders yet → summary cards show 0s; recent orders table shows "No orders yet." Dashboard still fully navigable (Add Product remains primary CTA).

### 11.2 Product Management List (`/vendor/products`)
**Elements:** Table of vendor's own products (image, name, price, stock, status toggle Active/Inactive, Edit link, Delete button) · "Add Product" button · Search/filter within own catalog

**User actions:**
- Click "Add Product" → `/vendor/products/new`
- Click Edit → `/vendor/products/{productId}/edit`
- Toggle Active/Inactive → `PATCH /api/products/{productId}` (status field) → immediate reflect, no confirmation needed
- Click Delete → confirmation dialog ("Delete {product name}? This cannot be undone.") → on confirm, `DELETE /api/products/{productId}`

**Success state:** Table updates in place after each action (no full reload).

**Error states:**
- Delete fails because product has existing order history → dialog error: "This product can't be deleted because it has past orders. Set it to Inactive instead."
- Save/update fails validation → handled at the form level (Section 11.3).

**Empty state:** "You haven't added any products yet." + "Add Product" button (primary, centered).

### 11.3 Add/Edit Product (`/vendor/products/new` and `/vendor/products/{productId}/edit`)
**Elements:** Name field · Description field (plain textarea in v1 — AI description generator is post-MVP) · Price field · Category dropdown · Stock quantity field · Image upload · "Save" button · "Cancel" link

**User actions:**
- Fill form → click "Save" → `POST /api/products` (new) or `PUT /api/products/{productId}` (edit)
- Click "Cancel" → navigates back to `/vendor/products` without saving, no confirmation needed if no changes made; if changes were made, show a confirm dialog: "Discard unsaved changes?"

**Success state:** Toast "Product saved" → navigates back to `/vendor/products`, new/updated row visible.

**Error states:**
- Required field missing → inline validation per field, Save disabled until valid.
- Price ≤ 0 → inline error: "Price must be greater than 0."
- Image upload fails (size/format) → inline error under upload field: "Image must be under 5MB and in JPG/PNG format."
- Save fails server-side → toast: "Couldn't save product. Please try again." Form retains all entered data (nothing is lost).

**Empty state:** N/A (form).

### 11.4 Vendor Order Management (`/vendor/orders`)
**Elements:** Table of orders containing this vendor's products (order number, customer name, date, status, "Update Status" action) · Status filter

**User actions:**
- Click "Update Status" on a row → dropdown to change status (Processing → Shipped) → `PATCH /api/orders/{orderId}/status`

**Success state:** Row updates in place, toast: "Order status updated."

**Error state:** Status update fails (e.g., invalid transition like Shipped → Processing) → inline error: "Orders can't be moved back to Processing once shipped."

**Empty state:** "No orders yet." (same pattern as 11.1).

---

## 12. Admin Panel (role: Admin only, `/admin/dashboard`)

Access control: same pattern as Vendor Dashboard — non-admins redirected to Home.

### 12.1 Admin Dashboard (`/admin/dashboard`)
**Elements:** Platform summary cards (Total Users, Total Vendors, Total Orders, Total GMV) · Quick links to User Management and Vendor Management

**Success/Error/Empty states:** Same pattern as Vendor Dashboard Home (Section 11.1).

### 12.2 User Management (`/admin/users`)
**Elements:** Table (name, email, role, status Active/Disabled, "Disable"/"Enable" action) · Search by email · Role filter

**User actions:**
- Click "Disable" → confirmation dialog ("Disable this account? They will not be able to sign in.") → `PATCH /api/users/{userId}/status`
- Click "Enable" → same pattern, no confirmation needed (re-enabling is lower-risk)

**Success state:** Row status updates in place, toast confirms action.

**Error state:** Action fails → toast: "Couldn't update user status. Please try again."

**Empty state:** Search returns no matches → "No users found matching '{query}'."

### 12.3 Vendor Management (`/admin/vendors`)
Same structural pattern as 12.2, scoped to vendor accounts, plus a "View Products" link per row → navigates to a read-only version of that vendor's product list.

---

## 13. Cross-Cutting Navigation Map

```
/ (Home)
├── /search?q=... (Search Results)
├── /category/{id} (Category Listing)
├── /product/{id} (Product Detail)
│   └── /vendor/{vendorId} (Public vendor storefront)
├── /cart (Cart)
│   └── /checkout (Checkout)
│       └── /orders/{id}/confirmation (Order Confirmation)
├── /orders (Order History)
│   └── /orders/{id} (Order Detail)
├── /login (Sign In)
├── /register (Register)
├── /vendor/dashboard (Vendor Dashboard) [ROLE_VENDOR]
│   ├── /vendor/products (Product Management)
│   │   └── /vendor/products/new | /vendor/products/{id}/edit
│   └── /vendor/orders (Vendor Order Management)
└── /admin/dashboard (Admin Dashboard) [ROLE_ADMIN]
    ├── /admin/users (User Management)
    └── /admin/vendors (Vendor Management)
```

## 14. Explicitly Out of Scope for This Flow Document (per PRD v1 exclusions)

Do not build screens/flows for: AI shopping assistant UI, review summarizer UI, personalized recommendation carousels, vendor AI description/email generator tools, multi-language/currency switchers, real-time buyer-vendor chat, native mobile screens. These are post-MVP per the PRD and intentionally excluded here to avoid scope creep in the build.
