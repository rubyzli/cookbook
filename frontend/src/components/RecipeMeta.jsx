import { useI18n } from '../i18n/context.js'
import { formatMinutes, totalMinutes } from '../utils/format.js'

// "Prep 30 min · Cook 45 min · Total 1 h 15 min · Serves 8", leaving out whatever is unknown
export default function RecipeMeta({ recipe, showTotal = false }) {
  const { t } = useI18n()
  const items = [
    ['prep', t('meta.prep'), formatMinutes(recipe.prepTimeMinutes, t)],
    ['cook', t('meta.cook'), formatMinutes(recipe.cookTimeMinutes, t)],
    showTotal && recipe.prepTimeMinutes != null && recipe.cookTimeMinutes != null
      ? ['total', t('meta.total'), formatMinutes(totalMinutes(recipe), t)]
      : null,
    ['serves', t('meta.serves'), recipe.servings],
  ].filter((item) => item && item[2] != null)

  if (items.length === 0) return null
  return (
    <dl className="meta">
      {items.map(([key, label, value]) => (
        <div key={key}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
