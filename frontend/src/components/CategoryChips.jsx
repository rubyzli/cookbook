import { Link } from 'react-router'

// Chips link to the recipe list filtered by that category, unless linked={false}
// (e.g. inside a card that is already a link, where nested links aren't allowed)
export default function CategoryChips({ categories, linked = true }) {
  if (categories.length === 0) return null
  return (
    <ul className="chips" aria-label="Categories">
      {categories.map((category) => (
        <li key={category.id}>
          {linked ? (
            <Link className="chip" to={`/?categoryId=${category.id}`}>
              {category.name}
            </Link>
          ) : (
            <span className="chip">{category.name}</span>
          )}
        </li>
      ))}
    </ul>
  )
}
