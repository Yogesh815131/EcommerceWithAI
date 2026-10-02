// Mirrors order-service OrderResponse. Money fields arrive as JSON numbers.

export interface OrderItem {
  id: number
  productId: number
  vendorId: number
  productName: string
  unitPrice: number
  quantity: number
  lineTotal: number
  itemStatus: string
}

export interface Order {
  id: number
  status: string
  subtotal: number
  shippingCost: number
  tax: number
  total: number
  shippingAddressId: number
  placedAt: string
  items: OrderItem[]
}
