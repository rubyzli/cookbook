import { Link, useNavigate, useParams } from 'react-router'
import { useRecipe, useUpdateRecipe } from '../api/queries.js'
import RecipeForm from '../components/RecipeForm.jsx'
import StatusMessage from '../components/StatusMessage.jsx'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'
import { formFromRecipe } from '../utils/recipeForm.js'

export default function EditRecipePage() {
  const { t } = useI18n()
  const { id } = useParams()
  const recipe = useRecipe(id)
  const updateRecipe = useUpdateRecipe(id)
  const navigate = useNavigate()

  async function save(request) {
    await updateRecipe.mutateAsync(request)
    navigate(`/recipes/${id}`, { replace: true })
  }

  return (
    <>
      <Link to={`/recipes/${id}`} className="back-link">
        {t('form.backToRecipe')}
      </Link>
      {recipe.isPending && <StatusMessage>{t('common.loadingRecipe')}</StatusMessage>}
      {recipe.isError && (
        <StatusMessage role="alert">
          {recipe.error.status === 404 || recipe.error.status === 400
            ? t('common.recipeMissing')
            : t('form.loadFailed', { message: errorMessage(recipe.error, t) })}
        </StatusMessage>
      )}
      {recipe.isSuccess && (
        <>
          <h1 className="form-title">{t('form.editTitle', { name: recipe.data.name })}</h1>
          {/* Keyed by id so the form starts fresh if another recipe is opened */}
          <RecipeForm
            key={recipe.data.id}
            initialValues={formFromRecipe(recipe.data)}
            submitLabel={t('form.saveChanges')}
            cancelTo={`/recipes/${id}`}
            onSave={save}
          />
        </>
      )}
    </>
  )
}
