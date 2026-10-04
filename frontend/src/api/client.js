const API_BASE_URL = import.meta.env.VITE_API_URL ?? ''

// Error thrown for any non-2xx response. The backend sends RFC 9457 problem details,
// so `detail` holds the reason and `errors` maps field names to validation messages.
// `kind` marks failures the client detected itself ('network', 'gateway') so the UI can
// describe them in the user's language; see i18n/errors.js.
// `code` is the server's reason code where it gives one (e.g. BLOCKED when importing a recipe).
export class ApiError extends Error {
  constructor(status, detail, errors = {}, kind = undefined, code = undefined) {
    super(detail || `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.errors = errors ?? {}
    this.kind = kind
    this.code = code
  }
}

// Status used for requests that never got a response
export const NETWORK_ERROR = 0

// Sent as Accept-Language so server-side validation messages match the page's language.
// Set by I18nProvider.
let language = 'en'

export function setApiLanguage(code) {
  language = code
}

export async function apiGet(path, params = {}) {
  const query = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ).toString()
  const response = await send(query ? `${path}?${query}` : path, {
    headers: { Accept: 'application/json', 'Accept-Language': language },
  })
  return response.json()
}

// POST/PUT/DELETE with an optional JSON body. Returns the parsed response, or null for 204 No Content.
export async function apiSend(method, path, body) {
  const response = await send(path, {
    method,
    headers: {
      Accept: 'application/json',
      'Accept-Language': language,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  return response.status === 204 ? null : response.json()
}

// Uploads one file as multipart/form-data (field "file"); the browser sets the content type
export async function apiUpload(path, file) {
  const body = new FormData()
  body.append('file', file)
  const response = await send(path, {
    method: 'POST',
    headers: { Accept: 'application/json', 'Accept-Language': language },
    body,
  })
  return response.json()
}

async function send(url, init) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${url}`, init)
  } catch {
    throw new ApiError(
        NETWORK_ERROR,
        'Can’t reach the server. Check that the app is running.',
        {},
        'network',
    )
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  return response
}

export function apiUrl(path) {
  if (!path) return path
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${API_BASE_URL}${path}`
}

async function toApiError(response) {
  try {
    const problem = await response.json()
    return new ApiError(response.status, problem.detail, problem.errors, undefined, problem.code)
  } catch {
    // The backend always answers with JSON, so a plain gateway error means a proxy in front of it
    // (Vite's, in development) couldn't reach it
    if ([502, 503, 504].includes(response.status)) {
      return new ApiError(
        response.status,
        'The backend isn’t responding. Check that it’s running on port 8080.',
        {},
        'gateway',
      )
    }
    return new ApiError(response.status)
  }
}
