import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, requestedUrls } from '../test/utils.jsx'

const dessert = { id: 'c1', name: 'Dessert' }
const italian = { id: 'c2', name: 'Italian' }

const applePie = {
  id: 'r1',
  name: 'Apple Pie',
  description: "Grandma's recipe",
  imageUrl: null,
  servings: 8,
  prepTimeMinutes: 30,
  cookTimeMinutes: 45,
  categories: [dessert],
}
const lasagna = {
  id: 'r2',
  name: 'Lasagna',
  description: null,
  imageUrl: null,
  servings: null,
  prepTimeMinutes: null,
  cookTimeMinutes: 60,
  categories: [italian],
}

describe('RecipeListPage', () => {
  it('shows a card for each recipe with its details', async () => {
    mockApi({ '/api/recipes': [applePie, lasagna], '/api/categories': [italian, dessert] })

    renderApp('/')

    const pie = await screen.findByRole('link', { name: /Apple Pie/ })
    expect(pie).toHaveAttribute('href', '/recipes/r1')
    expect(within(pie).getByText("Grandma's recipe")).toBeInTheDocument()
    expect(within(pie).getByText('30 min')).toBeInTheDocument()
    expect(within(pie).getByText('Dessert')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Lasagna/ })).toHaveAttribute('href', '/recipes/r2')
    expect(screen.getByText('2 recipes')).toBeInTheDocument()
  })

  it('lists categories alphabetically in the filter', async () => {
    mockApi({ '/api/recipes': [], '/api/categories': [italian, dessert] })

    renderApp('/')

    const select = screen.getByRole('combobox', { name: 'Category' })
    await waitFor(() =>
      expect(within(select).getAllByRole('option').map((option) => option.textContent)).toEqual([
        'All categories',
        'Dessert',
        'Italian',
      ]),
    )
  })

  it('filters by category and puts the filter in the URL', async () => {
    const fetchMock = mockApi({
      '/api/recipes': [applePie, lasagna],
      '/api/recipes?categoryId=c2': [lasagna],
      '/api/categories': [dessert, italian],
    })
    renderApp('/')
    await screen.findByRole('link', { name: /Apple Pie/ })

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Category' }), 'Italian')

    await waitFor(() => expect(screen.queryByRole('link', { name: /Apple Pie/ })).not.toBeInTheDocument())
    expect(screen.getByRole('link', { name: /Lasagna/ })).toBeInTheDocument()
    expect(requestedUrls(fetchMock)).toContain('/api/recipes?categoryId=c2')
    expect(screen.getByTestId('location')).toHaveTextContent('/?categoryId=c2')
  })

  it('searches after typing pauses, with a single request', async () => {
    const fetchMock = mockApi({
      '/api/recipes': [applePie, lasagna],
      '/api/recipes?search=las': [lasagna],
      '/api/categories': [],
    })
    renderApp('/')
    await screen.findByRole('link', { name: /Apple Pie/ })

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search recipes' }), 'las')

    await waitFor(() => expect(screen.queryByRole('link', { name: /Apple Pie/ })).not.toBeInTheDocument())
    expect(requestedUrls(fetchMock).filter((url) => url.startsWith('/api/recipes?'))).toEqual([
      '/api/recipes?search=las',
    ])
    expect(screen.getByTestId('location')).toHaveTextContent('/?search=las')
  })

  it('reads search and category from the URL on load', async () => {
    const fetchMock = mockApi({
      '/api/recipes?search=pie&categoryId=c1': [applePie],
      '/api/categories': [dessert],
    })

    renderApp('/?search=pie&categoryId=c1')

    await screen.findByRole('link', { name: /Apple Pie/ })
    expect(screen.getByRole('searchbox', { name: 'Search recipes' })).toHaveValue('pie')
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('c1'))
    expect(requestedUrls(fetchMock)).toContain('/api/recipes?search=pie&categoryId=c1')
  })

  it('clears the search box when the header link resets the URL', async () => {
    mockApi({ '/api/recipes?search=pie': [applePie], '/api/recipes': [applePie, lasagna], '/api/categories': [] })
    renderApp('/?search=pie')
    await screen.findByRole('link', { name: /Apple Pie/ })

    await userEvent.click(screen.getByRole('link', { name: 'Family Cookbook' }))

    await screen.findByRole('link', { name: /Lasagna/ })
    expect(screen.getByRole('searchbox', { name: 'Search recipes' })).toHaveValue('')
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
  })

  it('says when there are no recipes yet', async () => {
    mockApi({ '/api/recipes': [], '/api/categories': [] })

    renderApp('/')

    expect(await screen.findByText('No recipes yet.')).toBeInTheDocument()
  })

  it('says when nothing matches the search', async () => {
    mockApi({ '/api/recipes?search=xyz': [], '/api/categories': [] })

    renderApp('/?search=xyz')

    expect(await screen.findByText('No recipes match your search.')).toBeInTheDocument()
  })

  it('shows an error when the backend fails', async () => {
    mockApi({ '/api/recipes': { status: 500, body: { detail: 'Internal Server Error' } }, '/api/categories': [] })

    renderApp('/')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Couldn’t load recipes.')
    expect(alert).toHaveTextContent('Internal Server Error')
  })

  it('shows the photo, and the letter tile instead when the photo fails to load', async () => {
    mockApi({ '/api/recipes': [{ ...applePie, imageUrl: '/images/pie.jpg' }], '/api/categories': [] })
    const { container } = renderApp('/')
    await screen.findByRole('link', { name: /Apple Pie/ })

    const image = container.querySelector('img')
    expect(image).toHaveAttribute('src', '/images/pie.jpg')

    fireEvent.error(image)

    expect(container.querySelector('img')).toBeNull()
    expect(container.querySelector('.image-placeholder')).toHaveTextContent('A')
  })
})
