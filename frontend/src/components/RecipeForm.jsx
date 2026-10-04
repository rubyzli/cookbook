import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client.js'
import { findOrCreateIngredients } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'
import { LANGUAGES } from '../i18n/translate.js'
import {
  filledLines,
  formErrorsFromServer,
  toRequest,
  validateForm,
} from '../utils/recipeForm.js'
import CategoryPicker from './CategoryPicker.jsx'
import Field from './Field.jsx'
import IngredientLinesEditor from './IngredientLinesEditor.jsx'

// Shared by the new and edit pages. `onSave(request)` sends the request and resolves with the
// saved recipe; this component handles validation, creating new ingredients and showing errors.
export default function RecipeForm({ initialValues, submitLabel, cancelTo, onSave }) {
  const { t } = useI18n()
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const formRef = useRef(null)

  function set(field, value) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  function showErrors(fieldErrors, message) {
    setErrors(fieldErrors)
    setFormError(message)
    // Wait for the error attributes to render, then move focus to the first problem
    requestAnimationFrame(() => formRef.current?.querySelector('[aria-invalid="true"]')?.focus())
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const clientErrors = validateForm(values, t)
    if (Object.keys(clientErrors).length > 0) {
      showErrors(clientErrors, t('form.fixFields'))
      return
    }

    setSaving(true)
    setErrors({})
    setFormError(null)
    let lineKeys = []
    try {
      const ingredientIds = await findOrCreateIngredients(
        filledLines(values.lines).map((line) => line.name),
        values.language,
      )
      const built = toRequest(values, ingredientIds)
      lineKeys = built.lineKeys
      await onSave(built.request)
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        showErrors({ name: t('form.nameTaken') }, t('form.fixFields'))
      } else if (error instanceof ApiError && Object.keys(error.errors).length > 0) {
        showErrors(formErrorsFromServer(error.errors, lineKeys), t('form.fixFields'))
      } else {
        showErrors({}, t('form.saveFailed', { message: errorMessage(error, t) }))
      }
      setSaving(false)
    }
  }

  return (
    <form ref={formRef} className="recipe-form" onSubmit={handleSubmit} noValidate>
      {formError && (
        <p className="form-error" role="alert">
          {formError}
        </p>
      )}

      <fieldset className="form-section">
        <legend>{t('form.basics')}</legend>
        <Field label={t('form.language')} hint={t('form.languageHint')} error={errors.language}>
          {(props) => (
            <select {...props} value={values.language} onChange={(e) => set('language', e.target.value)}>
              {LANGUAGES.map(({ code }) => (
                <option key={code} value={code}>
                  {t(`language.${code}`)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('form.name')} error={errors.name}>
          {(props) => (
            <input {...props} type="text" value={values.name} onChange={(e) => set('name', e.target.value)} />
          )}
        </Field>
        <Field label={t('form.description')} hint={t('form.descriptionHint')} error={errors.description}>
          {(props) => (
            <input
              {...props}
              type="text"
              value={values.description}
              onChange={(e) => set('description', e.target.value)}
            />
          )}
        </Field>
        <Field label={t('form.imageUrl')} hint={t('form.imageUrlHint')} error={errors.imageUrl}>
          {(props) => (
            <input
              {...props}
              type="url"
              placeholder="https://…"
              value={values.imageUrl}
              onChange={(e) => set('imageUrl', e.target.value)}
            />
          )}
        </Field>
        <div className="field-row">
          <Field label={t('form.servings')} error={errors.servings}>
            {(props) => (
              <input
                {...props}
                type="text"
                inputMode="numeric"
                value={values.servings}
                onChange={(e) => set('servings', e.target.value)}
              />
            )}
          </Field>
          <Field label={t('form.prepTime')} error={errors.prepTimeMinutes}>
            {(props) => (
              <input
                {...props}
                type="text"
                inputMode="numeric"
                value={values.prepTimeMinutes}
                onChange={(e) => set('prepTimeMinutes', e.target.value)}
              />
            )}
          </Field>
          <Field label={t('form.cookTime')} error={errors.cookTimeMinutes}>
            {(props) => (
              <input
                {...props}
                type="text"
                inputMode="numeric"
                value={values.cookTimeMinutes}
                onChange={(e) => set('cookTimeMinutes', e.target.value)}
              />
            )}
          </Field>
        </div>
      </fieldset>

      <CategoryPicker
        selectedIds={values.categoryIds}
        onChange={(ids) => set('categoryIds', ids)}
        error={errors.categoryIds}
      />

      <IngredientLinesEditor lines={values.lines} onChange={(lines) => set('lines', lines)} errors={errors} />

      <fieldset className="form-section">
        <legend>{t('form.instructions')}</legend>
        <Field label={t('form.steps')} hint={t('form.stepsHint')} error={errors.instructions}>
          {(props) => (
            <textarea
              {...props}
              rows={8}
              value={values.instructions}
              onChange={(e) => set('instructions', e.target.value)}
            />
          )}
        </Field>
        <Field label={t('form.notes')} hint={t('form.notesHint')} error={errors.notes}>
          {(props) => (
            <textarea {...props} rows={4} value={values.notes} onChange={(e) => set('notes', e.target.value)} />
          )}
        </Field>
      </fieldset>

      <div className="form-actions">
        <button type="submit" className="button primary" disabled={saving}>
          {saving ? t('form.saving') : submitLabel}
        </button>
        <Link to={cancelTo} className="button ghost">
          {t('common.cancel')}
        </Link>
      </div>
    </form>
  )
}
