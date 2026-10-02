import { apiClient } from "@/lib/apiClient"
import type { Cart } from "@/types/cart"

export async function fetchCart(): Promise<Cart> {
  const { data } = await apiClient.get<Cart>("/api/cart")
  return data
}

export async function addToCart(productId: number, quantity: number): Promise<Cart> {
  const { data } = await apiClient.post<Cart>("/api/cart/items", { productId, quantity })
  return data
}

export async function updateCartItemQuantity(productId: number, quantity: number): Promise<Cart> {
  const { data } = await apiClient.patch<Cart>(`/api/cart/items/${productId}`, { quantity })
  return data
}

export async function removeCartItem(productId: number): Promise<Cart> {
  const { data } = await apiClient.delete<Cart>(`/api/cart/items/${productId}`)
  return data
}
