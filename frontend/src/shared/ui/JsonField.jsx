import { useState } from 'react'
import { useI18n } from '../i18n/useI18n.js'

export default function JsonField({ label, value, onChange, onEdit }) {
  const { t } = useI18n()
  const encoded = JSON.stringify(value, null, 2)
  const [local, setLocal] = useState({ source: encoded, text: encoded, valid: true })
  const text = local.source === encoded ? local.text : encoded
  return <label>{label}<textarea value={text} aria-invalid={local.source === encoded && !local.valid} onChange={e => { const next = e.target.value; onEdit?.(); try { const parsed = JSON.parse(next); setLocal({ source: JSON.stringify(parsed, null, 2), text: next, valid: true }); e.target.setCustomValidity(''); onChange(parsed) } catch { setLocal({ source: encoded, text: next, valid: false }); e.target.setCustomValidity(t('Invalid JSON')) } }} /></label>
}
