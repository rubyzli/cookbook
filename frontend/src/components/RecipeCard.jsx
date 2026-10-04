import { Link } from 'react-router'
import CategoryChips from './CategoryChips.jsx'
import RecipeImage from './RecipeImage.jsx'
import RecipeMeta from './RecipeMeta.jsx'

export default function RecipeCard({ recipe }) {
  return (
    <Link to={`/recipes/${recipe.id}`} className="recipe-card">
      <div className="recipe-card-media">
        <RecipeImage recipe={recipe} className="recipe-card-image" />
      </div>
      <div className="recipe-card-body">
        <CategoryChips categories={recipe.categories} linked={false} />
        <h2>{recipe.name}</h2>
        {recipe.description && <p className="description">{recipe.description}</p>}
        <RecipeMeta recipe={recipe} />
      </div>
    </Link>
  )
}
