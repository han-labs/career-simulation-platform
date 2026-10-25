// Presentation helpers for the existing UC-07 contracts. No inferred server fields.
export const MAX_SOURCE_BYTES = 8 * 1024 * 1024 // KnowledgeParser.MAX_BYTES
export const SOURCE_ACCEPT = '.txt,.md,.pdf,.docx,.xlsx'

export const SOURCE_RIGHTS = [
  { value: 'internal', label: 'Internal / organization-owned document' },
  { value: 'public', label: 'Public or official source' },
  { value: 'permission', label: 'Permission granted' },
]
export function sourceRightsChoice(draft) {
  return draft.rightsChoice ?? SOURCE_RIGHTS.find(option => option.label === draft.license)?.value ?? (draft.license ? 'other' : '')
}

export function sourceFileError(files) {
  if (files.length !== 1) return 'Choose one file per upload.'
  const file = files[0]
  if (!/\.(txt|md|pdf|docx|xlsx)$/i.test(file.name)) return 'Choose a TXT, MD, PDF, DOCX or XLSX file.'
  if (!file.size || file.size > MAX_SOURCE_BYTES) return 'Choose a non-empty file up to 8 MiB (8,388,608 bytes).'
  return ''
}

export function selectSource(file, previous = {}) {
  const fileIdentity = JSON.stringify([file.name, file.size, file.lastModified, file.type])
  if (previous.fileIdentity === fileIdentity && previous.assetKey && previous.requestId) return { ...previous, reviewed: false }
  // A new file after an uploaded replacement is another document, unless Replace
  // was explicitly selected again. Same-file reselection above keeps its receipt.
  const replacementKey = previous.stagedJob ? undefined : previous.replacementKey
  const title = file.name.replace(/\.[^.]+$/, '').trim().slice(0, 240) || file.name.slice(0, 240)
  const prefix = title.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 60) || 'document'
  return { assetKey: replacementKey || prefix + '-' + crypto.randomUUID(), title,
    license: replacementKey ? previous.license : '', track: replacementKey ? previous.track : '',
    language: replacementKey ? previous.language : '', sourceUri: replacementKey ? previous.sourceUri : '',
    reviewed: false, requestId: crypto.randomUUID(), fileIdentity, replacementKey }
}

export function uploadInput(draft) {
  const { assetKey, title, license, track, language, reviewed, requestId } = draft
  return { assetKey, title, license, track, language, reviewed, requestId, sourceUri: draft.sourceUri || null }
}

export function validUpload(draft) {
  if (!draft.reviewed || !/^[a-z0-9][a-z0-9-]{0,99}$/.test(draft.assetKey ?? '') || !draft.requestId) return false
  if (![draft.title, draft.license, draft.track].every(value => value?.trim()) || !['EN', 'VI'].includes(draft.language)) return false
  if (draft.sourceUri) {
    try { const uri = new URL(draft.sourceUri); if (uri.protocol !== 'https:' || !uri.hostname || uri.username || uri.password || draft.sourceUri.length > 600) return false } catch { return false }
  }
  return draft.title.length <= 240 && draft.license.length <= 240 && draft.track.length <= 60
}

export function groupDocuments(versions, jobs, kb) {
  const groups = new Map()
  function group(record) {
    if (!record.assetKey || (record.knowledgeBaseId && record.knowledgeBaseId !== kb)) return null
    if (!groups.has(record.assetKey)) groups.set(record.assetKey, { assetKey: record.assetKey, versions: [], jobs: [] })
    return groups.get(record.assetKey)
  }
  versions.forEach(version => group(version)?.versions.push(version))
  jobs.forEach(job => group(job)?.jobs.push(job)) // API returns newest createdAt first.
  return [...groups.values()].filter(row => row.versions.length
    ? row.versions.some(v => v.status !== 'RETIRED') || row.jobs.some(j => !['CANCELLED', 'READY', 'PUBLISHED'].includes(j.state))
    : row.jobs.some(j => j.state !== 'CANCELLED')).map(row => {
    row.versions.sort((a, b) => Number(b.versionNumber ?? b.id) - Number(a.versionNumber ?? a.id))
    row.job = row.jobs[0]
    row.published = row.versions.find(version => version.status === 'PUBLISHED')
    row.linked = row.job?.producedVersionId
      ? row.versions.find(version => row.job.producedVersionId === version.id)
      : row.versions.find(version => row.job?.sourceFileId && row.job.sourceFileId === version.sourceFileId)
    row.latest = row.versions[0]
    row.current = row.versions.find(version => ['READY', 'PUBLISHED'].includes(version.status)) ?? row.latest
    row.ready = row.current?.status === 'READY' ? row.current : null
    row.name = row.job?.metadata?.title || row.latest?.title || row.job?.metadata?.originalName || row.assetKey
    row.state = row.job?.state ?? row.current?.status ?? 'QUEUED'
    if (['READY', 'PUBLISHED'].includes(row.state)) row.state = row.current?.status ?? row.state
    return row
  })
}
