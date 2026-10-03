import de from './messages/de.js'
import en from './messages/en.js'
import hu from './messages/hu.js'

// Language names are shown in their own language, so people can find theirs
export const LANGUAGES = [
  { code: 'en', name: 'English' },
  { code: 'de', name: 'Deutsch' },
  { code: 'hu', name: 'Magyar' },
]

export const MESSAGES = { en, de, hu }

const STORAGE_KEY = 'cookbook.language'

// A saved choice wins; otherwise the first supported language the browser asks for; otherwise English
export function detectLanguage() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (MESSAGES[stored]) return stored
  } catch {
    // Storage can be blocked (private mode, settings); fall through to the browser language
  }
  for (const tag of navigator.languages ?? [navigator.language]) {
    const code = tag?.slice(0, 2).toLowerCase()
    if (MESSAGES[code]) return code
  }
  return 'en'
}

export function storeLanguage(code) {
  try {
    localStorage.setItem(STORAGE_KEY, code)
  } catch {
    // Not remembered across visits, but still switched for this one
  }
}

// Returns t(key, params). Messages can contain {placeholders}; a message given as
// { one, other, ... } is picked by params.count using the language's plural rules.
// Missing keys fall back to English, then to the key itself.
export function createTranslator(language) {
  const messages = MESSAGES[language] ?? en
  const plurals = new Intl.PluralRules(language)
  return function t(key, params = {}) {
    let message = messages[key] ?? en[key]
    if (message === undefined) return key
    if (typeof message === 'object') {
      message = message[plurals.select(params.count)] ?? message.other
    }
    return message.replace(/\{(\w+)\}/g, (match, name) => (name in params ? String(params[name]) : match))
  }
}
