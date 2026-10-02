import { useState, type CSSProperties } from "react"
import { useQuery } from "@tanstack/react-query"
import { fetchProducts, fetchCategories } from "@/lib/api/products"
import { Header } from "@/components/layout/Header"
import { ProductGrid } from "@/components/products/ProductGrid"

export function Home() {
  const [query, setQuery] = useState("")
  const [activeCategoryId, setActiveCategoryId] = useState<number | undefined>(undefined)

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategories,
    staleTime: 5 * 60_000, // categories change rarely — cache longer
  })

  const { data: productPage, isFetching } = useQuery({
    queryKey: ["products", query, activeCategoryId],
    queryFn: () => fetchProducts({ search: query || undefined, categoryId: activeCategoryId }),
  })

  return (
    <div className="min-h-screen bg-bg">
      <Header onSearch={setQuery} isSearching={isFetching} />

      <main className="mx-auto max-w-6xl px-6 py-10">
        <div className="home-hero mb-8 rounded-card px-1 py-2">
          <div className="relative z-10 flex flex-wrap items-end justify-between gap-4">
            <div className="animate-fade-up">
              <h1 className="font-display text-2xl font-semibold text-ink">
                {query ? `Results for "${query}"` : "Discover something new"}
              </h1>
              <p className="mt-1 text-sm text-ink-muted">
                {query
                  ? "Matched with AI-powered search across every vendor."
                  : "Curated from every seller on the marketplace."}
              </p>
            </div>

            <div className="flex flex-wrap gap-1.5">
              <button
                onClick={() => setActiveCategoryId(undefined)}
                style={{ "--stagger": 0 } as CSSProperties}
                className={
                  "animate-stagger rounded-full px-3.5 py-1.5 text-xs font-medium transition-colors duration-200 " +
                  (activeCategoryId === undefined
                    ? "bg-ink text-white"
                    : "border border-border bg-surface text-ink-muted hover:border-ink/20 hover:text-ink")
                }
              >
                All
              </button>
              {Array.isArray(categories) &&
                categories.map((cat, index) => (
                  <button
                    key={cat.id}
                    onClick={() => setActiveCategoryId(cat.id)}
                    style={{ "--stagger": index + 1 } as CSSProperties}
                    className={
                      "animate-stagger rounded-full px-3.5 py-1.5 text-xs font-medium transition-colors duration-200 " +
                      (activeCategoryId === cat.id
                        ? "bg-ink text-white"
                        : "border border-border bg-surface text-ink-muted hover:border-ink/20 hover:text-ink")
                    }
                  >
                    {cat.name}
                  </button>
                ))}
            </div>
          </div>
        </div>

        <ProductGrid products={productPage?.content} isLoading={isFetching} />
      </main>
    </div>
  )
}
