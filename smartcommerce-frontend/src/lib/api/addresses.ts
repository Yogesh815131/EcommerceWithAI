import { apiClient } from "@/lib/apiClient"
import type { Address, AddressRequest } from "@/types/address"

interface AddressPayload extends Address {
  default?: boolean
}

function normalizeAddress(raw: AddressPayload): Address {
  return {
    ...raw,
    line2: raw.line2 ?? null,
    isDefault: raw.isDefault ?? raw.default ?? false,
  }
}

export async function fetchAddresses(): Promise<Address[]> {
  const { data } = await apiClient.get<AddressPayload[]>("/api/addresses")
  return data.map(normalizeAddress)
}

export async function createAddress(request: AddressRequest): Promise<Address> {
  const { data } = await apiClient.post<AddressPayload>("/api/addresses", request)
  return normalizeAddress(data)
}
