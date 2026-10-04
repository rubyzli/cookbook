import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../i18n/context.js'
import { normalizeName } from '../utils/recipeForm.js'
import { ApiError, apiGet, apiSend, apiUpload } from './client.js'

// The API answers in the site language (sent as Accept-Language), so cached data is kept per language

export function useRecipes({ search, categoryId }) {
  const { language } = useI18n()
  return useQuery({
    queryKey: ['recipes', language, { search, categoryId }],
    queryFn: () => apiGet('/api/recipes', { search, categoryId }),
    // Keep showing the previous results while a new search loads, instead of flashing a spinner
    placeholderData: keepPreviousData,
  })
}

// original: the recipe as written, without translations (for editing it)
export function useRecipe(id, { original = false } = {}) {
  const { language } = useI18n()
  return useQuery({
    queryKey: ['recipes', original ? 'original' : language, id],
    queryFn: () => apiGet(`/api/recipes/${id}`, original ? { original: true } : {}),
  })
}

// Sorted by the server, by the name shown in the site language
export function useCategories() {
  const { language } = useI18n()
  return useQuery({
    queryKey: ['categories', language],
    queryFn: () => apiGet('/api/categories'),
  })
}

export function useIngredients() {
  const { language } = useI18n()
  return useQuery({
    queryKey: ['ingredients', language],
    queryFn: () => apiGet('/api/ingredients'),
  })
}

// Saving or deleting a recipe changes the lists and details, all under ['recipes']
function useRecipeMutation(mutationFn, extraKeys = []) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () =>
      Promise.all([['recipes'], ...extraKeys].map((queryKey) => queryClient.invalidateQueries({ queryKey }))),
  })
}

export function useCreateRecipe() {
  return useRecipeMutation((request) => apiSend('POST', '/api/recipes', request))
}

export function useUpdateRecipe(id) {
  return useRecipeMutation((request) => apiSend('PUT', `/api/recipes/${id}`, request), [['translations', id]])
}

export function useDeleteRecipe(id) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiSend('DELETE', `/api/recipes/${id}`),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['recipes'], predicate: (query) => query.queryKey.at(-1) === id })
      return queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
  })
}

// Create, rename and delete for the simple named lists. `kind` is 'categories' or 'ingredients'.
// New names are stored as written in the site language. Renames and deletes also refresh recipes,
// which show these names.
export function useCreateItem(kind) {
  const queryClient = useQueryClient()
  const { language } = useI18n()
  return useMutation({
    mutationFn: (name) => apiSend('POST', `/api/${kind}`, { name, language }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind] }),
  })
}

export function useRenameItem(kind) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }) => apiSend('PUT', `/api/${kind}/${id}`, { name }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: [kind] }),
        queryClient.invalidateQueries({ queryKey: ['recipes'] }),
      ]),
  })
}

export function useDeleteItem(kind) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id) => apiSend('DELETE', `/api/${kind}/${id}`),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: [kind] }),
        queryClient.invalidateQueries({ queryKey: ['recipes'] }),
      ]),
  })
}

// Reads a recipe from a web page into a draft for the form (nothing is saved yet)
export function useImportRecipe() {
  return useMutation({
    mutationFn: (url) => apiSend('POST', '/api/recipes/import', { url }),
  })
}

// Uploads a photo into the database; resolves with its URL ("/api/images/...")
export function useUploadPhoto() {
  return useMutation({
    mutationFn: async (file) => (await apiUpload('/api/images', file)).url,
  })
}

export function useCreateCategory() {
  return useCreateItem('categories')
}

// Every name an ingredient or category is known by: the original and all translations
export function allNames(item) {
  return [item.name, item.originalName, ...Object.values(item.translations ?? {}).map((t) => t.name)].filter(Boolean)
}

// Returns a Map from lower-cased ingredient name to id, creating any names that don't exist yet.
// Reads a fresh list first so ingredients added elsewhere since the page loaded are reused. A name
// matches an ingredient in any language, so "Mehl" finds "liszt" once it has that German name.
// New ingredients are stored as written in `language`.
export async function findOrCreateIngredients(names, language) {
  const ids = await knownIngredientIds()
  for (const name of names) {
    const key = normalizeName(name)
    if (!key || ids.has(key)) continue
    ids.set(key, await createIngredient(name.trim(), language))
  }
  return ids
}

async function knownIngredientIds() {
  const ids = new Map()
  for (const ingredient of await apiGet('/api/ingredients')) {
    for (const name of allNames(ingredient)) {
      const key = normalizeName(name)
      if (!ids.has(key)) ids.set(key, ingredient.id)
    }
  }
  return ids
}

async function createIngredient(name, language) {
  try {
    return (await apiSend('POST', '/api/ingredients', { name, language })).id
  } catch (error) {
    // Someone created it in the meantime: use theirs
    if (error instanceof ApiError && error.status === 409) {
      const match = (await knownIngredientIds()).get(normalizeName(name))
      if (match) return match
    }
    throw error
  }
}

// Translations

export function useTranslationSettings() {
  return useQuery({
    queryKey: ['translation-settings'],
    queryFn: () => apiGet('/api/translations/settings'),
    staleTime: Infinity,
  })
}

// The original text and every translation of one recipe, for the translation page
export function useRecipeTranslations(id) {
  return useQuery({
    queryKey: ['translations', id],
    queryFn: () => apiGet(`/api/recipes/${id}/translations`),
  })
}

function useTranslationMutation(id, mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    // A recipe translation can also add ingredient and category names
    onSuccess: () =>
      Promise.all(
        [['translations', id], ['recipes'], ['ingredients'], ['categories']].map((queryKey) =>
          queryClient.invalidateQueries({ queryKey }),
        ),
      ),
  })
}

export function useMachineTranslate(id) {
  return useTranslationMutation(id, (language) => apiSend('POST', `/api/recipes/${id}/translations/${language}/machine`))
}

export function useSaveTranslation(id) {
  return useTranslationMutation(id, ({ language, translation }) =>
    apiSend('PUT', `/api/recipes/${id}/translations/${language}`, translation),
  )
}

export function useDeleteTranslation(id) {
  return useTranslationMutation(id, (language) => apiSend('DELETE', `/api/recipes/${id}/translations/${language}`))
}

// Names of ingredients and categories in other languages. `kind` is 'categories' or 'ingredients'.
function useNameMutation(kind, mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () =>
      Promise.all([[kind], ['recipes']].map((queryKey) => queryClient.invalidateQueries({ queryKey }))),
  })
}

// changes: { languageCode: name }; a blank name removes that translation
export function useSaveNameTranslations(kind) {
  return useNameMutation(kind, ({ id, changes }) =>
    Promise.all(
      Object.entries(changes).map(([language, name]) =>
        name.trim()
          ? apiSend('PUT', `/api/${kind}/${id}/translations/${language}`, { name: name.trim() })
          : apiSend('DELETE', `/api/${kind}/${id}/translations/${language}`),
      ),
    ),
  )
}

// Machine-translates every name that has no translation yet, into each given language.
// Resolves with how many names were translated.
export function useFillNameTranslations(kind) {
  return useNameMutation(kind, async (languages) => {
    let translated = 0
    for (const language of languages) {
      translated += (await apiSend('POST', `/api/${kind}/translations/${language}/machine`)).translated
    }
    return translated
  })
}
