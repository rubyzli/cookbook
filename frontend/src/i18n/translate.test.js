import { afterEach, describe, expect, it, vi } from 'vitest'
import { MESSAGES, createTranslator, detectLanguage, storeLanguage } from './translate.js'

const placeholders = (message) =>
  [...new Set(JSON.stringify(message).match(/\{\w+\}/g) ?? [])].sort()

describe('dictionaries', () => {
  const english = MESSAGES.en

  it.each(['de', 'hu'])('%s has exactly the English keys', (language) => {
    expect(Object.keys(MESSAGES[language]).sort()).toEqual(Object.keys(english).sort())
  })

  it.each(['de', 'hu'])('%s uses the same placeholders as English', (language) => {
    for (const [key, message] of Object.entries(english)) {
      expect([key, placeholders(MESSAGES[language][key])]).toEqual([key, placeholders(message)])
    }
  })

  it.each(['en', 'de', 'hu'])('%s plural entries cover what the language needs', (language) => {
    const needed = new Intl.PluralRules(language).resolvedOptions().pluralCategories
    for (const [key, message] of Object.entries(MESSAGES[language])) {
      if (typeof message !== 'object') continue
      for (const category of needed) {
        expect([key, category, typeof (message[category] ?? message.other)]).toEqual([key, category, 'string'])
      }
    }
  })
})

describe('createTranslator', () => {
  it('fills in placeholders', () => {
    expect(createTranslator('en')('form.editTitle', { name: 'Pie' })).toBe('Edit Pie')
    expect(createTranslator('de')('form.editTitle', { name: 'Pie' })).toBe('Pie bearbeiten')
  })

  it('picks plural forms by count', () => {
    const en = createTranslator('en')
    expect(en('list.count', { count: 1 })).toBe('1 recipe')
    expect(en('list.count', { count: 2 })).toBe('2 recipes')
    const de = createTranslator('de')
    expect(de('list.count', { count: 1 })).toBe('1 Rezept')
    expect(de('list.count', { count: 2 })).toBe('2 Rezepte')
    const hu = createTranslator('hu')
    expect(hu('list.count', { count: 1 })).toBe('1 recept')
    expect(hu('list.count', { count: 2 })).toBe('2 recept')
  })

  it('falls back to English, then to the key', () => {
    expect(createTranslator('fr')('nav.recipes')).toBe('Recipes')
    expect(createTranslator('de')('no.such.key')).toBe('no.such.key')
  })
})

describe('detectLanguage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('prefers a saved choice', () => {
    storeLanguage('hu')
    expect(detectLanguage()).toBe('hu')
  })

  it('uses the first supported browser language', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR', 'de-AT', 'en'])
    expect(detectLanguage()).toBe('de')
  })

  it('falls back to English', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR', 'es'])
    expect(detectLanguage()).toBe('en')
  })

  it('ignores a saved value that is not a supported language', () => {
    localStorage.setItem('cookbook.language', 'xx')
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['hu-HU'])
    expect(detectLanguage()).toBe('hu')
  })
})
