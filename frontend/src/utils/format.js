// `t` is the translator from useI18n(); `language` its language code, used for number formatting

// 45 -> "45 min", 60 -> "1 h", 75 -> "1 h 15 min" (in the current language)
export function formatMinutes(minutes, t) {
  if (minutes === null || minutes === undefined) return null
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return t('duration.minutes', { m })
  return m === 0 ? t('duration.hours', { h }) : t('duration.hoursMinutes', { h, m })
}

// (250, "g") -> "250 g", (1.5, null) -> "1.5" ("1,5" in German and Hungarian), (null, "pinch") -> "pinch"
export function formatAmount(amount, unit, language) {
  const formatted =
    amount === null || amount === undefined
      ? null
      : new Intl.NumberFormat(language, { maximumFractionDigits: 2 }).format(amount)
  return [formatted, unit].filter(Boolean).join(' ')
}

export function totalMinutes(recipe) {
  const { prepTimeMinutes: prep, cookTimeMinutes: cook } = recipe
  if (prep == null && cook == null) return null
  return (prep ?? 0) + (cook ?? 0)
}
