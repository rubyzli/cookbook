import { Link, useParams } from 'react-router'
import { useRecipe } from '../api/queries.js'
import CategoryChips from '../components/CategoryChips.jsx'
import RecipeImage from '../components/RecipeImage.jsx'
import RecipeMeta from '../components/RecipeMeta.jsx'
import StatusMessage from '../components/StatusMessage.jsx'
import { formatAmount } from '../utils/format.js'

export default function RecipeDetailPage() {
  const { id } = useParams()
  const recipe = useRecipe(id)

  return (
    <>
      <Link to="/" className="back-link">
        ← All recipes
      </Link>
      <RecipeDetailContent query={recipe} />
    </>
  )
}

function RecipeDetailContent({ query }) {
  if (query.isPending) {
    return <StatusMessage>Loading recipe…</StatusMessage>
  }
  if (query.isError) {
    // A malformed id gets a 400 from the backend; treat it the same as a missing recipe
    const missing = query.error.status === 404 || query.error.status === 400
    return (
      <StatusMessage role="alert">
        {missing ? (
          <p>This recipe doesn’t exist. It may have been deleted.</p>
        ) : (
          <>
            <p>Couldn’t load this recipe.</p>
            <p className="hint">{query.error.message}</p>
          </>
        )}
      </StatusMessage>
    )
  }

  const recipe = query.data
  return (
    <article className="recipe-detail">
      <header className={recipe.imageUrl ? 'recipe-detail-header with-image' : 'recipe-detail-header'}>
        {/* No placeholder tile here: without a photo the title gets the full width */}
        {recipe.imageUrl && <RecipeImage recipe={recipe} className="recipe-detail-image" />}
        <div className="recipe-detail-heading">
          <h1>{recipe.name}</h1>
          {recipe.description && <p className="description">{recipe.description}</p>}
          <RecipeMeta recipe={recipe} showTotal />
          <CategoryChips categories={recipe.categories} />
        </div>
      </header>

      <div className="recipe-detail-body">
        <section aria-labelledby="ingredients-heading">
          <h2 id="ingredients-heading">Ingredients</h2>
          {recipe.ingredients.length === 0 ? (
            <p className="hint">No ingredients listed.</p>
          ) : (
            <ul className="ingredient-list">
              {recipe.ingredients.map((line, index) => (
                // Index as key: the same ingredient can appear on several lines
                <li key={index}>
                  <span className="amount">{formatAmount(line.amount, line.unit)}</span>{' '}
                  {line.name}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-labelledby="instructions-heading">
          <h2 id="instructions-heading">Instructions</h2>
          <Instructions text={recipe.instructions} />
        </section>
      </div>
    </article>
  )
}

// Each non-empty line is a step; a single line stays a paragraph
function Instructions({ text }) {
  const steps = (text ?? '').split('\n').map((line) => line.trim()).filter(Boolean)
  if (steps.length === 0) {
    return <p className="hint">No instructions yet.</p>
  }
  if (steps.length === 1) {
    return <p className="instructions">{steps[0]}</p>
  }
  return (
    <ol className="steps">
      {steps.map((step, index) => (
        <li key={index}>{step}</li>
      ))}
    </ol>
  )
}
