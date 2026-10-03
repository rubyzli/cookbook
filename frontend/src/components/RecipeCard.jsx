import { Link } from 'react-router'
import CategoryChips from './CategoryChips.jsx'
import RecipeImage from './RecipeImage.jsx'
import RecipeMeta from './RecipeMeta.jsx'

export default function RecipeCard({ recipe }) {
  return (
    <Link to={`/recipes/${recipe.id}`} className="recipe-card">
      <RecipeImage recipe={recipe} className="recipe-card-image" />
      <div className="recipe-card-body">
        <h2>{recipe.name}</h2>
        {recipe.description && <p className="description">{recipe.description}</p>}
        <RecipeMeta recipe={recipe} />
        <CategoryChips categories={recipe.categories} linked={false} />
      </div>
    </Link>
  )
}
