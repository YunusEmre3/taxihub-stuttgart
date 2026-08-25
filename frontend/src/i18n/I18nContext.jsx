import { createContext, useCallback, useContext, useState } from 'react'
import { translations } from './translations'

const STORAGE_KEY = 'taxihub.locale'
const DEFAULT_LOCALE = 'tr'

const I18nContext = createContext(null)

function detectInitialLocale() {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (stored && translations[stored]) return stored
  } catch {
    /* private browsing / storage unavailable - fall back to default */
  }
  return DEFAULT_LOCALE
}

export function I18nProvider({ children }) {
  const [locale, setLocaleState] = useState(detectInitialLocale)

  const setLocale = useCallback((next) => {
    if (!translations[next]) return
    setLocaleState(next)
    try {
      window.localStorage.setItem(STORAGE_KEY, next)
    } catch {
      /* ignore */
    }
  }, [])

  const t = useCallback(
    (key, params) => {
      const dict = translations[locale] || translations[DEFAULT_LOCALE]
      let value = dict[key] ?? translations[DEFAULT_LOCALE][key] ?? key
      if (params) {
        Object.entries(params).forEach(([paramKey, paramValue]) => {
          value = value.replaceAll(`{${paramKey}}`, paramValue)
        })
      }
      return value
    },
    [locale],
  )

  return <I18nContext.Provider value={{ locale, setLocale, t }}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const ctx = useContext(I18nContext)
  if (!ctx) {
    throw new Error('useI18n must be used within I18nProvider')
  }
  return ctx
}
