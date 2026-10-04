import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, sentRequests } from '../test/utils.jsx'

const recipe = {
  id: 'r1',
  name: 'Pie',
  description: null,
  servings: null,
  prepTimeMinutes: null,
  cookTimeMinutes: null,
  instructions: null,
  notes: null,
  imageUrl: null,
  createdBy: null,
  createdAt: '2026-10-04T12:00:00Z',
  categories: [],
  ingredients: [],
  language: 'en',
  originalLanguage: 'en',
  translationStatus: null,
  translationOutdated: false,
}

function routes(extra = {}) {
  return {
    '/api/categories': [],
    '/api/ingredients': [],
    'POST /api/recipes': ({ body }) => ({ ...recipe, ...body, id: 'r-new' }),
    '/api/recipes/r-new': recipe,
    ...extra,
  }
}

const photo = () => new File(['fake jpeg bytes'], 'pie.jpg', { type: 'image/jpeg' })
// The file input is visually hidden; its label is styled as the button
const fileInput = () => screen.getByLabelText(/^(Upload|Change) photo$/)

describe('photo field', () => {
  it('uploads a chosen photo, shows it, and saves its URL with the recipe', async () => {
    const fetchMock = mockApi(
      routes({ 'POST /api/images': { status: 201, body: { url: '/api/images/3f9a1c7b-0000-4000-8000-000000000001' } } }),
    )
    renderApp('/recipes/new')
    await screen.findByLabelText('Upload photo')

    await userEvent.upload(fileInput(), photo())

    expect(await screen.findByRole('img', { name: 'Photo of the recipe' })).toHaveAttribute(
      'src',
      '/api/images/3f9a1c7b-0000-4000-8000-000000000001',
    )
    expect(screen.getByLabelText('Change photo')).toHaveAttribute('type', 'file')
    const [upload] = sentRequests(fetchMock)
    expect(upload.url).toBe('/api/images')
    expect(upload.body.file.name).toBe('pie.jpg')

    await userEvent.type(screen.getByLabelText('Name'), 'Pie')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))
    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(2))
    expect(sentRequests(fetchMock)[1].body.imageUrl).toBe('/api/images/3f9a1c7b-0000-4000-8000-000000000001')
  })

  it.each([
    [415, 'Only JPEG, PNG, WebP and GIF photos can be uploaded.'],
    [413, 'The photo is too large (at most 15 MB).'],
  ])('explains a %i from the server', async (status, message) => {
    mockApi(routes({ 'POST /api/images': { status, body: { status, detail: 'x' } } }))
    renderApp('/recipes/new')
    await screen.findByLabelText('Upload photo')

    await userEvent.upload(fileInput(), photo())

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.queryByRole('img', { name: 'Photo of the recipe' })).not.toBeInTheDocument()
  })

  it('removes the photo of an existing recipe', async () => {
    const withPhoto = { ...recipe, imageUrl: '/api/images/pie' }
    const fetchMock = mockApi(
      routes({
        '/api/recipes/r1?original=true': withPhoto,
        '/api/recipes/r1': withPhoto,
        'PUT /api/recipes/r1': ({ body }) => ({ ...withPhoto, ...body }),
      }),
    )
    renderApp('/recipes/r1/edit')

    expect(await screen.findByRole('img', { name: 'Photo of the recipe' })).toHaveAttribute('src', '/api/images/pie')
    await userEvent.click(screen.getByRole('button', { name: 'Remove photo' }))
    expect(screen.queryByRole('img', { name: 'Photo of the recipe' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(1))
    expect(sentRequests(fetchMock)[0].body.imageUrl).toBeNull()
  })

  it('still accepts a link to a photo online', async () => {
    const fetchMock = mockApi(routes())
    renderApp('/recipes/new')

    await userEvent.click(await screen.findByRole('button', { name: 'Or link to a photo online' }))
    await userEvent.type(screen.getByLabelText('Photo URL'), 'https://example.com/pie.jpg')
    expect(screen.getByRole('img', { name: 'Photo of the recipe' })).toHaveAttribute('src', 'https://example.com/pie.jpg')
    await userEvent.type(screen.getByLabelText('Name'), 'Pie')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(1))
    expect(sentRequests(fetchMock)[0].body.imageUrl).toBe('https://example.com/pie.jpg')
  })

  it('shows the link field right away for a recipe that links to a photo online', async () => {
    const linked = { ...recipe, imageUrl: 'https://example.com/pie.jpg' }
    mockApi(routes({ '/api/recipes/r1?original=true': linked, '/api/recipes/r1': linked }))
    renderApp('/recipes/r1/edit')

    expect(await screen.findByLabelText('Photo URL')).toHaveValue('https://example.com/pie.jpg')
  })
})
