import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp } from '../test/utils.jsx'

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
})
