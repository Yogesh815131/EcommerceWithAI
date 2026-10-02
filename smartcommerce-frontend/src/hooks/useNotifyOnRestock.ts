import { useMutation } from "@tanstack/react-query"
import { requestStockNotification } from "@/lib/api/stockAlerts"

export function useNotifyOnRestock() {
  return useMutation({
    mutationFn: (productId: number) => requestStockNotification(productId),
  })
}
