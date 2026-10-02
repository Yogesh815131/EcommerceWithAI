import { useMutation, useQueryClient } from "@tanstack/react-query"
import { submitCheckout } from "@/lib/api/checkout"
import { ADDRESSES_QUERY_KEY } from "@/hooks/useAddresses"
import { CART_QUERY_KEY } from "@/hooks/useCart"
import type { Address } from "@/types/address"
import type { CheckoutFormValues } from "@/lib/validation/checkout"

export function usePlaceOrder(savedAddresses: Address[]) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (values: CheckoutFormValues) => submitCheckout(values, savedAddresses),
    onSuccess: () => {
      queryClient.setQueryData(CART_QUERY_KEY, { items: [], subtotal: 0 })
      queryClient.invalidateQueries({ queryKey: ADDRESSES_QUERY_KEY })
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: CART_QUERY_KEY })
    },
  })
}
