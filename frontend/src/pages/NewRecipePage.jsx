import { Link, useNavigate } from 'react-router'
import { useCreateRecipe } from '../api/queries.js'
import RecipeForm from '../components/RecipeForm.jsx'
import { emptyForm } from '../utils/recipeForm.js'

export default function NewRecipePage() {
  const createRecipe = useCreateRecipe()
  const navigate = useNavigate()

  async function save(request) {
    const created = await createRecipe.mutateAsync(request)
    navigate(`/recipes/${created.id}`, { replace: true })
  }

  return (
    <>
      <Link to="/" className="back-link">
        ← All recipes
      </Link>
      <h1 className="form-title">New recipe</h1>
      <RecipeForm initialValues={emptyForm()} submitLabel="Create recipe" cancelTo="/" onSave={save} />
    </>
  )
}
