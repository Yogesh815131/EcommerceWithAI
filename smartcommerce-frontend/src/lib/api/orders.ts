import { apiClient } from "@/lib/apiClient"
import type { Order } from "@/types/order"

export async function createOrder(shippingAddressId: number): Promise<Order> {
  const { data } = await apiClient.post<Order>("/api/orders", { shippingAddressId })
  return data
}
