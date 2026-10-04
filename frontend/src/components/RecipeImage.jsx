import { useState } from 'react'

// Shows the recipe photo, or a colored tile with the first letter when there is none
// or the photo can't be loaded (missing file, dead link)
export default function RecipeImage({ recipe, className }) {
  const [failedUrl, setFailedUrl] = useState(null)
  if (recipe.imageUrl && recipe.imageUrl !== failedUrl) {
    return <img className={className} src={recipe.imageUrl} alt="" onError={() => setFailedUrl(recipe.imageUrl)} />
  }
  return (
    <div className={`${className} image-placeholder`} aria-hidden="true">
      {recipe.name.charAt(0).toUpperCase()}
    </div>
  )
}
