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

// Stubs fetch. `routes` maps a path (with query string, if any) to either a JSON body
// or a { status, body } response; unknown paths return 404.
export function mockApi(routes) {
  const fetchMock = vi.fn(async (url) => {
    const route = routes[url]
    if (route === undefined) {
      return jsonResponse(404, { status: 404, detail: 'Not found' })
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
