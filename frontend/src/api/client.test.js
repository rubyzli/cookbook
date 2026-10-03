import { describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet } from './client.js'

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
    stubFetch(new Response('Bad Gateway', { status: 502 }))

    const error = await apiGet('/api/recipes').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(502)
    expect(error.message).toBe('Request failed with status 502')
    expect(error.errors).toEqual({})
  })
})
