// Form values are all strings (what the inputs hold); they become numbers and nulls only in toRequest.
// `lines` holds the ingredient rows: ingredient lines, and group headings ({ kind: 'heading' }) that
// the lines below them belong to, up to the next heading. A blank heading ends a group.
// Each row has a stable `key` so React rows and error messages follow it when rows move.

let nextLineKey = 0

export function newLine(values = {}) {
  return { key: `line-${nextLineKey++}`, name: '', amount: '', unit: '', ...values }
}

export function newHeading(name = '') {
  return { key: `heading-${nextLineKey++}`, kind: 'heading', name }
}

export function isHeading(row) {
  return row.kind === 'heading'
}

export function emptyForm() {
  return {
    name: '',
    description: '',
    servings: '',
    prepTimeMinutes: '',
    cookTimeMinutes: '',
    imageUrl: '',
    instructions: '',
    notes: '',
    categoryIds: [],
    lines: [newLine()],
  }
}

export function formFromRecipe(recipe) {
  return {
    name: recipe.name,
    description: recipe.description ?? '',
    servings: toText(recipe.servings),
    prepTimeMinutes: toText(recipe.prepTimeMinutes),
    cookTimeMinutes: toText(recipe.cookTimeMinutes),
    imageUrl: recipe.imageUrl ?? '',
    instructions: recipe.instructions ?? '',
    notes: recipe.notes ?? '',
    categoryIds: recipe.categories.map((category) => category.id),
    lines: recipe.ingredients.length > 0 ? rowsFromIngredients(recipe.ingredients) : [newLine()],
  }
}

// A heading row wherever the group changes; a blank heading where lines go back to having none
function rowsFromIngredients(ingredients) {
  const rows = []
  let group = null
  for (const line of ingredients) {
    const lineGroup = line.group ?? null
    if (lineGroup !== group) {
      rows.push(newHeading(lineGroup ?? ''))
      group = lineGroup
    }
    rows.push(newLine({ name: line.name, amount: toText(line.amount), unit: line.unit ?? '' }))
  }
  return rows
}

export function normalizeName(name) {
  return name.trim().toLowerCase()
}

export function lineField(line, field) {
  return `lines.${line.key}.${field}`
}

// Ingredient lines (not headings) with something in them; completely empty lines are ignored
// rather than treated as errors
export function filledLines(rows) {
  return rows.filter((row) => !isHeading(row) && (row.name.trim() || row.amount.trim() || row.unit.trim()))
}

const WHOLE_NUMBER = /^\d{1,6}$/
// Up to 8 digits before and 2 after the decimal point, matching NUMERIC(10, 2); "1,5" is accepted too
const AMOUNT = /^\d{1,8}([.,]\d{1,2})?$/

// Mirrors the backend's validation so most mistakes are caught before a request is sent.
// Returns { fieldName: message } in the language of `t`, with ingredient fields keyed by lineField().
export function validateForm(values, t) {
  const errors = {}

  if (!values.name.trim()) errors.name = t('validation.nameRequired')
  checkLength(errors, t, 'name', values.name, 255)
  checkLength(errors, t, 'description', values.description, 255)
  checkLength(errors, t, 'imageUrl', values.imageUrl, 255)

  checkWholeNumber(errors, t, 'servings', values.servings, 1)
  checkWholeNumber(errors, t, 'prepTimeMinutes', values.prepTimeMinutes, 0)
  checkWholeNumber(errors, t, 'cookTimeMinutes', values.cookTimeMinutes, 0)

  for (const line of filledLines(values.lines)) {
    if (!line.name.trim()) errors[lineField(line, 'name')] = t('validation.ingredientRequired')
    checkLength(errors, t, lineField(line, 'name'), line.name, 255)
    if (line.amount.trim() && !AMOUNT.test(line.amount.trim())) {
      errors[lineField(line, 'amount')] = t('validation.amount')
    }
    checkLength(errors, t, lineField(line, 'unit'), line.unit, 50)
  }
  for (const heading of values.lines.filter(isHeading)) {
    checkLength(errors, t, lineField(heading, 'name'), heading.name, 100)
  }
  return errors
}

function checkLength(errors, t, field, text, max) {
  if (!errors[field] && text.trim().length > max) {
    errors[field] = t('validation.tooLong', { max })
  }
}

function checkWholeNumber(errors, t, field, text, min) {
  const trimmed = text.trim()
  if (trimmed && (!WHOLE_NUMBER.test(trimmed) || Number(trimmed) < min)) {
    errors[field] = t(min === 1 ? 'validation.wholeNumberMin1' : 'validation.wholeNumberMin0')
  }
}

// Builds the API request body. `ingredientIds` maps normalizeName(name) to an ingredient id.
// Also returns the key of each submitted line, in order, so server errors like
// "ingredients[2].amount" can be traced back to the right row.
export function toRequest(values, ingredientIds) {
  const lines = []
  let group = null
  for (const row of values.lines) {
    if (isHeading(row)) group = blankToNull(row.name)
    else if (filledLines([row]).length > 0) lines.push({ ...row, group })
  }
  return {
    request: {
      name: values.name.trim(),
      description: blankToNull(values.description),
      servings: toNumber(values.servings),
      prepTimeMinutes: toNumber(values.prepTimeMinutes),
      cookTimeMinutes: toNumber(values.cookTimeMinutes),
      instructions: blankToNull(values.instructions),
      notes: blankToNull(values.notes),
      imageUrl: blankToNull(values.imageUrl),
      categoryIds: values.categoryIds,
      ingredients: lines.map((line) => ({
        ingredientId: ingredientIds.get(normalizeName(line.name)),
        amount: toNumber(line.amount.replace(',', '.')),
        unit: blankToNull(line.unit),
        group: line.group,
      })),
    },
    lineKeys: lines.map((line) => line.key),
  }
}

// Turns the backend's `errors` map ("ingredients[0].ingredientId", "servings", ...) into form field names
export function formErrorsFromServer(serverErrors, lineKeys) {
  const errors = {}
  for (const [path, message] of Object.entries(serverErrors ?? {})) {
    const lineMatch = path.match(/^ingredients\[(\d+)\]\.(\w+)$/)
    if (lineMatch) {
      const key = lineKeys[Number(lineMatch[1])]
      const field = lineMatch[2] === 'ingredientId' ? 'name' : lineMatch[2]
      if (key) errors[`lines.${key}.${field}`] = message
    } else if (path.startsWith('categoryIds')) {
      errors.categoryIds = message
    } else {
      errors[path] = message
    }
  }
  return errors
}

function toText(value) {
  return value === null || value === undefined ? '' : String(value)
}

function toNumber(text) {
  const trimmed = text.trim()
  return trimmed === '' ? null : Number(trimmed)
}

function blankToNull(text) {
  const trimmed = text.trim()
  return trimmed === '' ? null : trimmed
}
