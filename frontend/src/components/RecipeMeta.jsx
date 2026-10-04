import { useI18n } from '../i18n/context.js'
import { formatMinutes, totalMinutes } from '../utils/format.js'
import Icon from './Icon.jsx'

// "Prep 30 min · Cook 45 min · Total 1 h 15 min · Serves 8", leaving out whatever is unknown.
// `pills` shows each item as a rounded pill, for the recipe page.
export default function RecipeMeta({ recipe, showTotal = false, pills = false }) {
  const { t } = useI18n()
  const items = [
    ['prep', 'clock', t('meta.prep'), formatMinutes(recipe.prepTimeMinutes, t)],
    ['cook', 'clock', t('meta.cook'), formatMinutes(recipe.cookTimeMinutes, t)],
    showTotal && recipe.prepTimeMinutes != null && recipe.cookTimeMinutes != null
      ? ['total', 'clock', t('meta.total'), formatMinutes(totalMinutes(recipe), t)]
      : null,
    ['serves', 'users', t('meta.serves'), recipe.servings],
  ].filter((item) => item && item[3] != null)

  if (items.length === 0) return null
  return (
    <dl className={pills ? 'meta meta-pills' : 'meta'}>
      {items.map(([key, icon, label, value]) => (
        <div key={key}>
          <Icon name={icon} />
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
