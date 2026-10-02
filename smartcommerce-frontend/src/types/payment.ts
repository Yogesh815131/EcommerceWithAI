// Mirrors payment-service PaymentResponse.
// The only supported method is a Stripe PaymentMethod id (pm_...), never a raw card number.

export interface Payment {
  id: number
  orderId: number
  amount: number
  status: string
  failureReason: string | null
  createdAt: string
}
