// Error thrown for any non-2xx response. The backend sends RFC 9457 problem details,
// so `detail` holds the reason and `errors` maps field names to validation messages.
export class ApiError extends Error {
  constructor(status, detail, errors = {}) {
    super(detail || `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.errors = errors
  }
}

export async function apiGet(path, params = {}) {
  const query = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ).toString()
  const response = await fetch(query ? `${path}?${query}` : path, {
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  return response.json()
}

async function toApiError(response) {
  try {
    const problem = await response.json()
    return new ApiError(response.status, problem.detail, problem.errors)
  } catch {
    // Not JSON, e.g. the Vite proxy's error page when the backend is down
    return new ApiError(response.status)
  }
}
