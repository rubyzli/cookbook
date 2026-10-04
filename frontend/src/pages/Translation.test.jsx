import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, requestedUrls, sentRequests } from '../test/utils.jsx'

const ON = { machineTranslation: true, languages: ['de', 'en', 'hu'] }
const OFF = { machineTranslation: false, languages: ['de', 'en', 'hu'] }

const hungarian = {
  id: 'r1',
  name: 'Krumpli saláta',
  description: null,
  servings: 4,
  prepTimeMinutes: null,
  cookTimeMinutes: null,
  instructions: 'Főzzük meg.\nVágjuk fel.',
  notes: null,
  imageUrl: null,
  createdBy: null,
  createdAt: '2026-10-04T12:00:00Z',
  categories: [],
  ingredients: [
    { ingredientId: 'i1', name: 'krumpli', amount: 25, unit: 'dkg', group: 'A salátához' },
    { ingredientId: 'i2', name: 'ecet', amount: 1, unit: 'ek', group: null },
  ],
  language: 'hu',
  originalLanguage: 'hu',
  translationStatus: null,
  translationOutdated: false,
}

const german = {
  ...hungarian,
  name: 'Kartoffelsalat',
  instructions: 'Kochen.\nSchneiden.',
  ingredients: [
    { ingredientId: 'i1', name: 'Kartoffeln', amount: 25, unit: 'dkg', group: 'Für den Salat' },
    { ingredientId: 'i2', name: 'Essig', amount: 1, unit: 'ek', group: null },
  ],
  language: 'en',
  translationStatus: 'MACHINE',
}

describe('recipe page in another language', () => {
  it('shows a machine translation with converted units, and the original on request', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': ON,
      '/api/recipes/r1': german,
      '/api/recipes/r1?original=true': hungarian,
    })
    renderApp('/recipes/r1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Kartoffelsalat' })).toBeInTheDocument()
    expect(screen.getByText('Machine translation · original: Hungarian')).toBeInTheDocument()
    const lines = within(screen.getByRole('region', { name: 'Ingredients' })).getAllByRole('listitem')
    expect(lines.map((line) => line.textContent)).toEqual(['250 g Kartoffeln', '1 tbsp Essig'])

    await userEvent.click(screen.getByRole('button', { name: 'Show original' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Krumpli saláta' })).toBeInTheDocument()
    expect(screen.getByText('Showing the original (Hungarian).')).toBeInTheDocument()
    const originalLines = within(screen.getByRole('region', { name: 'Ingredients' })).getAllByRole('listitem')
    expect(originalLines.map((line) => line.textContent)).toEqual(['25 dkg krumpli', '1 ek ecet'])
    expect(requestedUrls(fetchMock)).toContain('/api/recipes/r1?original=true')

    await userEvent.click(screen.getByRole('button', { name: 'Show translation' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Kartoffelsalat' })).toBeInTheDocument()
  })

  it('warns when the original changed after translating, and links to editing the translation', async () => {
    mockApi({
      '/api/translations/settings': ON,
      '/api/recipes/r1': { ...german, translationStatus: 'REVIEWED', translationOutdated: true },
    })
    renderApp('/recipes/r1')

    expect(await screen.findByText(/Translation · original: Hungarian/)).toHaveTextContent(
      'The original has been changed since.',
    )
    expect(screen.getByRole('link', { name: 'Edit translation' })).toHaveAttribute('href', '/recipes/r1/translate/en')
  })

  it('offers to translate an untranslated recipe automatically', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': ON,
      '/api/recipes/r1': hungarian,
      // Wrapped: the translation's own `status` field would read as an HTTP status
      'POST /api/recipes/r1/translations/en/machine': { status: 200, body: { language: 'en', status: 'MACHINE' } },
    })
    renderApp('/recipes/r1')

    expect(await screen.findByText('This recipe is only in Hungarian.')).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', { name: 'Translate automatically' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock)).toEqual([
        { method: 'POST', url: '/api/recipes/r1/translations/en/machine', body: undefined },
      ]),
    )
  })

  it('only offers translating by hand without a DeepL key', async () => {
    mockApi({ '/api/translations/settings': OFF, '/api/recipes/r1': hungarian })
    renderApp('/recipes/r1')

    expect(await screen.findByRole('link', { name: 'Translate by hand' })).toHaveAttribute(
      'href',
      '/recipes/r1/translate/en',
    )
    expect(screen.queryByRole('button', { name: 'Translate automatically' })).not.toBeInTheDocument()
  })

  it('shows no notice for a recipe written in the site language', async () => {
    mockApi({ '/api/translations/settings': ON, '/api/recipes/r1': hungarian })
    renderApp('/recipes/r1', { language: 'hu' })

    await screen.findByRole('heading', { level: 1, name: 'Krumpli saláta' })
    expect(screen.queryByText(/magyar/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Automatikus fordítás' })).not.toBeInTheDocument()
  })
})

const translations = (existing = []) => ({
  originalLanguage: 'hu',
  original: { name: 'Krumpli saláta', description: null, instructions: 'Főzzük meg.\nVágjuk fel.', notes: 'Hidegen jó.', groups: ['A salátához'] },
  translations: existing,
})

const machineDraft = {
  language: 'de',
  status: 'MACHINE',
  outdated: false,
  name: 'Kartoffelsalat',
  description: null,
  instructions: 'Kochen.\nSchneiden.',
  notes: 'Kalt gut.',
  groups: { 'A salátához': 'Zum Salat' },
  updatedAt: '2026-10-04T12:00:00Z',
}

describe('translation page', () => {
  it('fills in a machine draft, which is saved as reviewed', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': ON,
      '/api/recipes/r1/translations': translations(),
      'POST /api/recipes/r1/translations/de/machine': { status: 200, body: machineDraft },
      'PUT /api/recipes/r1/translations/de': ({ body }) => ({ status: 200, body: { ...machineDraft, ...body } }),
      '/api/recipes/r1': german,
    })
    renderApp('/recipes/r1/translate/de')

    expect(await screen.findByText('Not translated yet.')).toBeInTheDocument()
    expect(screen.getByText('Krumpli saláta', { selector: '.translate-original' })).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', { name: 'Fill in automatically' }))

    await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Kartoffelsalat'))
    expect(screen.getByLabelText('A salátához')).toHaveValue('Zum Salat')
    await userEvent.clear(screen.getByLabelText('A salátához'))
    await userEvent.type(screen.getByLabelText('A salátához'), 'Für den Salat')
    await userEvent.click(screen.getByRole('button', { name: 'Save as reviewed' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/recipes\/r1$/))
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(put.body).toEqual({
      name: 'Kartoffelsalat',
      description: '',
      instructions: 'Kochen.\nSchneiden.',
      notes: 'Kalt gut.',
      groups: { 'A salátához': 'Für den Salat' },
      status: 'REVIEWED',
    })
  })

  it('asks before replacing an existing translation with a machine draft', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': ON,
      '/api/recipes/r1/translations': translations([{ ...machineDraft, status: 'REVIEWED', name: 'Mein Salat' }]),
    })
    renderApp('/recipes/r1/translate/de')

    expect(await screen.findByLabelText('Name')).toHaveValue('Mein Salat')
    expect(screen.getByText('Reviewed.')).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', { name: 'Fill in automatically' }))

    expect(screen.getByRole('group', { name: 'Fill in automatically' })).toHaveTextContent(
      'This replaces the translation below, including corrections made by hand.',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('needs a name before saving', async () => {
    const fetchMock = mockApi({ '/api/translations/settings': OFF, '/api/recipes/r1/translations': translations() })
    renderApp('/recipes/r1/translate/de')

    expect(await screen.findByText('Automatic translation isn’t set up (no DeepL API key), so translate by hand.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Save as reviewed' }))

    expect(screen.getByLabelText('Name')).toHaveAccessibleDescription('Enter a name.')
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('deletes a translation after confirming', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': OFF,
      '/api/recipes/r1/translations': translations([machineDraft]),
      'DELETE /api/recipes/r1/translations/de': { status: 204 },
      '/api/recipes/r1': hungarian,
    })
    renderApp('/recipes/r1/translate/de')

    await userEvent.click(await screen.findByRole('button', { name: 'Delete translation' }))
    await userEvent.click(screen.getByRole('button', { name: 'Yes, delete' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock)).toEqual([{ method: 'DELETE', url: '/api/recipes/r1/translations/de', body: undefined }]),
    )
  })

  it('offers the other languages, and explains the original one', async () => {
    mockApi({ '/api/translations/settings': OFF, '/api/recipes/r1/translations': translations() })
    renderApp('/recipes/r1/translate/hu')

    expect(await screen.findByText('This recipe is written in Hungarian. Pick another language to translate into.')).toBeInTheDocument()
    const languages = within(screen.getByRole('navigation', { name: 'Translate into' })).getAllByRole('link')
    expect(languages.map((link) => link.textContent)).toEqual(['English', 'German'])
  })
})

describe('recipe language in the form', () => {
  it('defaults to the site language and is sent with the recipe', async () => {
    const fetchMock = mockApi({
      '/api/categories': [],
      '/api/ingredients': [],
      'POST /api/recipes': ({ body }) => ({ ...hungarian, ...body, id: 'r-new' }),
      '/api/recipes/r-new': hungarian,
    })
    renderApp('/recipes/new', { language: 'de' })

    const select = await screen.findByLabelText('Sprache des Rezepts')
    expect(select).toHaveValue('de')
    await userEvent.selectOptions(select, 'hu')
    await userEvent.type(screen.getByLabelText('Name'), 'Lecsó')
    await userEvent.click(screen.getByRole('button', { name: 'Rezept erstellen' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(1))
    expect(sentRequests(fetchMock)[0].body.language).toBe('hu')
  })
})

describe('ingredient and category names in other languages', () => {
  const flour = {
    id: 'i1',
    name: 'liszt',
    originalName: 'liszt',
    originalLanguage: 'hu',
    recipeCount: 2,
    translations: { de: { name: 'Mehl', status: 'MACHINE' }, en: { name: 'flour', status: 'REVIEWED' } },
  }

  it('shows translated names and saves only the changed ones', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': OFF,
      '/api/categories': [],
      '/api/ingredients': [flour],
      'PUT /api/ingredients/i1/translations/de': { status: 204 },
      'DELETE /api/ingredients/i1/translations/en': { status: 204 },
    })
    renderApp('/manage')

    const row = (await screen.findByText('liszt', { selector: '.name' })).closest('li')
    expect(row).toHaveTextContent('DE Mehl · machine')
    expect(row).toHaveTextContent('EN flour')
    await userEvent.click(within(row).getByRole('button', { name: 'Translations' }))
    await userEvent.clear(screen.getByLabelText('German name for liszt'))
    await userEvent.type(screen.getByLabelText('German name for liszt'), 'Weizenmehl')
    await userEvent.clear(screen.getByLabelText('English name for liszt'))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(2))
    expect(sentRequests(fetchMock)).toEqual(
      expect.arrayContaining([
        { method: 'PUT', url: '/api/ingredients/i1/translations/de', body: { name: 'Weizenmehl' } },
        { method: 'DELETE', url: '/api/ingredients/i1/translations/en', body: undefined },
      ]),
    )
  })

  it('finds names by any of their translations', async () => {
    mockApi({ '/api/translations/settings': OFF, '/api/categories': [], '/api/ingredients': [flour] })
    renderApp('/manage')
    await screen.findByText('liszt', { selector: '.name' })

    await userEvent.type(screen.getByRole('searchbox', { name: 'Filter ingredients' }), 'mehl')

    expect(screen.getByText('liszt', { selector: '.name' })).toBeInTheDocument()
  })

  it('fills in missing names automatically for every language', async () => {
    const fetchMock = mockApi({
      '/api/translations/settings': ON,
      '/api/categories': [],
      '/api/ingredients': [flour],
      'POST /api/ingredients/translations/en/machine': { translated: 2 },
      'POST /api/ingredients/translations/de/machine': { translated: 1 },
      'POST /api/ingredients/translations/hu/machine': { translated: 0 },
    })
    renderApp('/manage')

    const section = await screen.findByRole('region', { name: 'Ingredients' })
    await userEvent.click(await within(section).findByRole('button', { name: 'Translate missing names automatically' }))

    expect(await within(section).findByText('Translated 3 names.')).toBeInTheDocument()
    expect(sentRequests(fetchMock).map((request) => request.url)).toEqual([
      '/api/ingredients/translations/en/machine',
      '/api/ingredients/translations/de/machine',
      '/api/ingredients/translations/hu/machine',
    ])
  })

  it('matches an ingredient typed in another language instead of creating a duplicate', async () => {
    const fetchMock = mockApi({
      '/api/categories': [],
      '/api/ingredients': [flour],
      'POST /api/recipes': ({ body }) => ({ ...hungarian, ...body, id: 'r-new' }),
      '/api/recipes/r-new': hungarian,
    })
    renderApp('/recipes/new', { language: 'de' })

    await userEvent.type(await screen.findByLabelText('Name'), 'Brot')
    await userEvent.type(screen.getByLabelText('Zutat 1'), 'mehl')
    await userEvent.click(screen.getByRole('button', { name: 'Rezept erstellen' }))

    await waitFor(() => expect(sentRequests(fetchMock)).toHaveLength(1))
    expect(sentRequests(fetchMock)[0].body.ingredients[0].ingredientId).toBe('i1')
  })
})

describe('switching the site language', () => {
  it('loads the recipe list again in the new language', async () => {
    const fetchMock = mockApi({ '/api/recipes': [], '/api/categories': [] })
    renderApp('/')
    await screen.findByText('No recipes yet.')

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'de')

    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url, options]) => url === '/api/recipes' && options.headers['Accept-Language'] === 'de'),
      ).toBe(true),
    )
  })
})
