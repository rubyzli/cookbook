import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp } from '../test/utils.jsx'

const pie = {
  id: 'r1',
  name: 'Apple Pie',
  description: null,
  imageUrl: null,
  servings: 8,
  prepTimeMinutes: 30,
  cookTimeMinutes: 45,
  categories: [],
}

describe('language switcher', () => {
  it('switches the page to German and remembers the choice', async () => {
    mockApi({ '/api/recipes': [pie], '/api/categories': [] })
    renderApp('/')
    await screen.findByText('1 recipe')

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'de')

    expect(screen.getByRole('heading', { level: 1, name: 'Rezepte' })).toBeInTheDocument()
    expect(screen.getByText('1 Rezept')).toBeInTheDocument()
    expect(screen.getByText('30 Min.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '+ Neues Rezept' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Sprache' })).toHaveValue('de')
    expect(document.documentElement.lang).toBe('de')
    await waitFor(() => expect(document.title).toBe('Familienkochbuch'))
    expect(localStorage.getItem('cookbook.language')).toBe('de')
  })

  it('shows Hungarian text', async () => {
    mockApi({ '/api/recipes': [pie], '/api/categories': [] })
    renderApp('/', { language: 'hu' })

    expect(await screen.findByText('1 recept')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Receptek' })).toBeInTheDocument()
    expect(screen.getByText('45 perc')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Kategóriák és hozzávalók' })).toBeInTheDocument()
  })

  it('sends the language with API requests', async () => {
    const fetchMock = mockApi({ '/api/recipes': [], '/api/categories': [] })
    renderApp('/', { language: 'hu' })
    await screen.findByText('Még nincsenek receptek.')

    expect(fetchMock.mock.calls.every(([, options]) => options.headers['Accept-Language'] === 'hu')).toBe(true)
  })

  it('translates validation messages in the form', async () => {
    mockApi({ '/api/categories': [], '/api/ingredients': [] })
    renderApp('/recipes/new', { language: 'de' })
    await screen.findByRole('heading', { level: 1, name: 'Neues Rezept' })

    await userEvent.click(screen.getByRole('button', { name: 'Rezept erstellen' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Bitte korrigiere die markierten Felder.')
    expect(screen.getByLabelText('Name')).toHaveAccessibleDescription('Gib dem Rezept einen Namen.')
  })
})
