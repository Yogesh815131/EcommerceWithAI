// Mirrors cart-service's CartDtos exactly.

export interface CartItem {
  productId: number
  name: string
  imageUrl: string | null
  price: number
  quantity: number
  subtotal: number
  availableStock: number
}

export interface Cart {
  items: CartItem[]
  subtotal: number
}
