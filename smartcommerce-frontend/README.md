# SmartCommerce AI — Frontend

React 19 + Vite 8 + TypeScript starter for the SmartCommerce AI marketplace.

## Stack
- React 19 + Vite 8 (Rolldown) + TypeScript
- Tailwind CSS v4 (theme tokens in `src/index.css`)
- shadcn-style components in `src/components/ui/` (Radix primitives + CVA, source-owned, no CLI dependency)
- TanStack Query v5 for server state (`src/lib/api/`)
- Zustand for client state (`src/store/`)
- lucide-react for icons

## Getting started
```bash
npm install
npm run dev
```

## What's here
- `src/components/search/AiSearchBar.tsx` — the natural-language search bar (signature UI element, animated "thinking" ring while a query is in flight)
- `src/components/products/` — product grid, card, and skeleton loading states
- `src/lib/api/products.ts` — mock product API with a simulated 900ms delay, standing in for the real `/api/products` and `/api/ai/search` calls once the gateway is wired up. Swap this file's implementation for real `fetch`/axios calls — every consumer already goes through TanStack Query, so nothing else changes.
- `src/store/useCartStore.ts` — Zustand cart store

## Next steps
- Point `fetchProducts` at the real API Gateway
- Add auth (login/register pages, JWT in memory via a Zustand `authStore`, axios/fetch interceptor for refresh)
- Add React Router routes for product detail, cart, checkout, seller dashboard
- Wire the AI search bar's `onSearch` to the real AI Service's NL-search endpoint

## API integration (added)

The frontend now calls your real backend through the API Gateway:
- `.env.example` → copy to `.env` and set `VITE_API_BASE_URL` (defaults to `http://localhost:8080`)
- `src/lib/apiClient.ts` — axios instance, attaches the JWT to every request, auto-refreshes on 401 and retries once
- `src/store/useAuthStore.ts` — session state (persisted to localStorage for dev convenience — see the note in that file about moving the refresh token to an httpOnly cookie before this goes anywhere public)
- `src/pages/Login.tsx` / `Register.tsx` — real forms hitting `/api/auth/login` and `/api/auth/register`
- `src/lib/api/products.ts` — real calls to `/api/products` and `/api/categories`, typed against the actual `ProductResponse`/`Page<T>` shapes

**Before running against the real backend:** your `api-gateway` has no CORS configuration yet, so browser requests from `localhost:5173` will be blocked. Add a `globalcors` block under `spring.cloud.gateway` in `api-gateway/src/main/resources/application.yml` allowing origin `http://localhost:5173` (see chat for the exact snippet), then restart the gateway.

**Not yet wired:** cart/order/payment API calls (still local-only via Zustand), and the AI search endpoints (search currently filters against `/api/products?search=` directly rather than the AI service).

## Product detail + cart integration (added)

- `src/pages/ProductDetail.tsx` — `/products/:id`, quantity selector, add-to-cart, stock-aware
- `src/hooks/useCart.ts` — TanStack Query hooks (`useCart`, `useAddToCart`, `useUpdateCartItem`, `useRemoveCartItem`) calling the real `cart-service` endpoints through the gateway
- `src/components/cart/CartSheet.tsx` — slide-in cart panel, opened from the header cart icon
- **`useCartStore` (Zustand) has been removed.** Cart is now genuine server state — `cart-service` requires a logged-in user (no anonymous carts), so the cart query is simply disabled for guests rather than falling back to local state. Guests clicking "Add to cart" are redirected to `/login`.
- `src/pages/Checkout.tsx` — placeholder only; real checkout (address, `order-service`, Stripe) is the next step

## Out-of-stock treatment + "Notify me" (added)

- Out-of-stock products now render grayscale/dimmed (grid cards and detail page), with a persistent badge instead of relying on hover states
- A "Notify me when back in stock" button replaces the quantity selector/add-to-cart control whenever `stockQuantity === 0`
- `src/lib/api/stockAlerts.ts` is a **stub** — there is no backend endpoint for this yet. It simulates a delay and logs a warning instead of persisting anything. When you build the real endpoint (suggested: `POST /api/products/{id}/stock-alerts` in `product-service`, since it already owns stock data, triggering a notification via `notification-service` whenever stock is replenished), swap the body of `requestStockNotification` for a real `apiClient.post` call — `useNotifyOnRestock` and every UI consumer stay unchanged.
