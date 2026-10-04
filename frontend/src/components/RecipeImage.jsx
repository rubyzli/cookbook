import { useState } from 'react'
import { apiUrl } from '../api/client.js'

// Shows the recipe photo, or a colored tile with the first letter when there is none
// or the photo can't be loaded (missing file, dead link).
export default function RecipeImage({ recipe, className }) {
  const [failedUrl, setFailedUrl] = useState(null)

  const imageUrl = recipe.imageUrl ? apiUrl(recipe.imageUrl) : null

  if (imageUrl && imageUrl !== failedUrl) {
    return (
        <img
            className={className}
            src={imageUrl}
            alt=""
            onError={() => setFailedUrl(imageUrl)}
        />
    )
  }

  return (
      <div
          className={`${className} image-placeholder`}
          aria-hidden="true"
      >
        {recipe.name.charAt(0).toUpperCase()}
      </div>
  )
}