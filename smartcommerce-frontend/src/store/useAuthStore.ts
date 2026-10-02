import { create } from "zustand"
import { persist } from "zustand/middleware"
import type { AuthResponse, AuthUser } from "@/types/auth"

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  setSession: (auth: AuthResponse) => void
  updateAccessToken: (token: string) => void
  clearSession: () => void
  isAuthenticated: () => boolean
  hasRole: (role: string) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (auth) =>
        set({
          accessToken: auth.token,
          refreshToken: auth.refreshToken,
          user: { userId: auth.userId, email: auth.email, roles: auth.roles },
        }),
      updateAccessToken: (token) => set({ accessToken: token }),
      clearSession: () => set({ accessToken: null, refreshToken: null, user: null }),
      isAuthenticated: () => Boolean(get().accessToken),
      hasRole: (role) => get().user?.roles.includes(role) ?? false,
    }),
    {
      name: "smartcommerce-auth",
      // NOTE: persisting the refresh token client-side (even in storage, not just
      // memory) is a pragmatic tradeoff for local development — it means a page
      // reload doesn't log you out. The more secure long-term design is to have
      // auth-service set the refresh token as an httpOnly cookie instead, so it's
      // never reachable from JS at all. Revisit before this goes anywhere public.
    }
  )
)
