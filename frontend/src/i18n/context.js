import { createContext, useContext } from 'react'

export const I18nContext = createContext(null)

// { language, setLanguage, t } for the current language
export function useI18n() {
  return useContext(I18nContext)
}
