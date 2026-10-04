import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, sentRequests } from '../test/utils.jsx'

const applePie = {
  id: 'r1',
  name: 'Apple Pie',
  description: "Grandma's recipe",
  servings: 8,
  prepTimeMinutes: 30,
  cookTimeMinutes: 45,
  instructions: 'Make the dough.\nBake for 45 minutes.',
  imageUrl: null,
  createdBy: null,
  createdAt: '2026-10-03T12:00:00Z',
  categories: [{ id: 'c1', name: 'Dessert' }],
  ingredients: [
    { ingredientId: 'i1', name: 'Flour', amount: 250, unit: 'g' },
    { ingredientId: 'i2', name: 'Butter', amount: 125, unit: 'g' },
    { ingredientId: 'i3', name: 'Salt', amount: null, unit: null },
    { ingredientId: 'i2', name: 'Butter', amount: 1, unit: 'tbsp' },
  ],
}

describe('RecipeDetailPage', () => {
  it('shows the recipe with times, categories, ingredients and instructions', async () => {
    mockApi({ '/api/recipes/r1': applePie })

    renderApp('/recipes/r1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })).toBeInTheDocument()
    expect(screen.getByText("Grandma's recipe")).toBeInTheDocument()
    expect(screen.getByText('1 h 15 min')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dessert' })).toHaveAttribute('href', '/?categoryId=c1')

    const ingredients = within(screen.getByRole('region', { name: 'Ingredients' })).getAllByRole('listitem')
    expect(ingredients.map((item) => item.textContent)).toEqual([
      '250 g Flour',
      '125 g Butter',
      ' Salt',
      '1 tbsp Butter',
    ])

    const steps = within(screen.getByRole('region', { name: 'Instructions' })).getAllByRole('listitem')
    expect(steps.map((step) => step.textContent)).toEqual(['Make the dough.', 'Bake for 45 minutes.'])
  })

  it('shows single-line instructions as a paragraph and skips blank lines', async () => {
    mockApi({ '/api/recipes/r1': { ...applePie, instructions: '\n  Bake it.  \n\n' } })

    renderApp('/recipes/r1')

    const instructions = await screen.findByRole('region', { name: 'Instructions' })
    expect(within(instructions).queryByRole('list')).not.toBeInTheDocument()
    expect(within(instructions).getByText('Bake it.')).toBeInTheDocument()
  })

  it('shows the photo only when the recipe has one', async () => {
    mockApi({ '/api/recipes/r1': { ...applePie, imageUrl: 'https://example.com/pie.jpg' } })

    const { container } = renderApp('/recipes/r1')

    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })
    expect(container.querySelector('img')).toHaveAttribute('src', 'https://example.com/pie.jpg')
  })

  it('shows no placeholder tile when there is no photo', async () => {
    mockApi({ '/api/recipes/r1': applePie })

    const { container } = renderApp('/recipes/r1')

    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })
    expect(container.querySelector('.image-placeholder, img')).toBeNull()
  })

  it('shows grouped ingredients under their headings', async () => {
    mockApi({
      '/api/recipes/r1': {
        ...applePie,
        ingredients: [
          { ingredientId: 'i3', name: 'Salt', amount: null, unit: 'pinch', group: null },
          { ingredientId: 'i1', name: 'Flour', amount: 250, unit: 'g', group: 'For the dough' },
          { ingredientId: 'i2', name: 'Butter', amount: 125, unit: 'g', group: 'For the dough' },
          { ingredientId: 'i2', name: 'Butter', amount: 1, unit: 'tbsp', group: 'For the top' },
        ],
      },
    })

    renderApp('/recipes/r1')

    const region = await screen.findByRole('region', { name: 'Ingredients' })
    expect(within(region).getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual([
      'For the dough',
      'For the top',
    ])
    const lists = within(region).getAllByRole('list')
    expect(lists.map((list) => within(list).getAllByRole('listitem').map((item) => item.textContent))).toEqual([
      ['pinch Salt'],
      ['250 g Flour', '125 g Butter'],
      ['1 tbsp Butter'],
    ])
  })

  it('shows notes as unnumbered paragraphs, and no notes section without them', async () => {
    mockApi({ '/api/recipes/r1': { ...applePie, notes: 'Use tart apples.\n\nBest the next day.' } })
    const { unmount } = renderApp('/recipes/r1')

    const notes = await screen.findByRole('region', { name: 'Notes' })
    expect(within(notes).queryByRole('list')).not.toBeInTheDocument()
    expect(within(notes).getByText('Use tart apples.')).toBeInTheDocument()
    expect(within(notes).getByText('Best the next day.')).toBeInTheDocument()
    unmount()

    mockApi({ '/api/recipes/r1': { ...applePie, notes: null } })
    renderApp('/recipes/r1')
    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })
    expect(screen.queryByRole('region', { name: 'Notes' })).not.toBeInTheDocument()
  })

  it('leaves out the total when only one time is known', async () => {
    mockApi({ '/api/recipes/r1': { ...applePie, prepTimeMinutes: null } })

    renderApp('/recipes/r1')

    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })
    expect(screen.queryByText('Total')).not.toBeInTheDocument()
    expect(screen.getByText('Cook')).toBeInTheDocument()
  })

  it('shows placeholders when ingredients and instructions are missing', async () => {
    mockApi({ '/api/recipes/r1': { ...applePie, ingredients: [], instructions: null } })

    renderApp('/recipes/r1')

    expect(await screen.findByText('No ingredients listed.')).toBeInTheDocument()
    expect(screen.getByText('No instructions yet.')).toBeInTheDocument()
  })

  it('says the recipe does not exist on a 404', async () => {
    mockApi({})

    renderApp('/recipes/missing')

    expect(await screen.findByRole('alert')).toHaveTextContent('This recipe doesn’t exist.')
  })

  it('goes back to the list from the back link', async () => {
    mockApi({ '/api/recipes/r1': applePie, '/api/recipes': [], '/api/categories': [] })
    renderApp('/recipes/r1')
    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })

    await userEvent.click(screen.getByRole('link', { name: '← All recipes' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Recipes' })).toBeInTheDocument()
  })

  it('deletes the recipe after confirmation and returns to the list', async () => {
    const fetchMock = mockApi({
      '/api/recipes/r1': applePie,
      'DELETE /api/recipes/r1': { status: 204 },
      '/api/recipes': [],
      '/api/categories': [],
    })
    renderApp('/recipes/r1')
    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    const confirm = screen.getByRole('group', { name: 'Confirm delete' })
    expect(confirm).toHaveTextContent('Delete “Apple Pie”? This can’t be undone.')
    expect(sentRequests(fetchMock)).toEqual([])

    await userEvent.click(within(confirm).getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Recipes' })).toBeInTheDocument()
    expect(sentRequests(fetchMock)).toEqual([{ method: 'DELETE', url: '/api/recipes/r1', body: undefined }])
  })

  it('cancels a delete without sending anything', async () => {
    const fetchMock = mockApi({ '/api/recipes/r1': applePie })
    renderApp('/recipes/r1')
    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('group', { name: 'Confirm delete' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit' })).toBeInTheDocument()
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('shows an error if the delete fails', async () => {
    mockApi({
      '/api/recipes/r1': applePie,
      'DELETE /api/recipes/r1': { status: 500, body: { detail: 'Internal Server Error' } },
    })
    renderApp('/recipes/r1')
    await screen.findByRole('heading', { level: 1, name: 'Apple Pie' })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t delete: Internal Server Error')
    expect(screen.getByTestId('location')).toHaveTextContent('/recipes/r1')
  })
})
