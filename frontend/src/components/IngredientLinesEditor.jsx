import { useIngredients } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { lineField, newLine } from '../utils/recipeForm.js'

const COMMON_UNITS = ['g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'cups', 'pinch', 'clove', 'slice', 'can']

// One row per ingredient line: name (with suggestions from existing ingredients), amount, unit,
// and buttons to reorder or remove the row. A name that doesn't exist yet is created on save.
export default function IngredientLinesEditor({ lines, onChange, errors }) {
  const { t } = useI18n()
  const ingredients = useIngredients()

  function update(key, field, value) {
    onChange(lines.map((line) => (line.key === key ? { ...line, [field]: value } : line)))
  }

  function move(index, offset) {
    const moved = [...lines]
    ;[moved[index], moved[index + offset]] = [moved[index + offset], moved[index]]
    onChange(moved)
  }

  function remove(key) {
    const remaining = lines.filter((line) => line.key !== key)
    onChange(remaining.length > 0 ? remaining : [newLine()])
  }

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
        {lines.map((line, index) => {
          const number = index + 1
          const lineErrors = ['name', 'amount', 'unit']
            .map((field) => errors[lineField(line, field)])
            .filter(Boolean)
          return (
            <li key={line.key} className="ingredient-row">
              <input
                className="ingredient-amount"
                type="text"
                inputMode="decimal"
                placeholder={t('form.amount')}
                aria-label={t('form.amountN', { n: number })}
                aria-invalid={errors[lineField(line, 'amount')] ? true : undefined}
                value={line.amount}
                onChange={(event) => update(line.key, 'amount', event.target.value)}
              />
              <input
                className="ingredient-unit"
                type="text"
                list="unit-options"
                placeholder={t('form.unit')}
                aria-label={t('form.unitN', { n: number })}
                aria-invalid={errors[lineField(line, 'unit')] ? true : undefined}
                value={line.unit}
                onChange={(event) => update(line.key, 'unit', event.target.value)}
              />
              <input
                className="ingredient-name"
                type="text"
                list="ingredient-options"
                placeholder={t('form.ingredient')}
                aria-label={t('form.ingredientN', { n: number })}
                aria-invalid={errors[lineField(line, 'name')] ? true : undefined}
                value={line.name}
                onChange={(event) => update(line.key, 'name', event.target.value)}
              />
              <div className="row-actions">
                <button
                  type="button"
                  className="icon-button"
                  aria-label={t('form.moveUp', { n: number })}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                >
                  ↑
                </button>
                <button
                  type="button"
                  className="icon-button"
                  aria-label={t('form.moveDown', { n: number })}
                  disabled={index === lines.length - 1}
                  onClick={() => move(index, 1)}
                >
                  ↓
                </button>
                <button
                  type="button"
                  className="icon-button"
                  aria-label={t('form.remove', { n: number })}
                  onClick={() => remove(line.key)}
                >
                  ×
                </button>
              </div>
              {lineErrors.length > 0 && (
                <p className="field-error row-error">{lineErrors.join(' ')}</p>
              )}
            </li>
          )
        })}
      </ol>

      <button type="button" className="button" onClick={() => onChange([...lines, newLine()])}>
        {t('form.addIngredient')}
      </button>
    </fieldset>
  )
}
