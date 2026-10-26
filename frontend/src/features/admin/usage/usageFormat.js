export const number = (value, locale) => new Intl.NumberFormat(locale === 'vi' ? 'vi-VN' : 'en-US', { maximumFractionDigits: 1 }).format(value)
const validAmount = value => (typeof value === 'number' || (typeof value === 'string' && value.trim() !== '')) && Number.isFinite(Number(value)) && Number(value) >= 0
export const money = (value, locale, t) => !validAmount(value) ? t('Not available') : new Intl.NumberFormat(locale === 'vi' ? 'vi-VN' : 'en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 20 }).format(value)
export function executionCost(status, amount, locale, t) {
  if (status === 'NOT_APPLICABLE') return { value: 'N/A', label: '' }
  if (!['ESTIMATED', 'PARTIAL'].includes(status) || !validAmount(amount)) return { value:t('Not available'), label:'' }
  const label = status === 'PARTIAL' ? t('Partial estimate') + (Number(amount) === 0 ? ' · ' + t('Recorded estimate') : '') : t(Number(amount) === 0 ? 'Recorded estimate' : 'Estimated')
  return { value:money(amount,locale,t), label }
}
