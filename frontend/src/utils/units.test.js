import { describe, expect, it } from 'vitest'
import { translateUnit } from './units.js'

describe('translateUnit', () => {
  it.each([
    ['ek', 'de', 'EL'],
    ['ek', 'en', 'tbsp'],
    ['kk', 'de', 'TL'],
    ['db', 'de', 'Stk.'],
    ['csomag', 'en', 'pack'],
    ['púpozott kanál', 'de', 'gehäufter Löffel'],
    ['csapott kanál', 'en', 'level spoon'],
    ['kg', 'de', 'kg'],
    ['g', 'en', 'g'],
  ])('Hungarian "%s" in %s is "%s"', (unit, to, expected) => {
    expect(translateUnit(1, unit, 'hu', to).unit).toBe(expected)
  })

  it('turns dekagrams into grams outside Hungarian', () => {
    expect(translateUnit(25, 'dkg', 'hu', 'de')).toEqual({ amount: 250, unit: 'g' })
    expect(translateUnit(33.5, 'dkg', 'hu', 'en')).toEqual({ amount: 335, unit: 'g' })
    expect(translateUnit(null, 'dkg', 'hu', 'en')).toEqual({ amount: null, unit: 'g' })
  })

  it('drops "fej", so "1 fej vöröshagyma" reads "1 onion"', () => {
    expect(translateUnit(1, 'fej', 'hu', 'en')).toEqual({ amount: 1, unit: null })
    expect(translateUnit(2, 'kis fej', 'hu', 'de')).toEqual({ amount: 2, unit: 'kleine' })
  })

  it('keeps ranges and "about" in front of the unit', () => {
    expect(translateUnit(null, '3–4 ek', 'hu', 'de').unit).toBe('3–4 EL')
    expect(translateUnit(null, '1–1,5 kg', 'hu', 'en').unit).toBe('1–1,5 kg')
    expect(translateUnit(null, 'kb. 1 dl', 'hu', 'de').unit).toBe('ca. 1 dl')
  })

  it('translates German and English units too', () => {
    expect(translateUnit(2, 'EL', 'de', 'hu').unit).toBe('ek')
    expect(translateUnit(1, 'Prise', 'de', 'en').unit).toBe('pinch')
    expect(translateUnit(1, 'tsp', 'en', 'de').unit).toBe('TL')
  })

  it('leaves unknown units and same-language lines alone', () => {
    expect(translateUnit(1, 'marék', 'hu', 'de')).toEqual({ amount: 1, unit: 'marék' })
    expect(translateUnit(1, 'ek', 'hu', 'hu')).toEqual({ amount: 1, unit: 'ek' })
    expect(translateUnit(1, null, 'hu', 'de')).toEqual({ amount: 1, unit: null })
  })
})
