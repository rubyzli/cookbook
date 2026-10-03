import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiGet } from './client.js'

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
    select: (categories) => [...categories].sort((a, b) => a.name.localeCompare(b.name)),
  })
}
