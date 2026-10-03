import { describe, expect, it } from 'vitest'
import {
  emptyForm,
  formErrorsFromServer,
  formFromRecipe,
  lineField,
  newLine,
  toRequest,
  validateForm,
} from './recipeForm.js'
import { createTranslator } from '../i18n/translate.js'

const en = createTranslator('en')

function form(overrides = {}) {
  return { ...emptyForm(), name: 'Pie', ...overrides }
}

describe('formFromRecipe', () => {
  it('turns a recipe into string form values', () => {
    const values = formFromRecipe({
      name: 'Pie',
      description: null,
      servings: 8,
      prepTimeMinutes: 0,
      cookTimeMinutes: null,
      imageUrl: null,
      instructions: 'Bake.',
      categories: [{ id: 'c1', name: 'Dessert' }],
      ingredients: [{ ingredientId: 'i1', name: 'Flour', amount: 250, unit: 'g' }],
    })

    expect(values).toMatchObject({
      name: 'Pie',
      description: '',
      servings: '8',
      prepTimeMinutes: '0',
      cookTimeMinutes: '',
      imageUrl: '',
      instructions: 'Bake.',
      categoryIds: ['c1'],
    })
    expect(values.lines).toEqual([expect.objectContaining({ name: 'Flour', amount: '250', unit: 'g' })])
  })

  it('starts with one empty line when the recipe has no ingredients', () => {
    const values = formFromRecipe({ name: 'Pie', categories: [], ingredients: [] })

    expect(values.lines).toEqual([expect.objectContaining({ name: '', amount: '', unit: '' })])
  })
})

describe('validateForm', () => {
  it('accepts a form with just a name', () => {
    expect(validateForm(form(), en)).toEqual({})
  })

  it('requires a name', () => {
    expect(validateForm(form({ name: '   ' }), en)).toEqual({ name: 'Give the recipe a name.' })
  })

  it('limits text lengths to the database columns', () => {
    const errors = validateForm(form({ name: 'x'.repeat(256), description: 'x'.repeat(256) }), en)

    expect(errors.name).toBe('Keep this under 255 characters.')
    expect(errors.description).toBe('Keep this under 255 characters.')
  })

  it.each([
    ['servings', '0', 'Use a whole number, 1 or more.'],
    ['servings', '2.5', 'Use a whole number, 1 or more.'],
    ['prepTimeMinutes', '-5', 'Use a whole number, 0 or more.'],
    ['cookTimeMinutes', 'abc', 'Use a whole number, 0 or more.'],
  ])('rejects %s = "%s"', (field, value, message) => {
    expect(validateForm(form({ [field]: value }), en)[field]).toBe(message)
  })

  it('allows zero minutes and blank numbers', () => {
    expect(validateForm(form({ prepTimeMinutes: '0', cookTimeMinutes: '', servings: ' 4 ' }), en)).toEqual({})
  })

  it('ignores completely empty ingredient lines', () => {
    expect(validateForm(form({ lines: [newLine(), newLine({ name: 'Flour' })] }), en)).toEqual({})
  })

  it('needs an ingredient name when amount or unit is filled in', () => {
    const line = newLine({ amount: '2', unit: 'cups' })

    expect(validateForm(form({ lines: [line] }), en)).toEqual({ [lineField(line, 'name')]: 'Enter an ingredient.' })
  })

  it.each(['1', '1.5', '1,5', '0.25', '12345678.99'])('accepts amount "%s"', (amount) => {
    expect(validateForm(form({ lines: [newLine({ name: 'Flour', amount })] }), en)).toEqual({})
  })

  it.each(['1.234', '-1', 'two', '123456789'])('rejects amount "%s"', (amount) => {
    const line = newLine({ name: 'Flour', amount })

    expect(validateForm(form({ lines: [line] }), en)[lineField(line, 'amount')]).toBe(
      'Use a number with up to 2 decimals.',
    )
  })
})

describe('validateForm in other languages', () => {
  it('returns messages in the translator’s language', () => {
    expect(validateForm(form({ name: '' }), createTranslator('de'))).toEqual({ name: 'Gib dem Rezept einen Namen.' })
    expect(validateForm(form({ name: '' }), createTranslator('hu'))).toEqual({ name: 'Adj nevet a receptnek.' })
  })
})

describe('toRequest', () => {
  it('builds the API body with trimmed text, numbers and nulls', () => {
    const flour = newLine({ name: ' flour ', amount: '1,5', unit: ' cups ' })
    const salt = newLine({ name: 'Salt' })
    const values = form({
      name: '  Pie ',
      description: '  ',
      servings: '8',
      prepTimeMinutes: '0',
      instructions: 'Mix.\nBake.\n',
      categoryIds: ['c1'],
      lines: [newLine(), flour, salt],
    })
    const ids = new Map([
      ['flour', 'i-flour'],
      ['salt', 'i-salt'],
    ])

    const { request, lineKeys } = toRequest(values, ids)

    expect(request).toEqual({
      name: 'Pie',
      description: null,
      servings: 8,
      prepTimeMinutes: 0,
      cookTimeMinutes: null,
      instructions: 'Mix.\nBake.',
      imageUrl: null,
      categoryIds: ['c1'],
      ingredients: [
        { ingredientId: 'i-flour', amount: 1.5, unit: 'cups' },
        { ingredientId: 'i-salt', amount: null, unit: null },
      ],
    })
    expect(lineKeys).toEqual([flour.key, salt.key])
  })
})

describe('formErrorsFromServer', () => {
  it('maps request paths back to form fields and ingredient rows', () => {
    const errors = formErrorsFromServer(
      {
        servings: 'must be greater than 0',
        'ingredients[1].amount': 'must be greater than or equal to 0',
        'ingredients[0].ingredientId': 'must not be null',
        'categoryIds[0]': 'must not be null',
      },
      ['line-a', 'line-b'],
    )

    expect(errors).toEqual({
      servings: 'must be greater than 0',
      'lines.line-b.amount': 'must be greater than or equal to 0',
      'lines.line-a.name': 'must not be null',
      categoryIds: 'must not be null',
    })
  })
})
