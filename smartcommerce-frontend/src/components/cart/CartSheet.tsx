import { ShoppingBag } from "lucide-react"
import { Link } from "react-router"
import { Sheet, SheetContent } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useCart } from "@/hooks/useCart"
import { CartItemRow } from "./CartItemRow"

interface CartSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function CartSheet({ open, onOpenChange }: CartSheetProps) {
  const { data: cart, isLoading } = useCart()

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent title="Your cart">
        <div className="flex-1 overflow-y-auto px-5">
          {isLoading && (
            <div className="divide-y divide-border">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="flex gap-3 py-4">
                  <Skeleton className="size-16 shrink-0 rounded-lg" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {!isLoading && (!cart || cart.items.length === 0) && (
            <div className="flex flex-col items-center justify-center gap-3 py-20 text-center">
              <ShoppingBag className="size-8 text-ink-muted" />
              <div>
                <p className="font-display font-medium text-ink">Your cart is empty</p>
                <p className="mt-1 text-sm text-ink-muted">
                  Browse the catalog and add something you like.
                </p>
              </div>
            </div>
          )}

          {!isLoading && cart && cart.items.length > 0 && (
            <div className="divide-y divide-border">
              {cart.items.map((item) => (
                <CartItemRow key={item.productId} item={item} />
              ))}
            </div>
          )}
        </div>

        {cart && cart.items.length > 0 && (
          <div className="border-t border-border px-5 py-4">
            <div className="mb-3 flex items-center justify-between">
              <span className="text-sm text-ink-muted">Subtotal</span>
              <span className="font-mono-price text-lg font-semibold text-ink">
                ₹{cart.subtotal.toLocaleString("en-IN")}
              </span>
            </div>
            <Button asChild size="lg" className="w-full">
              <Link to="/checkout">Checkout</Link>
            </Button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}
