import { useState } from "react"
import { Link } from "react-router"
import { ShoppingBag } from "lucide-react"
import { Header } from "@/components/layout/Header"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuthStore } from "@/store/useAuthStore"
import { useCart } from "@/hooks/useCart"
import { useAddresses } from "@/hooks/useAddresses"
import { CheckoutForm } from "@/components/checkout/CheckoutForm"
import { CheckoutSuccess } from "@/components/checkout/CheckoutSuccess"
import type { Order } from "@/types/order"
import type { Payment } from "@/types/payment"

export function Checkout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  const email = useAuthStore((s) => s.user?.email ?? "")
  const { data: cart, isLoading: cartLoading } = useCart()
  const { data: addresses } = useAddresses()
  const [placed, setPlaced] = useState<{ order: Order; payment: Payment } | null>(null)

  return (
    <div className="min-h-screen bg-bg">
      <Header onSearch={() => {}} isSearching={false} />

      {placed ? (
        <CheckoutSuccess order={placed.order} payment={placed.payment} />
      ) : (
        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
          <h1 className="font-display text-2xl font-semibold text-ink">Checkout</h1>
          <p className="mt-1 text-sm text-ink-muted">Confirm your details, then place the order.</p>

          {!isAuthenticated && <SignInPrompt />}

          {isAuthenticated && cartLoading && <CheckoutSkeleton />}

          {isAuthenticated && !cartLoading && (!cart || cart.items.length === 0) && <EmptyCart />}

          {isAuthenticated && cart && cart.items.length > 0 && (
            <CheckoutForm
              cart={cart}
              email={email}
              savedAddresses={addresses ?? []}
              onPlaced={setPlaced}
            />
          )}
        </main>
      )}
    </div>
  )
}

function SignInPrompt() {
  return (
    <Card className="mx-auto mt-10 max-w-md p-8 text-center">
      <h2 className="font-display text-lg font-semibold text-ink">Sign in to check out</h2>
      <p className="mt-2 text-sm text-ink-muted">Your cart is saved on your account, so checkout needs a signed-in user.</p>
      <Button asChild className="mt-6">
        <Link to="/login">Sign in</Link>
      </Button>
    </Card>
  )
}

function EmptyCart() {
  return (
    <Card className="mx-auto mt-10 flex max-w-md flex-col items-center gap-3 p-10 text-center">
      <ShoppingBag className="size-8 text-ink-muted" />
      <h2 className="font-display text-lg font-semibold text-ink">Your cart is empty</h2>
      <p className="text-sm text-ink-muted">Add a product before checking out.</p>
      <Button asChild variant="outline" className="mt-2">
        <Link to="/">Browse products</Link>
      </Button>
    </Card>
  )
}

function CheckoutSkeleton() {
  return (
    <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div className="space-y-4">
        <Skeleton className="h-56 w-full rounded-card" />
        <Skeleton className="h-72 w-full rounded-card" />
      </div>
      <Skeleton className="h-80 w-full rounded-card" />
    </div>
  )
}
