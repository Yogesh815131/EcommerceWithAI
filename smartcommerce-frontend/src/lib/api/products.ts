import { apiClient } from "@/lib/apiClient"
import type { PageResponse, Product, Category } from "@/types/product"

interface FetchProductsParams {
  search?: string
  categoryId?: number
  page?: number
  size?: number
}

export async function fetchProducts({
  search,
  categoryId,
  page = 0,
  size = 12,
}: FetchProductsParams): Promise<PageResponse<Product>> {
  const { data } = await apiClient.get<PageResponse<Product>>("/api/products", {
    params: {
      search: search || undefined,
      categoryId,
      page,
      size,
    },
  })
  return data
}

export async function fetchProductById(id: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/api/products/${id}`)
  return data
}

export async function fetchCategories(): Promise<Category[]> {
  const { data } = await apiClient.get<Category[]>("/api/categories")
  return data
}
