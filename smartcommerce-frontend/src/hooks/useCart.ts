import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { fetchCart, addToCart, updateCartItemQuantity, removeCartItem } from "@/lib/api/cart"
import { useAuthStore } from "@/store/useAuthStore"
import type { Cart } from "@/types/cart"

export const CART_QUERY_KEY = ["cart"] as const

// The cart only exists server-side for logged-in users (cart-service has no
// anonymous-cart support) — so the query is simply disabled for guests rather
// than falling back to any local state.
export function useCart() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())

  return useQuery({
    queryKey: CART_QUERY_KEY,
    queryFn: fetchCart,
    enabled: isAuthenticated,
  })
}

export function useAddToCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) =>
      addToCart(productId, quantity),
    onSuccess: (cart: Cart) => {
      queryClient.setQueryData(CART_QUERY_KEY, cart)
    },
  })
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) =>
      updateCartItemQuantity(productId, quantity),
    onSuccess: (cart: Cart) => {
      queryClient.setQueryData(CART_QUERY_KEY, cart)
    },
  })
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productId: number) => removeCartItem(productId),
    onSuccess: (cart: Cart) => {
      queryClient.setQueryData(CART_QUERY_KEY, cart)
    },
  })
}
