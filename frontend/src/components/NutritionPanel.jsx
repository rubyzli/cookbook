import { useEstimateNutrition, useNutrition } from '../api/queries.js'
import { useI18n } from '../i18n/context.js'
import { errorMessage } from '../i18n/errors.js'

// Calories and macros estimated from the ingredients, per serving when the servings are known.
// Hidden while loading, and when there is no estimate and none can be made.
export default function NutritionPanel({ recipeId, servings }) {
  const { t, language } = useI18n()
  const nutrition = useNutrition(recipeId)
  const estimate = useEstimateNutrition(recipeId)
  const data = nutrition.data
  if (!data || (!data.estimate && !data.enabled)) return null

  const perServing = servings > 0
  const divisor = perServing ? servings : 1
  const number = new Intl.NumberFormat(language, { maximumFractionDigits: 0 })
  const value = (amount) => number.format(Math.round(amount / divisor))

  return (
    <section aria-labelledby="nutrition-heading" className="nutrition">
      <h2 id="nutrition-heading">{t('nutrition.title')}</h2>
      {data.estimate ? (
        <>
          <p className="nutrition-basis">{perServing ? t('nutrition.perServing') : t('nutrition.wholeRecipe')}</p>
          <dl className="nutrition-grid">
            <div className="nutrition-kcal">
              <dt>{t('nutrition.kcal')}</dt>
              <dd>{value(data.estimate.kcal)} kcal</dd>
            </div>
            <div>
              <dt>{t('nutrition.protein')}</dt>
              <dd>{value(data.estimate.proteinGrams)} g</dd>
            </div>
            <div>
              <dt>{t('nutrition.carbs')}</dt>
              <dd>{value(data.estimate.carbsGrams)} g</dd>
            </div>
            <div>
              <dt>{t('nutrition.fat')}</dt>
              <dd>{value(data.estimate.fatGrams)} g</dd>
            </div>
          </dl>
          {perServing && (
            <p className="hint">{t('nutrition.total', { kcal: number.format(data.estimate.kcal) })}</p>
          )}
          <p className="hint">{t('nutrition.estimated')}</p>
        </>
      ) : (
        <p className="hint">{t('nutrition.none')}</p>
      )}
      {data.outdated && <p className="hint">{t('nutrition.outdated')}</p>}
      {data.enabled && (!data.estimate || data.outdated) && (
        <button
          type="button"
          className="button small"
          onClick={() => estimate.mutate()}
          disabled={estimate.isPending}
        >
          {estimate.isPending
            ? t('nutrition.calculating')
            : data.estimate
              ? t('nutrition.recalculate')
              : t('nutrition.calculate')}
        </button>
      )}
      {estimate.isError && (
        <p className="field-error" role="alert">
          {t('nutrition.failed', { message: errorMessage(estimate.error, t) })}
        </p>
      )}
    </section>
  )
}
