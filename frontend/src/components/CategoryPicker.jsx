import { useState } from 'react'
import { useCategories, useCreateCategory } from '../api/queries.js'
import { normalizeName } from '../utils/recipeForm.js'

// Existing categories as toggle chips, plus a box to add a new one (which is created right away)
export default function CategoryPicker({ selectedIds, onChange, error }) {
  const categories = useCategories()
  const createCategory = useCreateCategory()
  const [newName, setNewName] = useState('')
  const [addError, setAddError] = useState(null)

  function toggle(id) {
    onChange(selectedIds.includes(id) ? selectedIds.filter((selected) => selected !== id) : [...selectedIds, id])
  }

  function select(id) {
    if (!selectedIds.includes(id)) onChange([...selectedIds, id])
  }

  async function addCategory() {
    const name = newName.trim()
    if (!name) return
    setAddError(null)
    const existing = categories.data?.find((category) => normalizeName(category.name) === normalizeName(name))
    if (existing) {
      select(existing.id)
      setNewName('')
      return
    }
    try {
      const created = await createCategory.mutateAsync(name)
      select(created.id)
      setNewName('')
    } catch (e) {
      setAddError(`Couldn’t add “${name}”: ${e.message}`)
    }
  }

  return (
    <fieldset className="form-section">
      <legend>Categories</legend>
      {/* After a failed refetch the last good list is kept, so check isError before calling it empty */}
      {categories.isError && (
        <p className="field-error">Couldn’t load categories: {categories.error.message}</p>
      )}
      {!categories.isError && categories.data?.length === 0 && (
        <p className="field-hint">No categories yet. Add one below.</p>
      )}
      {categories.data?.length > 0 && (
        <div className="chip-options">
          {categories.data.map((category) => (
            <label key={category.id} className="chip-option">
              <input
                type="checkbox"
                checked={selectedIds.includes(category.id)}
                onChange={() => toggle(category.id)}
              />
              <span>{category.name}</span>
            </label>
          ))}
        </div>
      )}
      <div className="inline-add">
        <label className="visually-hidden" htmlFor="new-category">
          New category
        </label>
        <input
          id="new-category"
          type="text"
          placeholder="New category"
          value={newName}
          maxLength={255}
          onChange={(event) => setNewName(event.target.value)}
          onKeyDown={(event) => {
            // Enter adds the category instead of submitting the whole recipe form
            if (event.key === 'Enter') {
              event.preventDefault()
              addCategory()
            }
          }}
        />
        <button
          type="button"
          className="button"
          onClick={addCategory}
          disabled={!newName.trim() || createCategory.isPending}
        >
          Add
        </button>
      </div>
      {(addError || error) && <p className="field-error">{addError || error}</p>}
    </fieldset>
  )
}
