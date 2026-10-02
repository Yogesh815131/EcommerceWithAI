// STUB — no backend endpoint exists for this yet.
//
// When it does, this should be a real POST (likely product-service, since it
// already owns stock data — e.g. `POST /api/products/{id}/stock-alerts`),
// persisting {userId, productId} and having whatever process restocks
// inventory trigger an email/notification via notification-service.
//
// Swap the body of this function for a real apiClient.post call — every
// consumer already goes through the useNotifyOnRestock hook, so nothing
// else needs to change.
export async function requestStockNotification(productId: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 500))
  console.warn(
    `[stockAlerts] STUB: would subscribe current user to restock alerts for product ${productId}. ` +
      "No backend endpoint exists yet — nothing was actually persisted."
  )
}
