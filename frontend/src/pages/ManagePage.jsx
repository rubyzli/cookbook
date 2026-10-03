import { useCategories, useIngredients } from '../api/queries.js'
import NameListSection from '../components/NameListSection.jsx'
import { useI18n } from '../i18n/context.js'

const CATEGORY_LABELS = {
  title: 'manage.categories',
  newItem: 'manage.newCategory',
  filter: 'manage.filterCategories',
  empty: 'manage.emptyCategories',
  taken: 'manage.categoryTaken',
  deleteConfirmUsed: 'manage.deleteCategoryConfirm',
}

const INGREDIENT_LABELS = {
  title: 'manage.ingredients',
  newItem: 'manage.newIngredient',
  filter: 'manage.filterIngredients',
  empty: 'manage.emptyIngredients',
  taken: 'manage.ingredientTaken',
}

export default function ManagePage() {
  const { t } = useI18n()
  const categories = useCategories()
  const ingredients = useIngredients()

  return (
    <>
      <h1 className="form-title">{t('manage.title')}</h1>
      <div className="manage-grid">
        {/* Deleting a category just untags its recipes; an ingredient in use can't be deleted */}
        <NameListSection kind="categories" query={categories} labels={CATEGORY_LABELS} linkToRecipes />
        <NameListSection kind="ingredients" query={ingredients} labels={INGREDIENT_LABELS} blockDeleteWhenUsed />
      </div>
    </>
  )
}
