import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useCategories, useRecipes } from '../api/queries.js'
import Icon from '../components/Icon.jsx'
import RecipeCard from '../components/RecipeCard.jsx'
import StatusMessage from '../components/StatusMessage.jsx'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'

const SEARCH_DELAY_MS = 300

export default function RecipeListPage() {
  const { t } = useI18n()
  // Search and filter live in the URL so they survive reloads and can be linked to
  const [searchParams, setSearchParams] = useSearchParams()
  const search = searchParams.get('search') ?? ''
  const categoryId = searchParams.get('categoryId') ?? ''
  const [searchInput, setSearchInput] = useState(search)

  // When the URL changes from outside the input (header link, back button), show its search.
  // Our own debounced updates match the trimmed input already, so typing isn't interrupted.
  const [lastUrlSearch, setLastUrlSearch] = useState(search)
  if (search !== lastUrlSearch) {
    setLastUrlSearch(search)
    if (search !== searchInput.trim()) setSearchInput(search)
  }

  const recipes = useRecipes({ search, categoryId })
  const categories = useCategories()

  // Wait for a pause in typing before updating the URL, which triggers the request
  useEffect(() => {
    const trimmed = searchInput.trim()
    if (trimmed === search) return
    const timer = setTimeout(() => updateParam(setSearchParams, 'search', trimmed), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [searchInput, search, setSearchParams])

  const filtered = search !== '' || categoryId !== ''

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{t('list.title')}</h1>
          {recipes.data && <p className="count">{t('list.count', { count: recipes.data.length })}</p>}
        </div>
        <Link to="/recipes/new" className="button primary new-recipe">
          {t('list.newRecipe')}
        </Link>
      </div>

      <div className="toolbar" role="search">
        <label className="visually-hidden" htmlFor="recipe-search">
          {t('list.searchLabel')}
        </label>
        <div className="search-field">
          <Icon name="search" />
          <input
            id="recipe-search"
            type="search"
            placeholder={t('list.searchPlaceholder')}
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
        </div>
        <CategoryFilter
          categories={categories.data ?? []}
          selectedId={categoryId}
          onSelect={(id) => updateParam(setSearchParams, 'categoryId', id)}
        />
      </div>

      <RecipeResults query={recipes} filtered={filtered} />
    </>
  )
}

// One toggle button per category, with how many recipes it has. Categories nobody uses yet are
// left out (picking one would always show an empty list) unless one is already selected.
function CategoryFilter({ categories, selectedId, onSelect }) {
  const { t } = useI18n()
  const shown = categories.filter((category) => category.recipeCount !== 0 || category.id === selectedId)
  return (
    <div className="category-filter" role="group" aria-label={t('list.categoryLabel')}>
      <button type="button" className="filter-chip" aria-pressed={selectedId === ''} onClick={() => onSelect('')}>
        {t('list.allCategories')}
      </button>
      {shown.map((category) => (
        <button
          key={category.id}
          type="button"
          className="filter-chip"
          aria-pressed={selectedId === category.id}
          onClick={() => onSelect(selectedId === category.id ? '' : category.id)}
        >
          {category.name}
          {category.recipeCount != null && <span className="filter-count">{category.recipeCount}</span>}
        </button>
      ))}
    </div>
  )
}

function updateParam(setSearchParams, name, value) {
  setSearchParams(
    (params) => {
      if (value) params.set(name, value)
      else params.delete(name)
      return params
    },
    { replace: true },
  )
}

function RecipeResults({ query, filtered }) {
  const { t } = useI18n()
  if (query.isPending) {
    return <StatusMessage>{t('list.loading')}</StatusMessage>
  }
  if (query.isError) {
    return (
      <StatusMessage role="alert">
        <p>{t('list.loadFailed')}</p>
        <p className="hint">{errorMessage(query.error, t)}</p>
      </StatusMessage>
    )
  }
  if (query.data.length === 0) {
    return (
      <StatusMessage>
        {filtered ? (
          t('list.noMatches')
        ) : (
          <>
            <p>{t('list.empty')}</p>
            <Link to="/recipes/new">{t('list.addFirst')}</Link>
          </>
        )}
      </StatusMessage>
    )
  }
  return (
    <ul className="recipe-grid" aria-busy={query.isPlaceholderData}>
      {query.data.map((recipe) => (
        <li key={recipe.id}>
          <RecipeCard recipe={recipe} />
        </li>
      ))}
    </ul>
  )
}
