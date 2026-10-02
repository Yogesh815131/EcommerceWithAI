// Mirrors user-service Address and AddressRequest.

export interface Address {
  id: number
  userId: number
  line1: string
  line2: string | null
  city: string
  state: string
  postalCode: string
  country: string
  isDefault: boolean
}

export interface AddressRequest {
  line1: string
  line2?: string
  city: string
  state: string
  postalCode: string
  country: string
  isDefault?: boolean
}
