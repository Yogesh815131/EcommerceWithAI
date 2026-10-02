// Mirrors product-service's ProductResponse — keep in sync with the backend DTO.

export type ProductStatus = "ACTIVE" | "INACTIVE"

export interface Product {
  id: number
  vendorId: number
  categoryId: number
  name: string
  description: string | null
  price: number
  stockQuantity: number
  status: ProductStatus
  imageUrl: string | null
  createdAt: string
}

export interface Category {
  id: number
  name: string
  parentCategoryId: number | null
}

// Spring Data's default Page<T> JSON shape
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // current page (0-indexed)
  size: number
  last: boolean
}
