import { formatMinutes, totalMinutes } from '../utils/format.js'

// "Prep 30 min · Cook 45 min · Total 1 h 15 min · Serves 8", leaving out whatever is unknown
export default function RecipeMeta({ recipe, showTotal = false }) {
  const items = [
    ['Prep', formatMinutes(recipe.prepTimeMinutes)],
    ['Cook', formatMinutes(recipe.cookTimeMinutes)],
    showTotal && recipe.prepTimeMinutes != null && recipe.cookTimeMinutes != null
      ? ['Total', formatMinutes(totalMinutes(recipe))]
      : null,
    ['Serves', recipe.servings],
  ].filter((item) => item && item[1] != null)

  if (items.length === 0) return null
  return (
    <dl className="meta">
      {items.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
