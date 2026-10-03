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

// POST/PUT/DELETE with an optional JSON body. Returns the parsed response, or null for 204 No Content.
export async function apiSend(method, path, body) {
  const response = await fetch(path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  return response.status === 204 ? null : response.json()
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
