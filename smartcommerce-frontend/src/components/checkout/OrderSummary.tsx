import { PackageX } from "lucide-react"
import type { Cart } from "@/types/cart"
import { Card } from "@/components/ui/card"

function formatInr(amount: number) {
  return `₹${amount.toLocaleString("en-IN")}`
}

interface OrderSummaryProps {
  cart: Cart
}

export function OrderSummary({ cart }: OrderSummaryProps) {
  const shipping = 0
  const tax = 0
  const discount = 0
  const total = cart.subtotal + shipping + tax - discount

  return (
    <Card className="p-5">
      <h2 className="font-display text-base font-semibold text-ink">Order summary</h2>
      <ul className="mt-4 divide-y divide-border">
        {cart.items.map((item) => (
          <li key={item.productId} className="flex gap-3 py-3">
            <div className="size-14 shrink-0 overflow-hidden rounded-lg bg-bg">
              {item.imageUrl ? (
                <img src={item.imageUrl} alt="" className="size-full object-cover" />
              ) : (
                <div className="flex size-full items-center justify-center text-ink-muted">
                  <PackageX className="size-4" />
                </div>
              )}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-ink">{item.name}</p>
              <p className="mt-0.5 text-xs text-ink-muted">
                Qty {item.quantity} · {formatInr(item.price)}
              </p>
            </div>
            <p className="font-mono-price text-sm font-semibold text-ink">{formatInr(item.subtotal)}</p>
          </li>
        ))}
      </ul>

      <dl className="mt-2 space-y-2 border-t border-border pt-3 text-sm">
        <Row label="Subtotal" value={formatInr(cart.subtotal)} />
        <Row label="Shipping" value={formatInr(shipping)} />
        <Row label="Discount" value={formatInr(discount)} />
        <Row label="Tax" value={formatInr(tax)} />
        <div className="flex items-baseline justify-between border-t border-border pt-3">
          <dt className="font-medium text-ink">Total</dt>
          <dd className="font-mono-price text-lg font-semibold text-ink">{formatInr(total)}</dd>
        </div>
      </dl>
      <p className="mt-3 text-xs text-ink-muted">
        Shipping, tax, and discount are ₹0 because that is what the order service charges today.
      </p>
    </Card>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <dt className="text-ink-muted">{label}</dt>
      <dd className="font-mono-price text-ink">{value}</dd>
    </div>
  )
}

export { formatInr }
