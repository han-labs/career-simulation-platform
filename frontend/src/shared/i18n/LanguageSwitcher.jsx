import { useI18n } from './useI18n.js'

export default function LanguageSwitcher() {
  const { locale, setLocale, t } = useI18n()
  return (
    <label className="language-switcher">
      <span className="sr-only">{t('Website language')}</span>
      <select aria-label={t('Website language')} value={locale} onChange={(event) => setLocale(event.target.value)}>
        <option value="en" lang="en">EN</option>
        <option value="vi" lang="vi">VI</option>
      </select>
    </label>
  )
}
