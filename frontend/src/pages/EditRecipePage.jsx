import { Link, useNavigate, useParams } from 'react-router'
import { useRecipe, useUpdateRecipe } from '../api/queries.js'
import RecipeForm from '../components/RecipeForm.jsx'
import StatusMessage from '../components/StatusMessage.jsx'
import { formFromRecipe } from '../utils/recipeForm.js'

export default function EditRecipePage() {
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
        ← Back to recipe
      </Link>
      {recipe.isPending && <StatusMessage>Loading recipe…</StatusMessage>}
      {recipe.isError && (
        <StatusMessage role="alert">
          {recipe.error.status === 404 || recipe.error.status === 400
            ? 'This recipe doesn’t exist. It may have been deleted.'
            : `Couldn’t load this recipe: ${recipe.error.message}`}
        </StatusMessage>
      )}
      {recipe.isSuccess && (
        <>
          <h1 className="form-title">Edit {recipe.data.name}</h1>
          {/* Keyed by id so the form starts fresh if another recipe is opened */}
          <RecipeForm
            key={recipe.data.id}
            initialValues={formFromRecipe(recipe.data)}
            submitLabel="Save changes"
            cancelTo={`/recipes/${id}`}
            onSave={save}
          />
        </>
      )}
    </>
  )
}
