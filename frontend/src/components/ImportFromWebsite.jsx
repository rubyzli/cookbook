import { useState } from 'react'
import { ApiError } from '../api/client.js'
import { useImportRecipe } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'

// The reasons the server gives (RecipeImportException codes), each with its own message
const ERROR_KEYS = {
  INVALID_URL: 'import.errorInvalidUrl',
  ADDRESS_NOT_ALLOWED: 'import.errorNotAllowed',
  UNKNOWN_SITE: 'import.errorUnknownSite',
  UNREACHABLE: 'import.errorUnreachable',
  TIMEOUT: 'import.errorTimeout',
  BLOCKED: 'import.errorBlocked',
  PAGE_NOT_FOUND: 'import.errorNotFound',
  NOT_A_PAGE: 'import.errorNotAPage',
  TOO_LARGE: 'import.errorTooLarge',
  NO_RECIPE_DATA: 'import.errorNoRecipe',
}

// A link box that reads a recipe from another site; onImported(draft) fills in the form
export default function ImportFromWebsite({ onImported }) {
  const { t } = useI18n()
  const importRecipe = useImportRecipe()
  const [url, setUrl] = useState('')
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    if (!url.trim()) {
      setError(t('import.errorInvalidUrl'))
      return
    }
    setError(null)
    try {
      onImported(await importRecipe.mutateAsync(url.trim()))
      setUrl('')
    } catch (e) {
      const key = e instanceof ApiError ? ERROR_KEYS[e.code] : undefined
      setError(key ? t(key) : t('import.errorGeneric'))
    }
  }

  return (
    <form className="import-box" onSubmit={handleSubmit} noValidate>
      <label htmlFor="import-url" className="import-title">
        {t('import.title')}
      </label>
      <p className="field-hint">{t('import.hint')}</p>
      <div className="import-row">
        <input
          id="import-url"
          type="url"
          inputMode="url"
          placeholder="https://www.nosalty.hu/recept/…"
          value={url}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'import-error' : undefined}
          onChange={(event) => setUrl(event.target.value)}
        />
        <button type="submit" className="button primary" disabled={importRecipe.isPending}>
          {importRecipe.isPending ? t('import.importing') : t('import.button')}
        </button>
      </div>
      {error && (
        <p id="import-error" className="field-error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}
