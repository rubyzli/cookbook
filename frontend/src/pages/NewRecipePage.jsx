import { Link, useNavigate } from 'react-router'
import { useCreateRecipe } from '../api/queries.js'
import RecipeForm from '../components/RecipeForm.jsx'
import { useI18n } from '../i18n/context.js'
import { emptyForm } from '../utils/recipeForm.js'

export default function NewRecipePage() {
  const { t, language } = useI18n()
  const createRecipe = useCreateRecipe()
  const navigate = useNavigate()

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
      <RecipeForm initialValues={emptyForm(language)} submitLabel={t('form.create')} cancelTo="/" onSave={save} />
    </>
  )
}
