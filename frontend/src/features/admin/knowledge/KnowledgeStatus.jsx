import { useI18n } from '../../../shared/i18n/useI18n.js'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'

const states = {
  ACTIVE: ['info', 'Active'], HIDDEN: ['neutral', 'Hidden'], ARCHIVED: ['neutral', 'Archived'],
  QUEUED: ['neutral', 'Queued'], PARSING: ['info', 'Parsing'], EMBEDDING: ['info', 'Embedding'],
  READY: ['warning', 'Ready for review'], PUBLISHED: ['success', 'Published'], FAILED: ['danger', 'Failed'],
  CANCELLED: ['neutral', 'Cancelled'], RETIRED: ['neutral', 'Retired'], UNCHANGED: ['info', 'Unchanged'],
  ACCEPTED: ['info', 'Accepted'], RUNNING: ['info', 'Running'], SUCCEEDED: ['success', 'Succeeded'],
}
export default function KnowledgeStatus({ value, label: displayLabel }) {
  const { t } = useI18n(), [tone, label] = states[value] ?? ['neutral', value ?? 'Not reported']
  return <StatusBadge tone={tone}>{t(displayLabel ?? label)}</StatusBadge>
}
