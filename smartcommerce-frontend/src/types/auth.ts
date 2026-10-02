// Mirrors auth-service's AuthResponse/LoginRequest/RegisterRequest exactly —
// keep these in sync if the backend DTOs change.

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  refreshToken: string
  tokenType: string // "Bearer"
  userId: number
  email: string
  roles: string[]
}

export interface AuthUser {
  userId: number
  email: string
  roles: string[]
}
