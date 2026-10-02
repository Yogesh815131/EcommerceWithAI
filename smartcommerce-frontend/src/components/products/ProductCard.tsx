import { useState, type CSSProperties } from "react"
import { Plus, PackageX, BellRing, BellOff, Check } from "lucide-react"
import { Link, useNavigate } from "react-router"
import type { Product } from "@/types/product"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { useAddToCart } from "@/hooks/useCart"
import { useNotifyOnRestock } from "@/hooks/useNotifyOnRestock"
import { useAuthStore } from "@/store/useAuthStore"
import { cn } from "@/lib/utils"

export function ProductCard({ product, index = 0 }: { product: Product; index?: number }) {
  const navigate = useNavigate()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  const addToCart = useAddToCart()
  const notifyOnRestock = useNotifyOnRestock()
  const [subscribed, setSubscribed] = useState(false)
  const outOfStock = product.stockQuantity === 0

  function handleAddToCart(e: React.MouseEvent) {
    e.preventDefault() // don't follow the card's own Link
    e.stopPropagation()
    if (!isAuthenticated) {
      navigate("/login")
      return
    }
    addToCart.mutate({ productId: product.id, quantity: 1 })
  }

  function handleNotify(e: React.MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    if (!isAuthenticated) {
      navigate("/login")
      return
    }
    notifyOnRestock.mutate(product.id, { onSuccess: () => setSubscribed(true) })
  }

  return (
    <div
      className="animate-stagger"
      style={{ "--stagger": Math.min(index, 11) } as CSSProperties}
    >
      <Card className="group overflow-hidden transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
      <Link to={`/products/${product.id}`} className="block">
        <div className="relative aspect-square overflow-hidden bg-bg">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              loading="lazy"
              className={cn(
                "size-full object-cover transition-all duration-300",
                outOfStock
                  ? "scale-100 grayscale opacity-50"
                  : "group-hover:scale-[1.04]"
              )}
            />
          ) : (
            <div className="flex size-full items-center justify-center text-ink-muted">
              <PackageX className="size-8" />
            </div>
          )}

          {outOfStock && (
            <div className="absolute inset-0 flex items-center justify-center bg-ink/5">
              <Badge variant="outline" className="bg-surface shadow-sm">
                Out of stock
              </Badge>
            </div>
          )}

          {!outOfStock && (
            <button
              onClick={handleAddToCart}
              disabled={addToCart.isPending}
              className="absolute bottom-3 right-3 flex size-9 items-center justify-center rounded-full bg-surface text-ink opacity-0 shadow-md transition-all duration-200 group-hover:opacity-100 hover:bg-brand hover:text-white disabled:pointer-events-none"
              aria-label={`Add ${product.name} to cart`}
            >
              <Plus className="size-4" />
            </button>
          )}
        </div>

        <div className="p-4">
          <p className="text-xs text-ink-muted">Sold by vendor #{product.vendorId}</p>
          <h3
            className={cn(
              "font-display mt-0.5 truncate text-sm font-medium",
              outOfStock ? "text-ink-muted" : "text-ink"
            )}
          >
            {product.name}
          </h3>

          <div className="mt-2.5 flex items-baseline gap-2">
            <span
              className={cn(
                "font-mono-price text-base font-semibold",
                outOfStock ? "text-ink-muted" : "text-ink"
              )}
            >
              ₹{product.price.toLocaleString("en-IN")}
            </span>
          </div>

          {outOfStock && (
            <button
              onClick={handleNotify}
              disabled={subscribed || notifyOnRestock.isPending}
              className={cn(
                "mt-3 flex w-full items-center justify-center gap-1.5 rounded-full px-3 py-2 text-xs font-medium transition-colors",
                subscribed
                  ? "bg-brand-dim text-brand-hover"
                  : "border border-border text-ink hover:border-brand hover:text-brand disabled:opacity-60"
              )}
            >
              {subscribed ? (
                <>
                  <Check className="size-3.5" />
                  We'll notify you
                </>
              ) : notifyOnRestock.isPending ? (
                <>
                  <BellOff className="size-3.5 animate-pulse" />
                  Requesting…
                </>
              ) : (
                <>
                  <BellRing className="size-3.5" />
                  Notify me
                </>
              )}
            </button>
          )}
        </div>
      </Link>
    </Card>
    </div>
  )
}
