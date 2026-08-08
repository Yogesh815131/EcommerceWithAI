# UI/UX Design Brief
## SmartCommerce AI — Enterprise Multi-Vendor E-Commerce Platform

**Document owner:** Design
**Purpose:** Concrete, implementable visual and interaction direction — specific enough that an AI app builder or frontend engineer can apply it directly without further interpretation.
**Companion to:** PRD, TRD, App Flow Document

---

## 1. Design Style

**Direction: Modern Marketplace — clean, trustworthy, product-forward.**

Not a playful/startup-y aesthetic and not a heavy enterprise-dashboard look. The commerce screens (browsing, product, checkout) should feel like a premium consumer marketplace — think the restraint of a well-run storefront, not a generic Bootstrap template. The vendor/admin dashboards can be slightly more utilitarian and data-dense, since their job is task efficiency, not browsing delight — but they should still share the same design language (same colors, type, components), just denser.

Guiding principles:
- **Product imagery is the hero.** Chrome around product cards and detail pages stays minimal so photos do the selling.
- **Trust signals are visually prominent** — stock status, order status, payment security cues — because this is a marketplace with multiple vendors and real money changing hands; users need to feel confident at every step.
- **Generous whitespace over density** on shopper-facing screens; tighter, table-driven density on vendor/admin screens.
- Avoid: skeuomorphism, heavy gradients/glassmorphism, playful illustration-heavy empty states (a simple line icon + text is enough — this is a commerce tool, not a consumer social app).

## 2. Color Palette

| Role | Color | Hex | Usage |
|---|---|---|---|
| Primary | Deep Indigo | `#3730A3` | Primary buttons, active nav states, links, focus rings |
| Primary Hover | Indigo Dark | `#312E81` | Hover/pressed state for primary buttons |
| Secondary / Accent | Warm Amber | `#D97706` | "Add to Cart," price highlights, secondary CTAs, badges (e.g. "Only 3 left") |
| Success | Emerald | `#059669` | Order confirmed, in-stock, success toasts |
| Error / Destructive | Red | `#DC2626` | Errors, out-of-stock, delete actions |
| Warning | Amber (muted) | `#D97706` at 15% background / full for text | Low-stock warnings, pending status |
| Neutral 900 (text primary) | `#111827` | Body text, headings |
| Neutral 600 (text secondary) | `#4B5563` | Secondary text, captions, metadata |
| Neutral 300 (borders) | `#D1D5DB` | Input borders, dividers |
| Neutral 100 (surface) | `#F3F4F6` | Page background, card backgrounds on hover |
| Neutral 0 (surface) | `#FFFFFF` | Card/content backgrounds |

**Rationale:** Indigo reads as trustworthy/professional without being cold (avoids generic SaaS blue), and pairs cleanly with warm amber as the "money/action" accent color for cart and pricing — creating clear visual hierarchy between "navigate" actions (indigo) and "transact" actions (amber). All pairings above meet WCAG AA contrast on white/near-white backgrounds.

**Status badge colors** (order status, product status) reuse Success/Error/Warning/Neutral above — never introduce new colors for status, so users learn the system once.

## 3. Typography

| Role | Font | Weight | Size (desktop) |
|---|---|---|---|
| Font family | **Inter** (or system-ui fallback stack) | — | — |
| H1 (page titles) | Inter | 700 | 32px / 40px line-height |
| H2 (section headers) | Inter | 600 | 24px / 32px |
| H3 (card/subsection titles) | Inter | 600 | 18px / 28px |
| Body | Inter | 400 | 16px / 24px |
| Small / caption | Inter | 400 | 14px / 20px |
| Price display | Inter | 700 | 20px (product cards), 28px (product detail) |
| Buttons | Inter | 600 | 16px |

**Rationale:** Inter is chosen for its high legibility at small sizes (critical for dense vendor/admin tables and product metadata) and neutral, professional character that doesn't compete with product photography. Avoid decorative/display fonts entirely — this is a transactional product, not an editorial one.

Mobile: scale H1 down to 24px/32px, H2 to 20px/28px; body text stays 16px minimum (never shrink body text below 16px — protects tap-target legibility and avoids iOS auto-zoom-on-focus issues in form fields).

## 4. Layout Direction

- **Grid:** 12-column responsive grid, 24px gutters desktop / 16px mobile.
- **Max content width:** 1280px, centered, with side padding scaling from 16px (mobile) to 64px (desktop) — content never spans true edge-to-edge on large screens except imagery/hero sections.
- **Spacing scale:** 4px base unit — 4, 8, 12, 16, 24, 32, 48, 64. Use this scale exclusively for margin/padding; no arbitrary spacing values.
- **Product grids:** 2 columns mobile, 3 columns tablet, 4 columns desktop. Card aspect ratio for product images: 1:1 (square) for visual consistency across vendors with wildly different photography.
- **Page structure pattern (shopper-facing):** Nav bar (sticky) → page content (breadcrumb where relevant) → footer. No persistent sidebar on shopper-facing screens — keep the browsing experience full-width and uncluttered.
- **Page structure pattern (vendor/admin):** Persistent left sidebar (icons + labels, collapsible to icons-only on smaller viewports) + top bar (page title, account menu) + main content area. This is the one place a dashboard-style layout is appropriate, per Section 6 below.

## 5. Component Style

- **Buttons:** Rounded corners, 8px radius. Primary = solid indigo fill, white text. Secondary = white fill, indigo border and text. Destructive = solid red fill. Disabled = neutral-300 fill, neutral-600 text, no hover state. Minimum tap target 44x44px on all interactive elements (mobile accessibility baseline).
- **Cards:** 12px radius, 1px neutral-300 border OR subtle shadow (`0 1px 3px rgba(0,0,0,0.08)`) — pick one consistently (shadow preferred for product cards to let them "lift" off the page background; border preferred for dense table rows/dashboard cards).
- **Inputs:** 8px radius, 1px neutral-300 border, indigo border + subtle ring on focus. Error state = red border + red helper text below, not just color alone (icon + text) for accessibility.
- **Badges/status pills:** Fully rounded (pill shape), small caps or sentence case text, colored background at ~15% opacity of the status color with full-opacity text of that same color (e.g., in-stock badge = pale green background, dark green text).
- **Modals:** Centered, max-width 480px, overlay at 50% black, close on overlay click and Esc key, always include an explicit close (X) button — never rely on overlay-click alone.
- **Toasts:** Top-right on desktop, top-full-width on mobile, auto-dismiss after 4s (except error toasts tied to failed payments — those persist until dismissed, since missing them could confuse a user about whether they were charged).
- **Icons:** Single icon set throughout (e.g., Lucide/Feather-style line icons) — never mix icon families.

## 6. Dashboard Structure (Vendor & Admin)

Both Vendor and Admin use the same dashboard shell, differing only in nav items and data shown — this consistency matters for an AI builder implementing both from one layout component.

**Shell:**
```
┌──────────┬──────────────────────────────────┐
│          │  Top bar: Page title | Account ▾  │
│ Sidebar  ├──────────────────────────────────┤
│  - Logo  │                                    │
│  - Nav   │         Main content area          │
│    items │      (cards, tables, forms)        │
│  - ...   │                                    │
│          │                                    │
└──────────┴──────────────────────────────────┘
```

- **Sidebar nav (Vendor):** Dashboard, Products, Orders. **Sidebar nav (Admin):** Dashboard, Users, Vendors.
- **Dashboard home pattern:** summary metric cards in a row at top (3–4 cards, single row desktop, stacked 2x2 mobile) → primary data table/list below. This pattern repeats for every dashboard-style screen in the App Flow document (11.1, 12.1).
- **Tables:** Zebra-free (rely on row-hover highlight, not alternating background, for a cleaner look), sortable column headers where applicable, row actions right-aligned, sticky header on scroll for long tables.
- **Forms within dashboards** (Add/Edit Product, etc.): single-column layout, max-width 640px even within the wider content area — long form rows across the full dashboard width hurt scanability.

## 7. Mobile Responsiveness

- **Breakpoints:** Mobile < 640px · Tablet 640–1024px · Desktop > 1024px.
- **Shopper-facing nav:** collapses to a hamburger menu + persistent cart icon + search icon on mobile; full horizontal nav bar on tablet/desktop.
- **Vendor/Admin sidebar:** collapses to a bottom tab bar or a slide-out drawer (triggered by a hamburger in the top bar) below 1024px — a persistent left sidebar doesn't work on phone-width screens.
- **Product grid:** drops to 2 columns on mobile (per Section 4).
- **Checkout:** single-column, full-width fields, sticky "Place Order" button pinned to the bottom of the viewport so it's always reachable without scrolling back down.
- **Tables (vendor/admin) on mobile:** convert to stacked card view per row (label: value pairs) rather than horizontal-scroll tables — horizontal scroll on data tables is a common mobile UX failure point and should be avoided.
- Touch targets minimum 44x44px everywhere, consistent with Section 5.

## 8. User Experience Principles

1. **Never leave the user guessing what happened.** Every action (add to cart, save product, place order) gets an explicit, visible confirmation — toast, inline state change, or navigation. This is already encoded per-screen in the App Flow document; the visual design must make these confirmations impossible to miss (color + icon + text, never color alone).
2. **Protect the user from money mistakes.** Checkout and payment states get extra visual weight and reassurance copy (per App Flow Section 8) — this is the one place in the product where ambiguity has real consequences.
3. **Progressive disclosure on dashboards.** Vendor/admin screens default to summary views (cards, tables) and let users drill into detail — never front-load a dense form or full data dump on a dashboard home.
4. **Consistent status vocabulary.** The same color/badge pattern for "status" (order status, product status, user status) is used everywhere — a user who learns it once in Order History understands it instantly in Vendor Order Management.
5. **Graceful degradation over dead ends.** When the AI search feature fails, the product still works (falls back to keyword search per App Flow Section 4) — this principle should extend visually too: loading and error states should never block the user from navigating away or trying an alternative path.
6. **Accessibility is not optional.** WCAG AA contrast minimums (already reflected in the palette), never color-alone status indicators, visible focus states on all interactive elements, and semantic heading structure throughout.

## 9. Visual References

Since specific copyrighted brand screenshots can't be reproduced here, use these as directional references for an AI builder or designer to study (not copy):

- **Overall marketplace feel:** the restrained, product-forward layout style of well-known modern marketplaces (large square product photography, minimal chrome, clear price hierarchy) — think the visual weight distribution of sites like Etsy or Shopify-powered storefronts, generically, not any single brand's exact styling.
- **Dashboard shell pattern:** the sidebar + top bar + card/table content pattern common to modern SaaS admin panels (e.g., the general layout convention used by tools like Linear or Stripe's dashboard) — again, as a structural reference, not a visual clone.
- **Typography/spacing discipline:** Inter at the sizes specified above, with the 4px spacing scale, produces a look consistent with most modern Tailwind-based design systems — this is intentional, since the frontend stack is already Tailwind CSS per the TRD, and Tailwind's default spacing/type scale aligns closely with what's specified here.

---

**Implementation note for the AI app builder:** every color, spacing, and type value in this brief is a literal design token — implement them as CSS custom properties / Tailwind theme extensions (e.g., `colors.primary = '#3730A3'`) rather than re-deriving similar-looking values, so the design stays consistent across every screen defined in the App Flow document.
