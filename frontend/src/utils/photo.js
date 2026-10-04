// Makes a large photo smaller before uploading: phone photos are often 3-6 MB and 4000 px wide,
// far more than a recipe page needs. Returns the original file when shrinking isn't possible
// (an older browser, an unusual format), isn't needed, or wouldn't make it smaller.
export const MAX_PHOTO_SIZE = 1600

export async function shrinkPhoto(file, { maxSize = MAX_PHOTO_SIZE, quality = 0.85 } = {}) {
  // GIFs may be animated; re-encoding would keep only the first frame
  if (file.type === 'image/gif' || typeof createImageBitmap !== 'function') return file
  let bitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    return file
  }
  try {
    const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height))
    if (scale === 1 && file.size < 1_500_000) return file
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    const context = canvas.getContext('2d')
    if (!context) return file
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality))
    if (!blob || blob.size >= file.size) return file
    return new File([blob], file.name.replace(/\.[^.]+$/, '') + '.jpg', { type: 'image/jpeg' })
  } finally {
    bitmap.close?.()
  }
}
