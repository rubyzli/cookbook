import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { mockApi, renderApp, sentRequests } from '../test/utils.jsx'

const dessert = { id: 'c1', name: 'Dessert' }
const baking = { id: 'c2', name: 'Baking' }
const flour = { id: 'i1', name: 'Flour' }
const butter = { id: 'i2', name: 'Butter' }

const applePie = {
  id: 'r1',
  name: 'Apple Pie',
  description: 'Classic',
  servings: 8,
  prepTimeMinutes: 30,
  cookTimeMinutes: 45,
  instructions: 'Mix.\nBake.',
  imageUrl: null,
  createdBy: null,
  createdAt: '2026-10-03T12:00:00Z',
  categories: [dessert],
  ingredients: [
    { ingredientId: 'i1', name: 'Flour', amount: 250, unit: 'g' },
    { ingredientId: 'i2', name: 'Butter', amount: 125, unit: 'g' },
  ],
}

// Lists and the GET after save; individual tests add the write routes they need
function baseRoutes(extra = {}) {
  return {
    '/api/categories': [dessert, baking],
    '/api/ingredients': [flour, butter],
    '/api/recipes/r1': applePie,
    '/api/recipes': [applePie],
    ...extra,
  }
}

// By label rather than role: inputs with suggestions (list=) are comboboxes, the rest textboxes
const field = (label) => screen.getByLabelText(label)

describe('New recipe', () => {
  it('is reachable from the list page', async () => {
    mockApi(baseRoutes())
    renderApp('/')

    await userEvent.click(await screen.findByRole('link', { name: '+ New recipe' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'New recipe' })).toBeInTheDocument()
  })

  it('creates new ingredients, then the recipe, and opens it', async () => {
    const fetchMock = mockApi(
      baseRoutes({
        'POST /api/ingredients': ({ body }) => ({ id: 'i-new', name: body.name }),
        'POST /api/recipes': ({ body }) => ({ ...applePie, ...body, id: 'r-new', categories: [], ingredients: [] }),
        '/api/recipes/r-new': { ...applePie, id: 'r-new', name: 'Cinnamon Rolls' },
      }),
    )
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('Name'), 'Cinnamon Rolls')
    await userEvent.type(field('Servings'), '12')
    await userEvent.type(field('Cook time (min)'), '25')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Baking' }))
    await userEvent.type(field('Amount 1'), '500')
    await userEvent.type(field('Unit 1'), 'g')
    await userEvent.type(field('Ingredient 1'), 'flour')
    await userEvent.click(screen.getByRole('button', { name: '+ Add ingredient' }))
    await userEvent.type(field('Amount 2'), '2')
    await userEvent.type(field('Unit 2'), 'tbsp')
    await userEvent.type(field('Ingredient 2'), 'Cinnamon')
    await userEvent.type(field('Steps'), 'Make the dough.{enter}Roll and bake.')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/recipes/r-new'))
    expect(sentRequests(fetchMock)).toEqual([
      { method: 'POST', url: '/api/ingredients', body: { name: 'Cinnamon' } },
      {
        method: 'POST',
        url: '/api/recipes',
        body: {
          name: 'Cinnamon Rolls',
          description: null,
          servings: 12,
          prepTimeMinutes: null,
          cookTimeMinutes: 25,
          instructions: 'Make the dough.\nRoll and bake.',
          imageUrl: null,
          categoryIds: ['c2'],
          ingredients: [
            { ingredientId: 'i1', amount: 500, unit: 'g' },
            { ingredientId: 'i-new', amount: 2, unit: 'tbsp' },
          ],
        },
      },
    ])
  })

  it('shows validation errors without sending anything and focuses the first one', async () => {
    const fetchMock = mockApi(baseRoutes())
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('Servings'), 'lots')
    await userEvent.type(field('Amount 1'), '2')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Please fix the highlighted fields.')
    expect(field('Name')).toHaveAccessibleDescription('Give the recipe a name.')
    expect(field('Servings')).toHaveAccessibleDescription('Use a whole number, 1 or more.')
    expect(field('Ingredient 1')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Enter an ingredient.')).toBeInTheDocument()
    await waitFor(() => expect(field('Name')).toHaveFocus())
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('shows a duplicate name under the name field', async () => {
    mockApi(
      baseRoutes({
        'POST /api/recipes': { status: 409, body: { status: 409, detail: 'Recipe already exists' } },
      }),
    )
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('Name'), 'Apple Pie')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    await waitFor(() =>
      expect(field('Name')).toHaveAccessibleDescription('A recipe with this name already exists.'),
    )
    expect(screen.getByTestId('location')).toHaveTextContent('/recipes/new')
  })

  it('puts server validation errors on the matching ingredient row', async () => {
    mockApi(
      baseRoutes({
        'POST /api/recipes': {
          status: 400,
          body: { detail: 'Validation failed', errors: { 'ingredients[1].unit': 'size must be between 0 and 50' } },
        },
      }),
    )
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('Name'), 'Pie')
    await userEvent.type(field('Ingredient 1'), 'Flour')
    await userEvent.click(screen.getByRole('button', { name: '+ Add ingredient' }))
    await userEvent.type(field('Ingredient 2'), 'Butter')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    expect(await screen.findByText('size must be between 0 and 50')).toBeInTheDocument()
    expect(field('Unit 2')).toHaveAttribute('aria-invalid', 'true')
    expect(field('Unit 1')).not.toHaveAttribute('aria-invalid')
  })

  it('shows other failures as a form error and lets you try again', async () => {
    mockApi(baseRoutes({ 'POST /api/recipes': { status: 500, body: { detail: 'Internal Server Error' } } }))
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('Name'), 'Pie')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t save the recipe: Internal Server Error')
    expect(screen.getByRole('button', { name: 'Create recipe' })).toBeEnabled()
  })

  it('adds a new category inline and selects it', async () => {
    const categories = [dessert]
    const fetchMock = mockApi(
      baseRoutes({
        '/api/categories': () => categories,
        'POST /api/categories': ({ body }) => {
          const created = { id: 'c-new', name: body.name }
          categories.push(created)
          return created
        },
      }),
    )
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('New category'), 'Breakfast{enter}')

    expect(await screen.findByRole('checkbox', { name: 'Breakfast' })).toBeChecked()
    expect(field('New category')).toHaveValue('')
    expect(sentRequests(fetchMock)).toEqual([
      { method: 'POST', url: '/api/categories', body: { name: 'Breakfast' } },
    ])
  })

  it('shows a category load error without also saying there are none', async () => {
    // First load succeeds with no categories; the refetch after adding one fails. The cache still
    // holds the old empty list, which used to show "No categories yet" next to the error.
    let categoryLoads = 0
    mockApi(
      baseRoutes({
        '/api/categories': () =>
          ++categoryLoads === 1 ? [] : { status: 500, body: { detail: 'Internal Server Error' } },
        'POST /api/categories': ({ body }) => ({ id: 'c-new', name: body.name }),
      }),
    )
    renderApp('/recipes/new')
    await screen.findByText('No categories yet. Add one below.')

    await userEvent.type(field('New category'), 'Test{enter}')

    expect(await screen.findByText('Couldn’t load categories: Internal Server Error')).toBeInTheDocument()
    expect(screen.queryByText('No categories yet. Add one below.')).not.toBeInTheDocument()
  })

  it('shows a clear message when adding a category cannot reach the server', async () => {
    mockApi(baseRoutes({ '/api/categories': [] }))
    renderApp('/recipes/new')
    await screen.findByText('No categories yet. Add one below.')
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))

    await userEvent.type(field('New category'), 'Test{enter}')

    expect(
      await screen.findByText('Couldn’t add “Test”: Can’t reach the server. Check that the app is running.'),
    ).toBeInTheDocument()
  })

  it('selects an existing category instead of creating a duplicate', async () => {
    const fetchMock = mockApi(baseRoutes())
    renderApp('/recipes/new')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.type(field('New category'), 'dessert')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(screen.getByRole('checkbox', { name: 'Dessert' })).toBeChecked()
    expect(sentRequests(fetchMock)).toEqual([])
  })
})

describe('Edit recipe', () => {
  it('is reachable from the detail page', async () => {
    mockApi(baseRoutes())
    renderApp('/recipes/r1')

    await userEvent.click(await screen.findByRole('link', { name: 'Edit' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Edit Apple Pie' })).toBeInTheDocument()
  })

  it('starts with the recipe’s current values', async () => {
    mockApi(baseRoutes())
    renderApp('/recipes/r1/edit')

    await screen.findByRole('heading', { level: 1, name: 'Edit Apple Pie' })
    expect(field('Name')).toHaveValue('Apple Pie')
    expect(field('Servings')).toHaveValue('8')
    expect(field('Steps')).toHaveValue('Mix.\nBake.')
    expect(field('Ingredient 1')).toHaveValue('Flour')
    expect(field('Amount 1')).toHaveValue('250')
    expect(field('Ingredient 2')).toHaveValue('Butter')
    expect(await screen.findByRole('checkbox', { name: 'Dessert' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Baking' })).not.toBeChecked()
  })

  it('saves changes with PUT and returns to the recipe', async () => {
    const fetchMock = mockApi(baseRoutes({ 'PUT /api/recipes/r1': ({ body }) => ({ ...applePie, ...body }) }))
    renderApp('/recipes/r1/edit')
    await screen.findByRole('checkbox', { name: 'Dessert' })

    await userEvent.clear(field('Servings'))
    await userEvent.type(field('Servings'), '10')
    await userEvent.click(screen.getByRole('checkbox', { name: 'Dessert' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Baking' }))
    await userEvent.click(screen.getByRole('button', { name: 'Move ingredient 2 up' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/recipes\/r1$/))
    const [put] = sentRequests(fetchMock)
    expect(put.method).toBe('PUT')
    expect(put.url).toBe('/api/recipes/r1')
    expect(put.body).toMatchObject({
      name: 'Apple Pie',
      servings: 10,
      categoryIds: ['c2'],
      ingredients: [
        { ingredientId: 'i2', amount: 125, unit: 'g' },
        { ingredientId: 'i1', amount: 250, unit: 'g' },
      ],
    })
  })

  it('removes an ingredient row', async () => {
    mockApi(baseRoutes())
    renderApp('/recipes/r1/edit')
    await screen.findByRole('heading', { level: 1, name: 'Edit Apple Pie' })

    await userEvent.click(screen.getByRole('button', { name: 'Remove ingredient 1' }))

    const rows = within(screen.getByRole('group', { name: 'Ingredients' })).getAllByRole('listitem')
    expect(rows).toHaveLength(1)
    expect(field('Ingredient 1')).toHaveValue('Butter')
  })

  it('keeps one empty row when the last ingredient is removed', async () => {
    mockApi(baseRoutes({ '/api/recipes/r1': { ...applePie, ingredients: [applePie.ingredients[0]] } }))
    renderApp('/recipes/r1/edit')
    await screen.findByRole('heading', { level: 1, name: 'Edit Apple Pie' })

    await userEvent.click(screen.getByRole('button', { name: 'Remove ingredient 1' }))

    expect(field('Ingredient 1')).toHaveValue('')
  })

  it('says when the recipe does not exist', async () => {
    mockApi(baseRoutes())

    renderApp('/recipes/missing/edit')

    expect(await screen.findByRole('alert')).toHaveTextContent('This recipe doesn’t exist.')
  })
})
