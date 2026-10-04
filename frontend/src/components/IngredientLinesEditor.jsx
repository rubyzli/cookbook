import { useIngredients } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { isHeading, lineField, newHeading, newLine } from '../utils/recipeForm.js'

const COMMON_UNITS = ['g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'cups', 'pinch', 'clove', 'slice', 'can']

// Ingredient rows: lines (amount, unit, name with suggestions from existing ingredients) and group
// headings, each with buttons to reorder or remove it. A name that doesn't exist yet is created on
// save. Lines and headings are numbered separately so labels like "Ingredient 2" stay put when a
// heading is added.
export default function IngredientLinesEditor({ lines: rows, onChange, errors }) {
  const { t } = useI18n()
  const ingredients = useIngredients()

  function update(key, field, value) {
    onChange(rows.map((row) => (row.key === key ? { ...row, [field]: value } : row)))
  }

  function move(index, offset) {
    const moved = [...rows]
    ;[moved[index], moved[index + offset]] = [moved[index + offset], moved[index]]
    onChange(moved)
  }

  function remove(key) {
    const remaining = rows.filter((row) => row.key !== key)
    onChange(remaining.some((row) => !isHeading(row)) ? remaining : [...remaining, newLine()])
  }

  let lineNumber = 0
  let headingNumber = 0

  return (
    <fieldset className="form-section">
      <legend>{t('form.ingredients')}</legend>
      <p className="field-hint">{t('form.ingredientsHint')}</p>

      <datalist id="ingredient-options">
        {ingredients.data?.map((ingredient) => (
          <option key={ingredient.id} value={ingredient.name} />
        ))}
      </datalist>
      <datalist id="unit-options">
        {COMMON_UNITS.map((unit) => (
          <option key={unit} value={unit} />
        ))}
      </datalist>

      <ol className="ingredient-rows">
        {rows.map((row, index) => {
          const buttons = { index, count: rows.length, onMove: move, onRemove: () => remove(row.key) }
          if (isHeading(row)) {
            const n = ++headingNumber
            const error = errors[lineField(row, 'name')]
            return (
              <li key={row.key} className="ingredient-row heading-row">
                <input
                  className="ingredient-group"
                  type="text"
                  placeholder={t('form.groupPlaceholder')}
                  aria-label={t('form.groupN', { n })}
                  aria-invalid={error ? true : undefined}
                  value={row.name}
                  onChange={(event) => update(row.key, 'name', event.target.value)}
                />
                <RowButtons
                  {...buttons}
                  upLabel={t('form.moveGroupUp', { n })}
                  downLabel={t('form.moveGroupDown', { n })}
                  removeLabel={t('form.removeGroup', { n })}
                />
                {error && <p className="field-error row-error">{error}</p>}
              </li>
            )
          }

          const n = ++lineNumber
          const lineErrors = ['name', 'amount', 'unit', 'group']
            .map((field) => errors[lineField(row, field)])
            .filter(Boolean)
          return (
            <li key={row.key} className="ingredient-row">
              <input
                className="ingredient-amount"
                type="text"
                inputMode="decimal"
                placeholder={t('form.amount')}
                aria-label={t('form.amountN', { n })}
                aria-invalid={errors[lineField(row, 'amount')] ? true : undefined}
                value={row.amount}
                onChange={(event) => update(row.key, 'amount', event.target.value)}
              />
              <input
                className="ingredient-unit"
                type="text"
                list="unit-options"
                placeholder={t('form.unit')}
                aria-label={t('form.unitN', { n })}
                aria-invalid={errors[lineField(row, 'unit')] ? true : undefined}
                value={row.unit}
                onChange={(event) => update(row.key, 'unit', event.target.value)}
              />
              <input
                className="ingredient-name"
                type="text"
                list="ingredient-options"
                placeholder={t('form.ingredient')}
                aria-label={t('form.ingredientN', { n })}
                aria-invalid={errors[lineField(row, 'name')] ? true : undefined}
                value={row.name}
                onChange={(event) => update(row.key, 'name', event.target.value)}
              />
              <RowButtons
                {...buttons}
                upLabel={t('form.moveUp', { n })}
                downLabel={t('form.moveDown', { n })}
                removeLabel={t('form.remove', { n })}
              />
              {lineErrors.length > 0 && <p className="field-error row-error">{lineErrors.join(' ')}</p>}
            </li>
          )
        })}
      </ol>

      <div className="row-adders">
        <button type="button" className="button" onClick={() => onChange([...rows, newLine()])}>
          {t('form.addIngredient')}
        </button>
        <button type="button" className="button ghost" onClick={() => onChange([...rows, newHeading()])}>
          {t('form.addGroup')}
        </button>
      </div>
    </fieldset>
  )
}

function RowButtons({ index, count, onMove, onRemove, upLabel, downLabel, removeLabel }) {
  return (
    <div className="row-actions">
      <button type="button" className="icon-button" aria-label={upLabel} disabled={index === 0} onClick={() => onMove(index, -1)}>
        ↑
      </button>
      <button
        type="button"
        className="icon-button"
        aria-label={downLabel}
        disabled={index === count - 1}
        onClick={() => onMove(index, 1)}
      >
        ↓
      </button>
      <button type="button" className="icon-button" aria-label={removeLabel} onClick={onRemove}>
        ×
      </button>
    </div>
  )
}
