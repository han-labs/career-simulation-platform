import { Languages } from 'lucide-react'
import { useI18n } from './useI18n.js'

export default function LanguageSwitcher() {
  const { locale, setLocale, t } = useI18n()
  return (
    <label className="language-switcher">
      <Languages size={17} aria-hidden="true" />
      <span className="sr-only">{t('Website language')}</span>
      <select aria-label={t('Website language')} value={locale} onChange={(event) => setLocale(event.target.value)}>
        <option value="en" lang="en">English</option>
        <option value="vi" lang="vi">Tiếng Việt</option>
      </select>
    </label>
  )
}
