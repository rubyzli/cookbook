import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp, sentRequests } from '../test/utils.jsx'

const draft = {
  name: 'Rakott krumpli',
  description: 'Klasszikus.',
  servings: 6,
  prepTimeMinutes: 15,
  cookTimeMinutes: 30,
  instructions: 'Megfőzzük.\nRétegezzük.',
  imageUrl: '/images/upload-20261004-d2ce7e59.jpg',
  sourceUrl: 'https://www.nosalty.hu/recept/rakott-krumpli',
  language: 'hu',
  categoryIds: ['c1'],
  ingredients: [
    { amount: 1, unit: 'kg', name: 'burgonya' },
    { amount: null, unit: 'ízlés szerint', name: 'só' },
  ],
  recipeFound: true,
  warnings: [],
}

const recipe = {
  id: 'r-new',
  name: 'Rakott krumpli',
  description: null,
  servings: 6,
  prepTimeMinutes: 15,
  cookTimeMinutes: 30,
  instructions: 'Megfőzzük.',
  notes: null,
  imageUrl: null,
  sourceUrl: 'https://www.nosalty.hu/recept/rakott-krumpli',
  createdBy: null,
  createdAt: '2026-10-04T12:00:00Z',
  categories: [],
  ingredients: [],
  language: 'hu',
  originalLanguage: 'hu',
  translationStatus: null,
  translationOutdated: false,
}

function routes(extra = {}) {
  return {
    '/api/categories': [{ id: 'c1', name: 'Főétel', originalName: 'Főétel', originalLanguage: 'hu', recipeCount: 2, translations: {} }],
    '/api/ingredients': [],
    'POST /api/recipes': ({ body }) => ({ ...recipe, ...body, id: 'r-new' }),
    '/api/recipes/r-new': recipe,
    'POST /api/ingredients': ({ body }) => ({ id: `i-${body.name}`, name: body.name }),
    ...extra,
  }
}

async function importLink(url) {
  await userEvent.type(await screen.findByLabelText('Import from a website'), url)
  await userEvent.click(screen.getByRole('button', { name: 'Import' }))
}

describe('importing a recipe from a website', () => {
  it('fills in the form for checking, then saves with the source link', async () => {
    const fetchMock = mockApi(routes({ 'POST /api/recipes/import': draft }))
    renderApp('/recipes/new')

    await importLink('https://www.nosalty.hu/recept/rakott-krumpli')

    expect(await screen.findByRole('status')).toHaveTextContent('Imported from nosalty.hu. Check everything before saving.')
    expect(screen.getByLabelText('Name')).toHaveValue('Rakott krumpli')
    expect(screen.getByLabelText('Recipe language')).toHaveValue('hu')
    expect(screen.getByLabelText('Servings')).toHaveValue('6')
    expect(screen.getByLabelText('Steps')).toHaveValue('Megfőzzük.\nRétegezzük.')
    expect(screen.getByLabelText('Ingredient 1')).toHaveValue('burgonya')
    expect(screen.getByLabelText('Unit 2')).toHaveValue('ízlés szerint')
    expect(screen.getByRole('img', { name: 'Photo of the recipe' })).toHaveAttribute('src', draft.imageUrl)
    expect(screen.getByLabelText('Original recipe (link)')).toHaveValue(draft.sourceUrl)
    await waitFor(() => expect(screen.getByRole('checkbox', { name: 'Főétel' })).toBeChecked())

    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    await waitFor(() => expect(sentRequests(fetchMock).some((r) => r.url === '/api/recipes')).toBe(true))
    const create = sentRequests(fetchMock).find((r) => r.url === '/api/recipes')
    expect(create.body).toMatchObject({
      name: 'Rakott krumpli',
      language: 'hu',
      sourceUrl: draft.sourceUrl,
      imageUrl: draft.imageUrl,
      categoryIds: ['c1'],
      servings: 6,
    })
    expect(create.body.ingredients).toHaveLength(2)
    expect(sentRequests(fetchMock)[0]).toEqual({
      method: 'POST',
      url: '/api/recipes/import',
      body: { url: 'https://www.nosalty.hu/recept/rakott-krumpli' },
    })
  })

  it('says when only the title, description and photo could be taken', async () => {
    mockApi(
      routes({
        'POST /api/recipes/import': {
          ...draft,
          ingredients: [],
          instructions: null,
          recipeFound: false,
          warnings: ['NO_RECIPE_DATA', 'PHOTO_NOT_DOWNLOADED'],
        },
      }),
    )
    renderApp('/recipes/new')

    await importLink('https://example.com/lecso')

    const notice = await screen.findByRole('status')
    expect(notice).toHaveTextContent('That page has no recipe data, so only its title, description and photo were taken.')
    expect(notice).toHaveTextContent('The photo couldn’t be saved, so it’s linked from the site.')
    expect(screen.getByLabelText('Ingredient 1')).toHaveValue('')
  })

  it.each([
    ['BLOCKED', 502, 'That site doesn’t allow automatic downloads. Copy the recipe in by hand.'],
    ['ADDRESS_NOT_ALLOWED', 400, 'Only public web pages can be imported.'],
    ['TIMEOUT', 504, 'The site took too long to answer. Try again later.'],
    ['NO_RECIPE_DATA', 422, 'No recipe was found on that page.'],
    ['SOMETHING_NEW', 502, 'The recipe couldn’t be imported.'],
  ])('explains %s', async (code, status, message) => {
    mockApi(routes({ 'POST /api/recipes/import': { status, body: { status, code, detail: 'x' } } }))
    renderApp('/recipes/new')

    await importLink('https://site.example/recipe')

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.getByLabelText('Name')).toHaveValue('')
  })

  it('works in German', async () => {
    mockApi(routes({ 'POST /api/recipes/import': { status: 502, body: { status: 502, code: 'BLOCKED' } } }))
    renderApp('/recipes/new', { language: 'de' })

    await userEvent.type(await screen.findByLabelText('Von einer Website übernehmen'), 'https://www.chefkoch.de/x')
    await userEvent.click(screen.getByRole('button', { name: 'Übernehmen' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Diese Website erlaubt keine automatischen Downloads.')
  })
})

describe('source link', () => {
  it('is checked to be a web address', async () => {
    const fetchMock = mockApi(routes())
    renderApp('/recipes/new')

    await userEvent.type(await screen.findByLabelText('Name'), 'Pie')
    await userEvent.type(screen.getByLabelText('Original recipe (link)'), 'nosalty.hu')
    await userEvent.click(screen.getByRole('button', { name: 'Create recipe' }))

    expect(screen.getByLabelText('Original recipe (link)')).toHaveAccessibleDescription(
      /Use a web address starting with http:\/\/ or https:\/\//,
    )
    expect(sentRequests(fetchMock)).toEqual([])
  })

  it('is shown on the recipe page as a link to the site', async () => {
    mockApi({ '/api/recipes/r-new': recipe, '/api/translations/settings': { machineTranslation: false } })
    renderApp('/recipes/r-new', { language: 'hu' })

    const link = await screen.findByRole('link', { name: 'nosalty.hu' })
    expect(link).toHaveAttribute('href', recipe.sourceUrl)
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
    expect(within(link.closest('p')).getByText(/Eredeti recept:/)).toBeInTheDocument()
  })
})
