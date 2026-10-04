import { Link } from 'react-router'
import { useI18n } from '../i18n/context.js'
import CategoryChips from './CategoryChips.jsx'
import RecipeImage from './RecipeImage.jsx'
import RecipeMeta from './RecipeMeta.jsx'

export default function RecipeCard({ recipe }) {
  const { t, language } = useI18n()
  // Shown in another language than the site's: no translation yet
  const otherLanguage = recipe.language && recipe.language !== language
  return (
    <Link to={`/recipes/${recipe.id}`} className="recipe-card" lang={recipe.language}>
      <div className="recipe-card-media">
        <RecipeImage recipe={recipe} className="recipe-card-image" />
        {otherLanguage && (
          <span className="lang-tag" title={t('translation.onlyIn', { language: t(`language.${recipe.language}`) })}>
            {recipe.language.toUpperCase()}
          </span>
        )}
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
