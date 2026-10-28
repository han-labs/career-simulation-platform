import { apiRequest } from './httpClient.js'
import { createTranslator } from '../i18n/translate.js'

const vietnamese = createTranslator('vi')

export const coreEnabled = import.meta.env.VITE_CORE_V2_ENABLED === 'true'
export const explicitDemoFixtures = import.meta.env.DEV && import.meta.env.VITE_DEMO_FIXTURES === 'true'
export function command(path, body, revision, method = 'POST') {
  return apiRequest(path, { method, headers: { 'Content-Type': 'application/json', ...(revision === undefined ? {} : { 'If-Match': String(revision) }) }, body: JSON.stringify(body) })
}
export function localized(row, locale, field) { const value = row?.translations?.[locale]?.[field] ?? row?.[field] ?? ''; return locale === 'vi' ? vietnamese(value) : value }
export function draftKey(owner, kind, id) { return `careersim_core_${owner}_${kind}_${id}` }
export function readDraft(key) { try { return JSON.parse(localStorage.getItem(key) ?? 'null') } catch { return null } }
export function writeDraft(key, value) { try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* Server save remains available; keep in-memory work. */ } }
