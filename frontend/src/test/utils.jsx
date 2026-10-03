import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { vi } from 'vitest'
import App from '../App.jsx'

// Renders the whole app at `route`. Each test gets a fresh cache and no retries.
// The current URL is exposed in a hidden element so tests can check it.
export function renderApp(route = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <App />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location" hidden>{location.pathname + location.search}</output>
}

// Stubs fetch. `routes` maps "METHOD /path?query" (or just "/path" for GET) to a response:
// a JSON body, { status, body }, or a function receiving { method, url, body } and returning either.
// Unknown routes return 404.
export function mockApi(routes) {
  const fetchMock = vi.fn(async (url, options = {}) => {
    const method = options.method ?? 'GET'
    let route = routes[`${method} ${url}`] ?? (method === 'GET' ? routes[url] : undefined)
    if (typeof route === 'function') {
      route = route({ method, url, body: options.body ? JSON.parse(options.body) : undefined })
    }
    if (route === undefined) {
      return jsonResponse(404, { status: 404, detail: 'Not found' })
    }
    if (route.status === 204) {
      return new Response(null, { status: 204 })
    }
    return route.status ? jsonResponse(route.status, route.body) : jsonResponse(200, route)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function jsonResponse(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function requestedUrls(fetchMock) {
  return fetchMock.mock.calls.map(([url]) => url)
}

// Requests other than GETs, as { method, url, body }, in the order they were sent
export function sentRequests(fetchMock) {
  return fetchMock.mock.calls
    .filter(([, options]) => options?.method && options.method !== 'GET')
    .map(([url, options]) => ({
      method: options.method,
      url,
      body: options.body ? JSON.parse(options.body) : undefined,
    }))
}
