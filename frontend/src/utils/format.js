const amountFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 })

// 45 -> "45 min", 60 -> "1 h", 75 -> "1 h 15 min"
export function formatMinutes(minutes) {
  if (minutes === null || minutes === undefined) return null
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} min`
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`
}

// (250, "g") -> "250 g", (1.5, null) -> "1.5", (null, "pinch") -> "pinch"
export function formatAmount(amount, unit) {
  return [amount === null || amount === undefined ? null : amountFormat.format(amount), unit]
    .filter(Boolean)
    .join(' ')
}

export function totalMinutes(recipe) {
  const { prepTimeMinutes: prep, cookTimeMinutes: cook } = recipe
  if (prep == null && cook == null) return null
  return (prep ?? 0) + (cook ?? 0)
}
