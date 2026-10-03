import { useEffect, useMemo, useState } from 'react'
import { setApiLanguage } from '../api/client.js'
import { I18nContext } from './context.js'
import { createTranslator, detectLanguage, storeLanguage } from './translate.js'

export default function I18nProvider({ initialLanguage, children }) {
  const [language, setLanguage] = useState(() => initialLanguage ?? detectLanguage())

  // Set during render, not in an effect: child components start their first requests
  // before a parent's effects run, and those requests should already carry the language
  setApiLanguage(language)

  const value = useMemo(
    () => ({
      language,
      t: createTranslator(language),
      setLanguage: (code) => {
        storeLanguage(code)
        setLanguage(code)
      },
    }),
    [language],
  )

  useEffect(() => {
    document.documentElement.lang = language
    document.title = value.t('app.title')
  }, [language, value])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
