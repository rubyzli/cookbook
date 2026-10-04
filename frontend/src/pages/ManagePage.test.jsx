import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, sentRequests } from '../test/utils.jsx'

const dessert = { id: 'c1', name: 'Dessert', recipeCount: 2 }
const breakfast = { id: 'c2', name: 'Breakfast', recipeCount: 0 }
const flour = { id: 'i1', name: 'Flour', recipeCount: 3 }
const saffron = { id: 'i2', name: 'Saffron', recipeCount: 0 }

function routes(extra = {}) {
  return {
    // Sorted by the server
    '/api/categories': [breakfast, dessert],
    '/api/ingredients': [flour, saffron],
    ...extra,
  }
}

const section = (name) => screen.getByRole('region', { name })
const row = (sectionName, itemName) =>
  within(section(sectionName))
    .getAllByRole('listitem')
    .find((item) => within(item).queryByText(itemName, { selector: '.name' }))

describe('Categories & ingredients page', () => {
  it('is linked from the header', async () => {
    mockApi(routes({ '/api/recipes': [] }))
    renderApp('/')

    await userEvent.click(screen.getByRole('link', { name: 'Categories & ingredients' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Categories & ingredients' })).toBeInTheDocument()
  })

  it('lists both kinds sorted by name with usage', async () => {
    mockApi(routes())
    renderApp('/manage')

    await screen.findByText('Breakfast')
    const categoryNames = within(section('Categories')).getAllByText(/^(Breakfast|Dessert)$/)
    expect(categoryNames.map((element) => element.textContent)).toEqual(['Breakfast', 'Dessert'])
    expect(within(row('Categories', 'Dessert')).getByRole('link', { name: 'Used in 2 recipes' })).toHaveAttribute(
      'href',
      '/?categoryId=c1',
    )
    expect(within(row('Categories', 'Breakfast')).getByText('Not used yet')).toBeInTheDocument()
    // Ingredients can't filter the recipe list, so their usage is plain text
    expect(within(row('Ingredients', 'Flour')).getByText('Used in 3 recipes').tagName).toBe('SPAN')
  })

  it('filters a list', async () => {
    mockApi(routes())
    renderApp('/manage')
    await screen.findByText('Flour')

    await userEvent.type(screen.getByRole('searchbox', { name: 'Filter ingredients' }), 'saf')

    expect(within(section('Ingredients')).queryByText('Flour')).not.toBeInTheDocument()
    expect(within(section('Ingredients')).getByText('Saffron')).toBeInTheDocument()

    await userEvent.clear(screen.getByRole('searchbox', { name: 'Filter ingredients' }))
    await userEvent.type(screen.getByRole('searchbox', { name: 'Filter ingredients' }), 'xyz')
    expect(within(section('Ingredients')).getByText('Nothing matches “xyz”.')).toBeInTheDocument()
  })

  it('adds an item', async () => {
    const fetchMock = mockApi(routes({ 'POST /api/ingredients': ({ body }) => ({ id: 'i3', name: body.name }) }))
    renderApp('/manage')
    await screen.findByText('Flour')

    await userEvent.type(within(section('Ingredients')).getByRole('textbox', { name: 'New ingredient' }), ' Cumin {enter}')

    await waitFor(() =>
      expect(sentRequests(fetchMock)).toEqual([{ method: 'POST', url: '/api/ingredients', body: { name: 'Cumin', language: 'en' } }]),
    )
    await waitFor(() =>
      expect(within(section('Ingredients')).getByRole('textbox', { name: 'New ingredient' })).toHaveValue(''),
    )
  })

  it('shows add errors: blank, and a name that exists', async () => {
    mockApi(routes({ 'POST /api/categories': { status: 409, body: { detail: 'Category already exists' } } }))
    renderApp('/manage')
    await screen.findByText('Dessert')
    const input = within(section('Categories')).getByRole('textbox', { name: 'New category' })

    await userEvent.click(within(section('Categories')).getByRole('button', { name: 'Add' }))
    expect(input).toHaveAccessibleDescription('Enter a name.')

    await userEvent.type(input, 'dessert{enter}')
    await waitFor(() => expect(input).toHaveAccessibleDescription('A category with this name already exists.'))
  })

  it('renames an item', async () => {
    const fetchMock = mockApi(routes({ 'PUT /api/categories/c1': ({ body }) => ({ id: 'c1', name: body.name }) }))
    renderApp('/manage')
    await screen.findByText('Dessert')

    await userEvent.click(within(row('Categories', 'Dessert')).getByRole('button', { name: 'Rename' }))
    const input = screen.getByRole('textbox', { name: 'New name for Dessert' })
    expect(input).toHaveFocus()
    await userEvent.clear(input)
    await userEvent.type(input, 'Desserts{enter}')

    await waitFor(() =>
      expect(sentRequests(fetchMock)).toEqual([{ method: 'PUT', url: '/api/categories/c1', body: { name: 'Desserts' } }]),
    )
    await waitFor(() => expect(screen.queryByRole('textbox', { name: 'New name for Dessert' })).not.toBeInTheDocument())
  })

  it('cancels a rename with Escape without sending anything', async () => {
    const fetchMock = mockApi(routes())
    renderApp('/manage')
    await screen.findByText('Dessert')

    await userEvent.click(within(row('Categories', 'Dessert')).getByRole('button', { name: 'Rename' }))
    await userEvent.type(screen.getByRole('textbox', { name: 'New name for Dessert' }), 'x{Escape}')

    expect(screen.queryByRole('textbox', { name: 'New name for Dessert' })).not.toBeInTheDocument()
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('shows a rename conflict next to the field', async () => {
    mockApi(routes({ 'PUT /api/ingredients/i2': { status: 409, body: { detail: 'Ingredient already exists' } } }))
    renderApp('/manage')
    await screen.findByText('Saffron')

    await userEvent.click(within(row('Ingredients', 'Saffron')).getByRole('button', { name: 'Rename' }))
    const input = screen.getByRole('textbox', { name: 'New name for Saffron' })
    await userEvent.clear(input)
    await userEvent.type(input, 'Flour{enter}')

    await waitFor(() => expect(input).toHaveAccessibleDescription('An ingredient with this name already exists.'))
  })

  it('clears a rename error once the rename succeeds', async () => {
    let attempts = 0
    mockApi(
      routes({
        'PUT /api/ingredients/i2': ({ body }) =>
          ++attempts === 1
            ? { status: 409, body: { detail: 'Ingredient already exists' } }
            : { id: 'i2', name: body.name },
      }),
    )
    renderApp('/manage')
    await screen.findByText('Saffron')

    await userEvent.click(within(row('Ingredients', 'Saffron')).getByRole('button', { name: 'Rename' }))
    const input = screen.getByRole('textbox', { name: 'New name for Saffron' })
    await userEvent.clear(input)
    await userEvent.type(input, 'Flour{enter}')
    await screen.findByText('An ingredient with this name already exists.')
    await userEvent.clear(input)
    await userEvent.type(input, 'Saffron threads{enter}')

    await waitFor(() => expect(screen.queryByRole('textbox', { name: 'New name for Saffron' })).not.toBeInTheDocument())
    expect(screen.queryByText('An ingredient with this name already exists.')).not.toBeInTheDocument()
  })

  it('deletes a used category after saying how many recipes lose it', async () => {
    const fetchMock = mockApi(routes({ 'DELETE /api/categories/c1': { status: 204 } }))
    renderApp('/manage')
    await screen.findByText('Dessert')

    await userEvent.click(screen.getByRole('button', { name: 'Delete Dessert' }))
    const confirm = screen.getByRole('group', { name: 'Confirm delete' })
    expect(confirm).toHaveTextContent('Delete “Dessert”? It will be removed from 2 recipes.')
    await userEvent.click(within(confirm).getByRole('button', { name: 'Yes, delete' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock)).toEqual([{ method: 'DELETE', url: '/api/categories/c1', body: undefined }]),
    )
  })

  it('deletes an unused ingredient after a plain confirmation', async () => {
    const fetchMock = mockApi(routes({ 'DELETE /api/ingredients/i2': { status: 204 } }))
    renderApp('/manage')
    await screen.findByText('Saffron')

    await userEvent.click(screen.getByRole('button', { name: 'Delete Saffron' }))
    expect(screen.getByRole('group', { name: 'Confirm delete' })).toHaveTextContent('Delete “Saffron”?')
    await userEvent.click(screen.getByRole('button', { name: 'Yes, delete' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(1))
  })

  it('explains instead of deleting an ingredient that recipes use', async () => {
    const fetchMock = mockApi(routes())
    renderApp('/manage')
    await screen.findByText('Flour')

    await userEvent.click(screen.getByRole('button', { name: 'Delete Flour' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Used in recipes, so it can’t be deleted.')
    expect(screen.queryByRole('button', { name: 'Yes, delete' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'OK' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('explains when an ingredient became used after the page loaded', async () => {
    mockApi(routes({ 'DELETE /api/ingredients/i2': { status: 409, body: { detail: 'Ingredient is used by 1 recipe' } } }))
    renderApp('/manage')
    await screen.findByText('Saffron')

    await userEvent.click(screen.getByRole('button', { name: 'Delete Saffron' }))
    await userEvent.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Used in recipes, so it can’t be deleted.')
  })

  it('works in Hungarian', async () => {
    mockApi(routes())
    renderApp('/manage', { language: 'hu' })

    expect(await screen.findByRole('heading', { level: 1, name: 'Kategóriák és hozzávalók' })).toBeInTheDocument()
    await screen.findByText('Flour')
    expect(within(row('Hozzávalók', 'Flour')).getByText('3 receptben szerepel')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Dessert törlése' }))
    expect(screen.getByRole('group', { name: 'Törlés megerősítése' })).toHaveTextContent(
      'Törlöd ezt: „Dessert”? 2 receptből kikerül.',
    )
  })
})
