import { useQuery } from "@tanstack/react-query"
import { fetchAddresses } from "@/lib/api/addresses"
import { useAuthStore } from "@/store/useAuthStore"

export const ADDRESSES_QUERY_KEY = ["addresses"] as const

export function useAddresses() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())

  return useQuery({
    queryKey: ADDRESSES_QUERY_KEY,
    queryFn: fetchAddresses,
    enabled: isAuthenticated,
  })
}
