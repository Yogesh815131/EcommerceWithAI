import { useState } from "react"
import { useParams, useNavigate, Link } from "react-router"
import { useQuery } from "@tanstack/react-query"
import { Minus, Plus, PackageX, ArrowLeft, ShoppingBag, BellRing, Check } from "lucide-react"
import { fetchProductById } from "@/lib/api/products"
import { Header } from "@/components/layout/Header"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useAddToCart } from "@/hooks/useCart"
import { useNotifyOnRestock } from "@/hooks/useNotifyOnRestock"
import { useAuthStore } from "@/store/useAuthStore"

export function ProductDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const productId = Number(id)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  const addToCart = useAddToCart()
  const notifyOnRestock = useNotifyOnRestock()
  const [subscribed, setSubscribed] = useState(false)
  const [quantity, setQuantity] = useState(1)

  const {
    data: product,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["product", productId],
    queryFn: () => fetchProductById(productId),
    enabled: Number.isFinite(productId),
  })

  function handleAddToCart() {
    if (!isAuthenticated) {
      navigate("/login")
      return
    }
    if (!product) return
    addToCart.mutate({ productId: product.id, quantity })
  }

  function handleNotify() {
    if (!isAuthenticated) {
      navigate("/login")
      return
    }
    if (!product) return
    notifyOnRestock.mutate(product.id, { onSuccess: () => setSubscribed(true) })
  }

  return (
    <div className="min-h-screen bg-bg">
      <Header onSearch={() => {}} isSearching={false} />

      <main className="mx-auto max-w-5xl px-6 py-8">
        <Link
          to="/"
          className="mb-6 inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink"
        >
          <ArrowLeft className="size-3.5" />
          Back to catalog
        </Link>

        {isLoading && (
          <div className="grid grid-cols-1 gap-10 md:grid-cols-2">
            <Skeleton className="aspect-square w-full rounded-card" />
            <div className="space-y-3">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-7 w-3/4" />
              <Skeleton className="h-5 w-28" />
              <Skeleton className="mt-4 h-24 w-full" />
            </div>
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-center justify-center gap-3 rounded-card border border-dashed border-border py-20 text-center">
            <PackageX className="size-8 text-ink-muted" />
            <div>
              <p className="font-display font-medium text-ink">Couldn't find that product</p>
              <p className="mt-1 text-sm text-ink-muted">
                It may have been removed, or the link is incorrect.
              </p>
            </div>
          </div>
        )}

        {product && (
          <div className="grid grid-cols-1 gap-10 md:grid-cols-2">
            <div className="aspect-square overflow-hidden rounded-card bg-surface">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className={`size-full object-cover ${
                    product.stockQuantity === 0 ? "grayscale opacity-50" : ""
                  }`}
                />
              ) : (
                <div className="flex size-full items-center justify-center text-ink-muted">
                  <PackageX className="size-10" />
                </div>
              )}
            </div>

            <div>
              <p className="text-sm text-ink-muted">Sold by vendor #{product.vendorId}</p>
              <h1 className="font-display mt-1 text-2xl font-semibold text-ink">
                {product.name}
              </h1>

              <div className="mt-3 flex items-center gap-3">
                <span className="font-mono-price text-2xl font-semibold text-ink">
                  ₹{product.price.toLocaleString("en-IN")}
                </span>
                {product.stockQuantity === 0 && (
                  <span className="rounded-full border border-border px-2.5 py-0.5 text-xs font-medium text-ink-muted">
                    Out of stock
                  </span>
                )}
              </div>

              {product.description && (
                <p className="mt-5 whitespace-pre-line text-sm leading-relaxed text-ink-muted">
                  {product.description}
                </p>
              )}

              {product.stockQuantity > 0 ? (
                <div className="mt-6 flex items-center gap-4">
                  <div className="flex items-center gap-3 rounded-full border border-border px-1.5">
                    <button
                      onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                      className="flex size-8 items-center justify-center text-ink-muted hover:text-ink disabled:opacity-30"
                      aria-label="Decrease quantity"
                    >
                      <Minus className="size-3.5" />
                    </button>
                    <span className="w-5 text-center text-sm font-medium text-ink">
                      {quantity}
                    </span>
                    <button
                      onClick={() =>
                        setQuantity((q) => Math.min(product.stockQuantity, q + 1))
                      }
                      disabled={quantity >= product.stockQuantity}
                      className="flex size-8 items-center justify-center text-ink-muted hover:text-ink disabled:opacity-30"
                      aria-label="Increase quantity"
                    >
                      <Plus className="size-3.5" />
                    </button>
                  </div>

                  <Button
                    size="lg"
                    className="flex-1"
                    disabled={addToCart.isPending}
                    onClick={handleAddToCart}
                  >
                    <ShoppingBag className="size-4" />
                    {addToCart.isPending ? "Adding…" : "Add to cart"}
                  </Button>
                </div>
              ) : (
                <div className="mt-6">
                  <Button
                    size="lg"
                    variant={subscribed ? "outline" : "default"}
                    className="w-full"
                    disabled={subscribed || notifyOnRestock.isPending}
                    onClick={handleNotify}
                  >
                    {subscribed ? (
                      <>
                        <Check className="size-4" />
                        We'll email you when it's back
                      </>
                    ) : (
                      <>
                        <BellRing className="size-4" />
                        {notifyOnRestock.isPending ? "Requesting…" : "Notify me when back in stock"}
                      </>
                    )}
                  </Button>
                </div>
              )}

              {!isAuthenticated && (
                <p className="mt-3 text-xs text-ink-muted">
                  {product.stockQuantity > 0
                    ? "You'll need to sign in to add items to your cart."
                    : "You'll need to sign in to get notified."}
                </p>
              )}
              {addToCart.isSuccess && (
                <p className="mt-3 text-xs text-success">Added to your cart.</p>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
