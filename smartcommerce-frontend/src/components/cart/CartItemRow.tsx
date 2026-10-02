import { Minus, Plus, Trash2, PackageX } from "lucide-react"
import type { CartItem } from "@/types/cart"
import { useUpdateCartItem, useRemoveCartItem } from "@/hooks/useCart"

export function CartItemRow({ item }: { item: CartItem }) {
  const updateQuantity = useUpdateCartItem()
  const removeItem = useRemoveCartItem()

  const atStockLimit = item.quantity >= item.availableStock
  const isMutating = updateQuantity.isPending || removeItem.isPending

  return (
    <div className="flex gap-3 py-4">
      <div className="size-16 shrink-0 overflow-hidden rounded-lg bg-bg">
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.name} className="size-full object-cover" />
        ) : (
          <div className="flex size-full items-center justify-center text-ink-muted">
            <PackageX className="size-5" />
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col justify-between">
        <div className="flex items-start justify-between gap-2">
          <p className="font-display line-clamp-2 text-sm font-medium text-ink">{item.name}</p>
          <button
            onClick={() => removeItem.mutate(item.productId)}
            disabled={isMutating}
            className="shrink-0 text-ink-muted hover:text-red-600 disabled:opacity-40"
            aria-label={`Remove ${item.name} from cart`}
          >
            <Trash2 className="size-4" />
          </button>
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 rounded-full border border-border">
            <button
              onClick={() =>
                updateQuantity.mutate({ productId: item.productId, quantity: item.quantity - 1 })
              }
              disabled={isMutating || item.quantity <= 1}
              className="flex size-7 items-center justify-center text-ink-muted hover:text-ink disabled:opacity-30"
              aria-label="Decrease quantity"
            >
              <Minus className="size-3" />
            </button>
            <span className="w-4 text-center text-sm font-medium text-ink">{item.quantity}</span>
            <button
              onClick={() =>
                updateQuantity.mutate({ productId: item.productId, quantity: item.quantity + 1 })
              }
              disabled={isMutating || atStockLimit}
              className="flex size-7 items-center justify-center text-ink-muted hover:text-ink disabled:opacity-30"
              aria-label="Increase quantity"
            >
              <Plus className="size-3" />
            </button>
          </div>

          <span className="font-mono-price text-sm font-semibold text-ink">
            ₹{item.subtotal.toLocaleString("en-IN")}
          </span>
        </div>

        {atStockLimit && (
          <p className="mt-1 text-xs text-accent">Only {item.availableStock} in stock</p>
        )}
      </div>
    </div>
  )
}
