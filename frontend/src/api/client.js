const JSON_HEADERS = { 'Content-Type': 'application/json' }

// Empty by default, which keeps every call same-origin: that is what both
// `npm run dev` (Vite proxies /api to :8080) and the production build served
// by Spring Boot need. Only a frontend deployed apart from the backend - the
// Vercel demo - sets VITE_API_BASE_URL to the backend's public origin, and
// that origin must be listed in the backend's app.cors.allowed-origins.
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

const apiUrl = (path) => `${API_BASE}${path}`

async function parseOrThrow(response) {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.message || 'Sunucu isteği başarısız oldu')
  }
  return body
}

export async function fetchPricing() {
  const response = await fetch(apiUrl('/api/public/pricing'))
  return parseOrThrow(response)
}

export async function fetchContact() {
  const response = await fetch(apiUrl('/api/public/contact'))
  return parseOrThrow(response)
}

export async function calculateRoute(pickupAddress, dropoffAddress, vehicleType) {
  const response = await fetch(apiUrl('/api/bookings/calculate-route'), {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ pickupAddress, dropoffAddress, vehicleType }),
  })
  return parseOrThrow(response)
}

export async function submitPublicBooking(payload) {
  const response = await fetch(apiUrl('/api/public/bookings'), {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(payload),
  })
  return parseOrThrow(response)
}
