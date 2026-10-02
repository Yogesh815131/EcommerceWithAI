import { apiClient } from "@/lib/apiClient"
import type { Payment } from "@/types/payment"

export async function createPayment(orderId: number, paymentMethodId: string): Promise<Payment> {
  const { data } = await apiClient.post<Payment>("/api/payments", { orderId, paymentMethodId })
  return data
}
