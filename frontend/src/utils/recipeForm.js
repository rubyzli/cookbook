// Form values are all strings (what the inputs hold); they become numbers and nulls only in toRequest.
// Each ingredient line has a stable `key` so React rows and error messages follow it when lines move.

let nextLineKey = 0

export function newLine(values = {}) {
  return { key: `line-${nextLineKey++}`, name: '', amount: '', unit: '', ...values }
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
    categoryIds: recipe.categories.map((category) => category.id),
    lines:
      recipe.ingredients.length > 0
        ? recipe.ingredients.map((line) =>
            newLine({ name: line.name, amount: toText(line.amount), unit: line.unit ?? '' }),
          )
        : [newLine()],
  }
}

export function normalizeName(name) {
  return name.trim().toLowerCase()
}

export function lineField(line, field) {
  return `lines.${line.key}.${field}`
}

// Lines the user left completely empty are ignored rather than treated as errors
export function filledLines(lines) {
  return lines.filter((line) => line.name.trim() || line.amount.trim() || line.unit.trim())
}

const WHOLE_NUMBER = /^\d{1,6}$/
// Up to 8 digits before and 2 after the decimal point, matching NUMERIC(10, 2); "1,5" is accepted too
const AMOUNT = /^\d{1,8}([.,]\d{1,2})?$/

// Mirrors the backend's validation so most mistakes are caught before a request is sent.
// Returns { fieldName: message }, with ingredient fields keyed by lineField().
export function validateForm(values) {
  const errors = {}

  if (!values.name.trim()) errors.name = 'Give the recipe a name.'
  checkLength(errors, 'name', values.name, 255)
  checkLength(errors, 'description', values.description, 255)
  checkLength(errors, 'imageUrl', values.imageUrl, 255)

  checkWholeNumber(errors, 'servings', values.servings, 1)
  checkWholeNumber(errors, 'prepTimeMinutes', values.prepTimeMinutes, 0)
  checkWholeNumber(errors, 'cookTimeMinutes', values.cookTimeMinutes, 0)

  for (const line of filledLines(values.lines)) {
    if (!line.name.trim()) errors[lineField(line, 'name')] = 'Enter an ingredient.'
    checkLength(errors, lineField(line, 'name'), line.name, 255)
    if (line.amount.trim() && !AMOUNT.test(line.amount.trim())) {
      errors[lineField(line, 'amount')] = 'Use a number with up to 2 decimals.'
    }
    checkLength(errors, lineField(line, 'unit'), line.unit, 50)
  }
  return errors
}

function checkLength(errors, field, text, max) {
  if (!errors[field] && text.trim().length > max) {
    errors[field] = `Keep this under ${max} characters.`
  }
}

function checkWholeNumber(errors, field, text, min) {
  const trimmed = text.trim()
  if (trimmed && (!WHOLE_NUMBER.test(trimmed) || Number(trimmed) < min)) {
    errors[field] = min === 1 ? 'Use a whole number, 1 or more.' : 'Use a whole number, 0 or more.'
  }
}

// Builds the API request body. `ingredientIds` maps normalizeName(name) to an ingredient id.
// Also returns the key of each submitted line, in order, so server errors like
// "ingredients[2].amount" can be traced back to the right row.
export function toRequest(values, ingredientIds) {
  const lines = filledLines(values.lines)
  return {
    request: {
      name: values.name.trim(),
      description: blankToNull(values.description),
      servings: toNumber(values.servings),
      prepTimeMinutes: toNumber(values.prepTimeMinutes),
      cookTimeMinutes: toNumber(values.cookTimeMinutes),
      instructions: blankToNull(values.instructions),
      imageUrl: blankToNull(values.imageUrl),
      categoryIds: values.categoryIds,
      ingredients: lines.map((line) => ({
        ingredientId: ingredientIds.get(normalizeName(line.name)),
        amount: toNumber(line.amount.replace(',', '.')),
        unit: blankToNull(line.unit),
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
