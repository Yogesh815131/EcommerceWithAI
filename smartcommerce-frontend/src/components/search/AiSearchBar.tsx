import { useState, useRef, useEffect } from "react"
import { Sparkles, Search, X } from "lucide-react"
import { cn } from "@/lib/utils"

interface AiSearchBarProps {
  onSearch: (query: string) => void
  isThinking: boolean
}

const SUGGESTIONS = [
  "black running shoes under ₹4,000",
  "a laptop for video editing",
  "gift ideas for my wife",
  "noise-cancelling earbuds on sale",
]

export function AiSearchBar({ onSearch, isThinking }: AiSearchBarProps) {
  const [value, setValue] = useState("")
  const [focused, setFocused] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault()
        inputRef.current?.focus()
      }
    }
    window.addEventListener("keydown", handler)
    return () => window.removeEventListener("keydown", handler)
  }, [])

  function submit(q: string) {
    setValue(q)
    onSearch(q)
    inputRef.current?.blur()
  }

  const shortcut = typeof navigator !== "undefined" && /Mac|iPhone|iPad/i.test(navigator.platform)
    ? "⌘K"
    : "Ctrl K"

  return (
    <div className="relative w-full">
      <div className="relative rounded-pill">
        <div
          className={cn(
            "relative flex items-center rounded-pill border bg-surface pl-4 pr-1.5 transition-all duration-200",
            focused
              ? "border-brand/50 shadow-[0_0_0_4px_rgba(91,79,224,0.10)]"
              : "border-border shadow-[0_1px_2px_rgba(20,21,26,0.04)] hover:border-brand/30"
          )}
        >
          <Sparkles
            className={cn(
              "size-4 shrink-0 text-brand transition-opacity",
              isThinking ? "animate-pulse opacity-100" : "opacity-80"
            )}
          />
          <input
            ref={inputRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onFocus={() => setFocused(true)}
            onBlur={() => setTimeout(() => setFocused(false), 120)}
            onKeyDown={(e) => e.key === "Enter" && submit(value)}
            placeholder="Ask AI to find anything…"
            className="h-12 w-full bg-transparent px-3 text-sm text-ink placeholder:text-ink-muted focus:outline-none"
          />
          {!value && !focused && (
            <kbd className="mr-2 hidden shrink-0 rounded-md border border-border bg-surface px-1.5 py-0.5 font-mono text-[10px] font-medium text-ink-muted lg:inline-flex">
              {shortcut}
            </kbd>
          )}
          {value && (
            <button
              onClick={() => submit("")}
              className="mr-1 rounded-full p-1.5 text-ink-muted hover:bg-bg hover:text-ink"
              aria-label="Clear search"
            >
              <X className="size-3.5" />
            </button>
          )}
          <button
            onClick={() => submit(value)}
            className="my-1 flex size-9 shrink-0 items-center justify-center rounded-full bg-brand text-white shadow-sm transition-colors hover:bg-brand-hover"
            aria-label="Search"
          >
            <Search className="size-4" />
          </button>
        </div>
      </div>

      {focused && !value && (
        <div className="absolute inset-x-0 top-full z-40 mt-2 overflow-hidden rounded-2xl border border-border bg-surface p-2 shadow-xl">
          <p className="px-3 pb-1.5 pt-1 text-xs font-medium uppercase tracking-wide text-ink-muted">
            Try asking
          </p>
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onMouseDown={() => submit(s)}
              className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-left text-sm text-ink transition-colors hover:bg-brand-dim/60"
            >
              <Search className="size-3.5 text-brand" />
              {s}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
