import { isAxiosError } from "axios"
import { createAddress } from "@/lib/api/addresses"
import { createOrder } from "@/lib/api/orders"
import { createPayment } from "@/lib/api/payments"
import type { Address } from "@/types/address"
import type { CheckoutFormValues } from "@/lib/validation/checkout"
import type { Order } from "@/types/order"
import type { Payment } from "@/types/payment"

export interface PlacedCheckout {
  order: Order
  payment: Payment
}

function sameAddress(saved: Address, values: CheckoutFormValues) {
  return (
    saved.line1 === values.line1 &&
    (saved.line2 ?? "") === values.line2 &&
    saved.city === values.city &&
    saved.state === values.state &&
    saved.postalCode === values.postalCode &&
    saved.country === values.country
  )
}

export async function submitCheckout(
  values: CheckoutFormValues,
  savedAddresses: Address[]
): Promise<PlacedCheckout> {
  const match = savedAddresses.find((address) => sameAddress(address, values))
  const address =
    match ??
    (await createAddress({
      line1: values.line1,
      line2: values.line2 || undefined,
      city: values.city,
      state: values.state,
      postalCode: values.postalCode,
      country: values.country,
      isDefault: savedAddresses.length === 0,
    }))

  const order = await createOrder(address.id)
  const payment = await createPayment(order.id, values.paymentMethodId)
  return { order, payment }
}

export function checkoutErrorMessage(error: unknown) {
  if (isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === "string" && message.length > 0) return message
    if (error.response?.status === 401) return "Please sign in again to place this order."
  }
  return "Couldn't place the order. Nothing was confirmed. Please try again."
}
