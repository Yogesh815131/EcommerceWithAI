import { useEffect, useRef, useState, type ReactNode } from "react"
import {
  ShoppingBag,
  Heart,
  LogOut,
  Package,
  UserRound,
  ChevronDown,
  LayoutDashboard,
} from "lucide-react"
import { Link, useNavigate } from "react-router"
import { AiSearchBar } from "@/components/search/AiSearchBar"
import { CartSheet } from "@/components/cart/CartSheet"
import { useCart } from "@/hooks/useCart"
import { useAuthStore } from "@/store/useAuthStore"
import { cn } from "@/lib/utils"

interface HeaderProps {
  onSearch: (query: string) => void
  isSearching: boolean
}

export function Header({ onSearch, isSearching }: HeaderProps) {
  const [cartOpen, setCartOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const { data: cart } = useCart()
  const itemCount = cart?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    onScroll()
    window.addEventListener("scroll", onScroll, { passive: true })
    return () => window.removeEventListener("scroll", onScroll)
  }, [])

  return (
    <header
      className={cn(
        "nav-bar sticky top-0 z-30 border-b border-border bg-surface/95 backdrop-blur-md transition-shadow duration-300",
        scrolled && "shadow-[0_8px_24px_-18px_rgba(20,21,26,0.45)]"
      )}
    >
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-3 gap-y-3 px-4 py-3 md:gap-x-5 md:px-6">
        <Link
          to="/"
          className="order-1 flex items-center gap-2.5 rounded-xl pr-1 transition-opacity hover:opacity-80"
        >
          <span className="flex size-9 items-center justify-center rounded-xl bg-brand text-[0.95rem] font-display font-bold text-white shadow-[0_6px_16px_-6px_rgba(91,79,224,0.7)]">
            S
          </span>
          <span className="font-display hidden text-[1.05rem] font-semibold leading-none tracking-tight text-ink sm:inline">
            smart<span className="text-brand">commerce</span>
          </span>
        </Link>

        <nav className="order-2 ml-auto flex shrink-0 items-center gap-2 md:order-3 md:ml-0">
          <div className="flex items-center rounded-full border border-border bg-bg p-0.5">
            <NavIconButton className="hidden sm:flex" label="Wishlist">
              <Heart className="size-[1.15rem]" />
            </NavIconButton>

            <NavIconButton label="Cart" onClick={() => setCartOpen(true)}>
              <ShoppingBag className="size-[1.15rem]" />
              {itemCount > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-semibold leading-none text-ink ring-2 ring-surface">
                  {itemCount > 99 ? "99+" : itemCount}
                </span>
              )}
            </NavIconButton>
          </div>

          <AccountMenu />
        </nav>

        <div className="order-3 w-full min-w-0 md:order-2 md:w-auto md:flex-1">
          <AiSearchBar onSearch={onSearch} isThinking={isSearching} />
        </div>
      </div>

      <CartSheet open={cartOpen} onOpenChange={setCartOpen} />
    </header>
  )
}

function NavIconButton({
  label,
  onClick,
  className,
  children,
}: {
  label: string
  onClick?: () => void
  className?: string
  children: ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className={cn(
        "relative flex size-11 items-center justify-center rounded-full text-ink-muted transition-all duration-200 hover:bg-brand-dim hover:text-brand active:scale-95",
        className
      )}
    >
      {children}
    </button>
  )
}

function AccountMenu() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const clearSession = useAuthStore((s) => s.clearSession)
  const hasRole = useAuthStore((s) => s.hasRole)
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const isVendor = hasRole("VENDOR") || hasRole("ROLE_VENDOR")
  const isAdmin = hasRole("ADMIN") || hasRole("ROLE_ADMIN")

  useEffect(() => {
    function onPointerDown(event: MouseEvent) {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false)
    }
    document.addEventListener("mousedown", onPointerDown)
    document.addEventListener("keydown", onKey)
    return () => {
      document.removeEventListener("mousedown", onPointerDown)
      document.removeEventListener("keydown", onKey)
    }
  }, [])

  function handleLogout() {
    setOpen(false)
    clearSession()
    navigate("/")
  }

  const initials = user?.email?.slice(0, 1).toUpperCase() ?? "U"

  return (
    <div ref={menuRef} className="relative ml-1">
      {user ? (
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          aria-haspopup="menu"
          className="flex h-11 items-center gap-2 rounded-full border border-border bg-surface pl-1 pr-2.5 text-sm font-medium text-ink transition-all hover:border-brand/30 hover:bg-brand-dim/50"
        >
          <span className="flex size-8 items-center justify-center rounded-full bg-brand-dim font-display text-xs font-semibold text-brand">
            {initials}
          </span>
          <span className="hidden max-w-[8.5rem] truncate sm:inline">{user.email}</span>
          <ChevronDown className={cn("size-3.5 text-ink-muted transition-transform", open && "rotate-180")} />
        </button>
      ) : (
        <div className="flex items-center rounded-full border border-border bg-surface p-0.5">
          <Link
            to="/register"
            className="hidden h-10 items-center rounded-full px-3.5 text-sm font-medium text-ink transition-colors hover:bg-bg md:inline-flex"
          >
            Join
          </Link>
          <Link
            to="/login"
            className="inline-flex h-10 items-center rounded-full bg-brand px-4 text-sm font-semibold text-white transition-colors hover:bg-brand-hover"
          >
            Sign in
          </Link>
        </div>
      )}

      {open && user && (
        <div
          role="menu"
          className="absolute right-0 top-full z-40 mt-2 w-56 overflow-hidden rounded-2xl border border-border bg-surface py-1.5 shadow-lg"
        >
          <div className="border-b border-border px-3.5 py-2.5">
            <p className="text-xs text-ink-muted">Signed in as</p>
            <p className="mt-0.5 truncate text-sm font-medium text-ink">{user.email}</p>
          </div>
          <MenuLink to="/" onClick={() => setOpen(false)} icon={<Package className="size-4" />}>
            My orders
          </MenuLink>
          <MenuLink to="/" onClick={() => setOpen(false)} icon={<UserRound className="size-4" />}>
            Profile
          </MenuLink>
          {(isVendor || isAdmin) && (
            <MenuLink
              to="/"
              onClick={() => setOpen(false)}
              icon={<LayoutDashboard className="size-4" />}
            >
              Dashboard
            </MenuLink>
          )}
          <button
            type="button"
            role="menuitem"
            onClick={handleLogout}
            className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-left text-sm text-ink-muted transition-colors hover:bg-bg hover:text-ink"
          >
            <LogOut className="size-4" />
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

function MenuLink({
  to,
  onClick,
  icon,
  children,
}: {
  to: string
  onClick: () => void
  icon: ReactNode
  children: ReactNode
}) {
  return (
    <Link
      to={to}
      role="menuitem"
      onClick={onClick}
      className="flex items-center gap-2.5 px-3.5 py-2.5 text-sm text-ink transition-colors hover:bg-bg"
    >
      <span className="text-ink-muted">{icon}</span>
      {children}
    </Link>
  )
}
