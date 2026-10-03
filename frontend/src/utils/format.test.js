import { describe, expect, it } from 'vitest'
import { createTranslator } from '../i18n/translate.js'
import { formatAmount, formatMinutes, totalMinutes } from './format.js'

const en = createTranslator('en')

describe('formatMinutes', () => {
  it.each([
    [0, '0 min'],
    [45, '45 min'],
    [60, '1 h'],
    [75, '1 h 15 min'],
    [150, '2 h 30 min'],
  ])('formats %i as "%s"', (minutes, expected) => {
    expect(formatMinutes(minutes, en)).toBe(expected)
  })

  it('returns null when the time is unknown', () => {
    expect(formatMinutes(null, en)).toBeNull()
    expect(formatMinutes(undefined, en)).toBeNull()
  })

  it('uses the words of the given language', () => {
    expect(formatMinutes(75, createTranslator('de'))).toBe('1 Std. 15 Min.')
    expect(formatMinutes(75, createTranslator('hu'))).toBe('1 óra 15 perc')
  })
})

describe('formatAmount', () => {
  it('joins amount and unit', () => {
    expect(formatAmount(250, 'g', 'en')).toBe('250 g')
  })

  it('drops trailing zeros and keeps up to two decimals', () => {
    expect(formatAmount(1.5, 'cups', 'en')).toBe('1.5 cups')
    expect(formatAmount(0.333, null, 'en')).toBe('0.33')
  })

  it('handles a missing amount or unit', () => {
    expect(formatAmount(2, null, 'en')).toBe('2')
    expect(formatAmount(null, 'pinch', 'en')).toBe('pinch')
    expect(formatAmount(null, null, 'en')).toBe('')
  })

  it('uses a decimal comma in German and Hungarian', () => {
    expect(formatAmount(1.5, 'EL', 'de')).toBe('1,5 EL')
    expect(formatAmount(0.25, 'kg', 'hu')).toBe('0,25 kg')
  })
})

describe('totalMinutes', () => {
  it('adds prep and cook time', () => {
    expect(totalMinutes({ prepTimeMinutes: 20, cookTimeMinutes: 40 })).toBe(60)
  })

  it('counts a missing part as zero', () => {
    expect(totalMinutes({ prepTimeMinutes: 20, cookTimeMinutes: null })).toBe(20)
  })

  it('returns null when both are unknown', () => {
    expect(totalMinutes({ prepTimeMinutes: null, cookTimeMinutes: null })).toBeNull()
  })
})
