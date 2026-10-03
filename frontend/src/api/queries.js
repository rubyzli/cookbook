import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { normalizeName } from '../utils/recipeForm.js'
import { ApiError, apiGet, apiSend } from './client.js'

const byName = (a, b) => a.name.localeCompare(b.name)

export function useRecipes({ search, categoryId }) {
  return useQuery({
    queryKey: ['recipes', { search, categoryId }],
    queryFn: () => apiGet('/api/recipes', { search, categoryId }),
    // Keep showing the previous results while a new search loads, instead of flashing a spinner
    placeholderData: keepPreviousData,
  })
}

export function useRecipe(id) {
  return useQuery({
    queryKey: ['recipes', id],
    queryFn: () => apiGet(`/api/recipes/${id}`),
  })
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => apiGet('/api/categories'),
    select: (categories) => [...categories].sort(byName),
  })
}

export function useIngredients() {
  return useQuery({
    queryKey: ['ingredients'],
    queryFn: () => apiGet('/api/ingredients'),
    select: (ingredients) => [...ingredients].sort(byName),
  })
}

// Saving a recipe changes the list and that recipe's detail; both live under ['recipes']
function useRecipeMutation(mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (saved) => {
      if (saved) queryClient.setQueryData(['recipes', saved.id], saved)
      return queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
  })
}

export function useCreateRecipe() {
  return useRecipeMutation((request) => apiSend('POST', '/api/recipes', request))
}

export function useUpdateRecipe(id) {
  return useRecipeMutation((request) => apiSend('PUT', `/api/recipes/${id}`, request))
}

export function useDeleteRecipe(id) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiSend('DELETE', `/api/recipes/${id}`),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['recipes', id], exact: true })
      return queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
  })
}

// Create, rename and delete for the simple named lists. `kind` is 'categories' or 'ingredients'.
// Renames and deletes also refresh recipes, which show these names.
export function useCreateItem(kind) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name) => apiSend('POST', `/api/${kind}`, { name }),
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

export function useCreateCategory() {
  return useCreateItem('categories')
}

// Returns a Map from lower-cased ingredient name to id, creating any names that don't exist yet.
// Reads a fresh list first so ingredients added elsewhere since the page loaded are reused.
export async function findOrCreateIngredients(names) {
  const existing = await apiGet('/api/ingredients')
  const ids = new Map(existing.map((ingredient) => [normalizeName(ingredient.name), ingredient.id]))
  for (const name of names) {
    const key = normalizeName(name)
    if (!key || ids.has(key)) continue
    ids.set(key, await createIngredient(name.trim()))
  }
  return ids
}

async function createIngredient(name) {
  try {
    return (await apiSend('POST', '/api/ingredients', { name })).id
  } catch (error) {
    // Someone created it in the meantime: use theirs
    if (error instanceof ApiError && error.status === 409) {
      const all = await apiGet('/api/ingredients')
      const match = all.find((ingredient) => normalizeName(ingredient.name) === normalizeName(name))
      if (match) return match.id
    }
    throw error
  }
}
