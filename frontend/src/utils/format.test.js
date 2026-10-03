import { describe, expect, it } from 'vitest'
import { formatAmount, formatMinutes, totalMinutes } from './format.js'

describe('formatMinutes', () => {
  it.each([
    [0, '0 min'],
    [45, '45 min'],
    [60, '1 h'],
    [75, '1 h 15 min'],
    [150, '2 h 30 min'],
  ])('formats %i as "%s"', (minutes, expected) => {
    expect(formatMinutes(minutes)).toBe(expected)
  })

  it('returns null when the time is unknown', () => {
    expect(formatMinutes(null)).toBeNull()
    expect(formatMinutes(undefined)).toBeNull()
  })
})

describe('formatAmount', () => {
  it('joins amount and unit', () => {
    expect(formatAmount(250, 'g')).toBe('250 g')
  })

  it('drops trailing zeros and keeps up to two decimals', () => {
    expect(formatAmount(1.5, 'cups')).toBe('1.5 cups')
    expect(formatAmount(0.333, null)).toBe('0.33')
  })

  it('handles a missing amount or unit', () => {
    expect(formatAmount(2, null)).toBe('2')
    expect(formatAmount(null, 'pinch')).toBe('pinch')
    expect(formatAmount(null, null)).toBe('')
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
