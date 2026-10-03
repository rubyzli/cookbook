import { describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet, apiSend, NETWORK_ERROR } from './client.js'

function stubFetch(response) {
  const fetchMock = vi.fn(async () => response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('apiGet', () => {
  it('returns the parsed JSON body', async () => {
    stubFetch(Response.json([{ id: '1', name: 'Pie' }]))

    await expect(apiGet('/api/recipes')).resolves.toEqual([{ id: '1', name: 'Pie' }])
  })

  it('adds non-empty params to the query string', async () => {
    const fetchMock = stubFetch(Response.json([]))

    await apiGet('/api/recipes', { search: 'apple pie', categoryId: '', other: null })

    expect(fetchMock).toHaveBeenCalledWith('/api/recipes?search=apple+pie', expect.anything())
  })

  it('throws an ApiError with the problem detail and field errors', async () => {
    stubFetch(
      Response.json(
        { status: 400, detail: 'Validation failed', errors: { name: 'must not be blank' } },
        { status: 400 },
      ),
    )

    const error = await apiGet('/api/recipes').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.errors).toEqual({ name: 'must not be blank' })
  })

  it('throws an ApiError with a generic message when the error body is not JSON', async () => {
    stubFetch(new Response('oops', { status: 500 }))

    const error = await apiGet('/api/recipes').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(500)
    expect(error.message).toBe('Request failed with status 500')
    expect(error.errors).toEqual({})
  })

  it('says the backend is not responding when the dev proxy returns a plain 502', async () => {
    stubFetch(new Response('', { status: 502, headers: { 'Content-Type': 'text/plain' } }))

    const error = await apiGet('/api/recipes').catch((e) => e)

    expect(error.status).toBe(502)
    expect(error.kind).toBe('gateway')
    expect(error.message).toBe('The backend isn’t responding. Check that it’s running on port 8080.')
  })

  it('says the server cannot be reached when fetch itself fails', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))

    const error = await apiGet('/api/recipes').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(NETWORK_ERROR)
    expect(error.message).toBe('Can’t reach the server. Check that the app is running.')
  })
})

describe('apiSend', () => {
  it('says the server cannot be reached when fetch itself fails', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))

    const error = await apiSend('POST', '/api/categories', { name: 'Test' }).catch((e) => e)

    expect(error.status).toBe(NETWORK_ERROR)
    expect(error.kind).toBe('network')
    expect(error.message).toBe('Can’t reach the server. Check that the app is running.')
  })

  it('returns null for 204 No Content', async () => {
    stubFetch(new Response(null, { status: 204 }))

    await expect(apiSend('DELETE', '/api/recipes/r1')).resolves.toBeNull()
  })
})
