import { useCallback, useEffect, useMemo, useState } from 'react'
import { I18nContext, LANGUAGE_KEY } from './context.js'
import { createTranslator } from './translate.js'

function storedLanguage() {
  try { return localStorage.getItem(LANGUAGE_KEY) === 'vi' ? 'vi' : 'en' } catch { return 'en' }
}

export default function I18nProvider({ children, initialLocale }) {
  const [locale, updateLocale] = useState(() => initialLocale === 'vi' ? 'vi' : initialLocale === 'en' ? 'en' : storedLanguage())
  const setLocale = useCallback((language) => {
    if (language !== 'en' && language !== 'vi') return
    updateLocale(language)
    try { localStorage.setItem(LANGUAGE_KEY, language) } catch { /* Preference remains usable in this tab. */ }
  }, [])
  const t = useMemo(() => createTranslator(locale), [locale])
  const value = useMemo(() => ({ locale, setLocale, t }), [locale, setLocale, t])
  useEffect(() => {
    document.documentElement.lang = locale
    document.title = `${t('Career Simulation Platform')} | HCM-UTE`
    document.querySelector('meta[name="description"]')?.setAttribute('content', t('Explore careers through practical tasks, career assessment and guidance from Ho Chi Minh City University of Technology and Engineering (HCM-UTE).'))
  }, [locale, t])
  useEffect(() => {
    const onStorage = (event) => {
      if (event.key === LANGUAGE_KEY) updateLocale(event.newValue === 'vi' ? 'vi' : 'en')
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
