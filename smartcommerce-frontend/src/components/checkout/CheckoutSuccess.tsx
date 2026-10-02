import { Link } from "react-router"
import { Check } from "lucide-react"
import type { Order } from "@/types/order"
import type { Payment } from "@/types/payment"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { formatInr } from "@/components/checkout/OrderSummary"

interface CheckoutSuccessProps {
  order: Order
  payment: Payment
}

export function CheckoutSuccess({ order, payment }: CheckoutSuccessProps) {
  return (
    <main className="mx-auto max-w-lg px-4 py-16 sm:px-6">
      <Card className="p-8 text-center">
        <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-green-50 text-success">
          <Check className="size-6" />
        </div>
        <h1 className="font-display mt-4 text-2xl font-semibold text-ink">Order placed</h1>
        <p className="mt-2 text-sm text-ink-muted">
          Your payment went through. The cart is now empty.
        </p>
        <dl className="mt-6 space-y-2 text-left text-sm">
          <div className="flex justify-between gap-4">
            <dt className="text-ink-muted">Order ID</dt>
            <dd className="font-medium text-ink">#{order.id}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ink-muted">Status</dt>
            <dd className="font-medium text-ink">{order.status}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ink-muted">Payment</dt>
            <dd className="font-medium text-ink">{payment.status}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ink-muted">Total</dt>
            <dd className="font-mono-price font-semibold text-ink">{formatInr(order.total)}</dd>
          </div>
        </dl>
        <Button asChild className="mt-8">
          <Link to="/">Back to shopping</Link>
        </Button>
      </Card>
    </main>
  )
}
