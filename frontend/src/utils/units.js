// Fixed rules for showing an ingredient unit in another language. Kept out of machine translation:
// abbreviations like "ek" or "EL" are easy for a translator to misread, and these never change.
//
// Each table maps a unit word (lower case) in the source language to the word in each target
// language. An empty string drops the unit ("1 fej vöröshagyma" reads "1 onion" in English).

const FROM_HU = {
  ek: { de: 'EL', en: 'tbsp' },
  evőkanál: { de: 'EL', en: 'tbsp' },
  kk: { de: 'TL', en: 'tsp' },
  tk: { de: 'TL', en: 'tsp' },
  kiskanál: { de: 'TL', en: 'tsp' },
  teáskanál: { de: 'TL', en: 'tsp' },
  mk: { de: 'Mokkalöffel', en: 'coffee spoon' },
  mokkáskanál: { de: 'Mokkalöffel', en: 'coffee spoon' },
  kanál: { de: 'Löffel', en: 'spoon' },
  'csapott kanál': { de: 'gestrichener Löffel', en: 'level spoon' },
  'púpozott kanál': { de: 'gehäufter Löffel', en: 'heaped spoon' },
  db: { de: 'Stk.', en: 'pcs' },
  'db közepes': { de: 'mittelgroße', en: 'medium' },
  fej: { de: '', en: '' },
  'kis fej': { de: 'kleine', en: 'small' },
  'nagy fej': { de: 'große', en: 'large' },
  gerezd: { de: 'Zehe', en: 'clove' },
  szál: { de: 'Stange', en: 'stick' },
  csomag: { de: 'Pck.', en: 'pack' },
  tasak: { de: 'Pck.', en: 'sachet' },
  zacskó: { de: 'Pck.', en: 'pack' },
  csipet: { de: 'Prise', en: 'pinch' },
  doboz: { de: 'Dose', en: 'can' },
  bögre: { de: 'Tasse', en: 'cup' },
  szelet: { de: 'Scheibe', en: 'slice' },
  'kb.': { de: 'ca.', en: 'approx.' },
}

const FROM_DE = {
  el: { hu: 'ek', en: 'tbsp' },
  esslöffel: { hu: 'ek', en: 'tbsp' },
  tl: { hu: 'kk', en: 'tsp' },
  teelöffel: { hu: 'kk', en: 'tsp' },
  msp: { hu: 'késhegynyi', en: 'pinch' },
  'msp.': { hu: 'késhegynyi', en: 'pinch' },
  löffel: { hu: 'kanál', en: 'spoon' },
  stk: { hu: 'db', en: 'pcs' },
  'stk.': { hu: 'db', en: 'pcs' },
  stück: { hu: 'db', en: 'pcs' },
  pck: { hu: 'csomag', en: 'pack' },
  'pck.': { hu: 'csomag', en: 'pack' },
  päckchen: { hu: 'csomag', en: 'pack' },
  prise: { hu: 'csipet', en: 'pinch' },
  zehe: { hu: 'gerezd', en: 'clove' },
  bund: { hu: 'csokor', en: 'bunch' },
  dose: { hu: 'doboz', en: 'can' },
  becher: { hu: 'pohár', en: 'pot' },
  tasse: { hu: 'bögre', en: 'cup' },
  scheibe: { hu: 'szelet', en: 'slice' },
  'ca.': { hu: 'kb.', en: 'approx.' },
}

const FROM_EN = {
  tbsp: { hu: 'ek', de: 'EL' },
  tablespoon: { hu: 'ek', de: 'EL' },
  tsp: { hu: 'kk', de: 'TL' },
  teaspoon: { hu: 'kk', de: 'TL' },
  cup: { hu: 'bögre', de: 'Tasse' },
  cups: { hu: 'bögre', de: 'Tassen' },
  pinch: { hu: 'csipet', de: 'Prise' },
  pcs: { hu: 'db', de: 'Stk.' },
  piece: { hu: 'db', de: 'Stk.' },
  clove: { hu: 'gerezd', de: 'Zehe' },
  cloves: { hu: 'gerezd', de: 'Zehen' },
  can: { hu: 'doboz', de: 'Dose' },
  slice: { hu: 'szelet', de: 'Scheibe' },
  pack: { hu: 'csomag', de: 'Pck.' },
  'approx.': { hu: 'kb.', de: 'ca.' },
}

const TABLES = { hu: FROM_HU, de: FROM_DE, en: FROM_EN }

// A number or range at the start of the unit field, as in "3–4 ek" or "1-1,5 kg"
const LEADING_AMOUNT = /^(\d+(?:[.,]\d+)?(?:\s*[–-]\s*\d+(?:[.,]\d+)?)?)\s+(.*)$/
const APPROX = /^(kb\.|ca\.|approx\.)\s+(.*)$/i

function translateWord(word, table, to) {
  const target = table[word.toLowerCase()]?.[to]
  return target === undefined ? word : target
}

function translateUnitText(unit, table, to) {
  const approx = unit.match(APPROX)
  if (approx) {
    return [translateWord(approx[1], table, to), translateUnitText(approx[2], table, to)].filter(Boolean).join(' ')
  }
  const leading = unit.match(LEADING_AMOUNT)
  if (leading) {
    return [leading[1], translateWord(leading[2], table, to)].filter(Boolean).join(' ')
  }
  return translateWord(unit, table, to)
}

// Returns { amount, unit } for showing a line written in `from` to a reader of `to`.
// Dekagrams (Hungarian) become grams in other languages.
export function translateUnit(amount, unit, from, to) {
  if (!unit || from === to || !TABLES[from]) return { amount, unit }
  if (unit.trim().toLowerCase() === 'dkg' && to !== 'hu') {
    return { amount: amount == null ? amount : Math.round(amount * 10 * 100) / 100, unit: 'g' }
  }
  const translated = translateUnitText(unit.trim(), TABLES[from], to)
  return { amount, unit: translated || null }
}
