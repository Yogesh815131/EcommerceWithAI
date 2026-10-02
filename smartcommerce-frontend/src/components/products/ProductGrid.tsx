import type { Product } from "@/types/product"
import { ProductCard } from "./ProductCard"
import { ProductCardSkeleton } from "./ProductCardSkeleton"
import { PackageSearch } from "lucide-react"

interface ProductGridProps {
  products: Product[] | undefined
  isLoading: boolean
}

export function ProductGrid({ products, isLoading }: ProductGridProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <ProductCardSkeleton key={i} />
        ))}
      </div>
    )
  }

  if (!products || products.length === 0) {
    return (
      <div className="animate-fade-up flex flex-col items-center justify-center gap-3 rounded-card border border-dashed border-border py-20 text-center">
        <PackageSearch className="size-8 text-ink-muted" />
        <div>
          <p className="font-display font-medium text-ink">No matches for that search</p>
          <p className="mt-1 text-sm text-ink-muted">
            Try describing what you're looking for differently, or browse all products.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4">
      {products.map((product, index) => (
        <ProductCard key={product.id} product={product} index={index} />
      ))}
    </div>
  )
}
