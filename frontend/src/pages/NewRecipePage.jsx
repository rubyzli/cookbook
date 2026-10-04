import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useCreateRecipe } from '../api/queries.js'
import ImportFromWebsite from '../components/ImportFromWebsite.jsx'
import RecipeForm from '../components/RecipeForm.jsx'
import { useI18n } from '../i18n/context.js'
import { emptyForm, formFromImport } from '../utils/recipeForm.js'

export default function NewRecipePage() {
  const { t, language } = useI18n()
  const createRecipe = useCreateRecipe()
  const navigate = useNavigate()
  // The latest import, with a counter so the form starts over for each one
  const [imported, setImported] = useState(null)

  async function save(request) {
    const created = await createRecipe.mutateAsync(request)
    navigate(`/recipes/${created.id}`, { replace: true })
  }

  return (
    <>
      <Link to="/" className="back-link">
        {t('common.backToRecipes')}
      </Link>
      <h1 className="form-title">{t('form.newTitle')}</h1>
      <ImportFromWebsite onImported={(draft) => setImported((current) => ({ draft, count: (current?.count ?? 0) + 1 }))} />
      {imported && <ImportNotice draft={imported.draft} />}
      <RecipeForm
        key={imported ? `import-${imported.count}` : 'new'}
        initialValues={imported ? formFromImport(imported.draft) : emptyForm(language)}
        submitLabel={t('form.create')}
        cancelTo="/"
        onSave={save}
      />
    </>
  )
}

function ImportNotice({ draft }) {
  const { t } = useI18n()
  let site = draft.sourceUrl
  try {
    site = new URL(draft.sourceUrl).hostname.replace(/^www\./, '')
  } catch {
    // Keep the address as it is
  }
  return (
    <div className="translation-notice import-notice" role="status">
      <span>
        {t('import.done', { site })}
        {draft.warnings?.includes('NO_RECIPE_DATA') && <> {t('import.noRecipeData')}</>}
        {draft.warnings?.includes('PHOTO_NOT_DOWNLOADED') && <> {t('import.photoLinked')}</>}
      </span>
    </div>
  )
}
