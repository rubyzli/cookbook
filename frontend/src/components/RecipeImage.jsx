// Shows the recipe photo, or a colored tile with the first letter when there is none
export default function RecipeImage({ recipe, className }) {
  if (recipe.imageUrl) {
    return <img className={className} src={recipe.imageUrl} alt="" />
  }
  return (
    <div className={`${className} image-placeholder`} aria-hidden="true">
      {recipe.name.charAt(0).toUpperCase()}
    </div>
  )
}
