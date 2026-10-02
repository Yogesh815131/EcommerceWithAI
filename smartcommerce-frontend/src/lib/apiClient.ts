import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios"
import { useAuthStore } from "@/store/useAuthStore"

if (!import.meta.env.VITE_API_BASE_URL) {
  // Fail loud: without this, axios silently falls back to same-origin requests
  // (http://localhost:5173/api/...), which Vite's dev server answers with
  // index.html instead of a 404 — leading to confusing "X.map is not a
  // function" errors far from the actual cause.
  console.error(
    "[apiClient] VITE_API_BASE_URL is not set. Did you copy .env.example to .env " +
      "and restart `npm run dev`? Vite only reads .env files at server startup."
  )
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 8000, // don't let a hung service block the UI indefinitely
})

// Attach the access token to every request
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Auto-refresh on 401, retry the original request once
let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const refreshToken = useAuthStore.getState().refreshToken
  if (!refreshToken) throw new Error("No refresh token available")

  const { data } = await axios.post(
    `${import.meta.env.VITE_API_BASE_URL}/api/auth/refresh`,
    { refreshToken }
  )
  useAuthStore.getState().updateAccessToken(data.token)
  return data.token
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        // De-dupe concurrent 401s into a single refresh call
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null
        })
        const newToken = await refreshPromise
        original.headers.Authorization = `Bearer ${newToken}`
        return apiClient(original)
      } catch {
        useAuthStore.getState().clearSession()
        window.location.href = "/login"
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  }
)
