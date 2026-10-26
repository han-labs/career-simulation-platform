import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import StatusBadge from '../../../shared/ui/StatusBadge.jsx'
import { agentPath, platformCommand } from '../agentPlatform/platformApi.js'
import StudentExperiencePreview from './StudentExperiencePreview.jsx'

export default function PublishAssignPanel({ active, agent, selected, published, dirty, execution, onChanged }) {
  const { t } = useI18n(), root = agentPath(agent.id)
  const [query, setQuery] = useState(''), [students, setStudents] = useState(null), [searchError, setSearchError] = useState(''), [searching, setSearching] = useState(false)
  const [choice, setChoice] = useState(''), [confirmation, setConfirmation] = useState(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const alive = useRef(false), pending = useRef(false), searchSequence = useRef(0)
  const cursorQuery = useRef('')
  useEffect(() => { alive.current = true; return () => { alive.current = false; searchSequence.current += 1 } }, [])
  const refresh = () => setRevision(value => value + 1)
  const configuration = selected?.id, hash = selected?.contentHash
  const previewRevision = `${execution?.id ?? ''}:${execution?.status ?? ''}`
  const readinessKey = `${hash}:${agent.revision}:${previewRevision}:${revision}`
  const { value: readiness, error: readinessError } = useAdminRead(`${root}/configurations/${configuration}/publication-readiness`, active && Boolean(configuration), readinessKey)
  const { value: assignments, error: assignmentError } = useAdminRead(`${root}/assignments`, active, revision)
  async function search(text, after = null) {
    const sequence = ++searchSequence.current
    setSearching(true); setSearchError(''); setChoice('')
    if (!after) setStudents(null)
    try {
      const params = new URLSearchParams({ q: text, limit: '20', ...(after ? { afterId: after } : {}) })
      const page = await apiRequest(`${root}/eligible-students?${params}`)
      if (!alive.current || sequence !== searchSequence.current) return
      cursorQuery.current = text
      setStudents(previous => ({ ...page, items: after ? [...(previous?.items ?? []), ...page.items] : page.items }))
    } catch (failure) { if (alive.current && sequence === searchSequence.current) setSearchError(failure.message) }
    finally { if (alive.current && sequence === searchSequence.current) setSearching(false) }
  }
  useEffect(() => {
    if (!active) return undefined
    const sequence = ++searchSequence.current
    apiRequest(`${root}/eligible-students?${new URLSearchParams({ q: '', limit: '20' })}`)
      .then(page => { if (alive.current && sequence === searchSequence.current) { cursorQuery.current = ''; setStudents(page); setSearchError(''); setSearching(false); setChoice('') } })
      .catch(failure => { if (alive.current && sequence === searchSequence.current) { setSearchError(failure.message); setSearching(false) } })
    return () => { searchSequence.current += 1 }
  }, [active, root])
  const currentReadiness = readiness?.configurationId === configuration && readiness?.configurationHash === hash && readiness?.agentRevision === agent.revision ? readiness : null
  const canPublish = !dirty && currentReadiness?.publishable && selected?.status === 'SAVED'
  const selectedStudent = students?.items.find(item => item.id === choice)
  const alreadyAssigned = assignments?.some(item => item.studentUserId === choice && item.status === 'ACTIVE')
  async function confirm() {
    if (pending.current || !confirmation) return
    const operation = confirmation
    if (operation.kind === 'publish' && (!canPublish || configuration !== operation.configId || hash !== operation.hash || agent.revision !== operation.revision)) {
      setConfirmation(null); setError('Publication state changed. Refresh readiness before publishing.'); return
    }
    pending.current = true; setBusy(true); setError(''); setNotice('')
    try {
      if (operation.kind === 'publish') await platformCommand(`${root}/configurations/${operation.configId}/publish`, { confirmed: true }, operation.revision)
      else if (operation.kind === 'assign') await platformCommand(`${root}/assignments`, { studentUserId: operation.student.id })
      else await platformCommand(`${root}/assignments/${operation.assignment.id}/revoke`, {})
      if (!alive.current) return
      setConfirmation(null); setChoice('')
      setNotice(operation.kind === 'publish' ? 'Publication saved. No Students were assigned automatically.' : operation.kind === 'assign' ? 'Assignment is active. No Student conversation was created.' : 'Assignment revoked. History is preserved.')
      refresh(); await onChanged()
    } catch (failure) {
      if (alive.current) { setConfirmation(null); setError(failure.message); refresh() }
    } finally { pending.current = false; if (alive.current) setBusy(false) }
  }
  const version = selected ? `${t('Version')} ${selected.versionNumber}` : t('None selected')
  const checks = [
    ['Configuration saved', Boolean(selected)],
    ['No unsaved changes', !dirty],
    ['Agent is not archived', currentReadiness?.agentEditable],
    ['Knowledge grants available', currentReadiness?.knowledgeAvailable],
    ['Successful Preview of this exact saved configuration', currentReadiness?.previewSucceeded],
    ['Version eligible for publication', currentReadiness?.publishable],
  ]
  return <div className="cs-stack agent-publication-panel">
    <section className="cs-surface cs-stack" aria-label={t('Publication')}><div className="cs-section-header"><h3>{t('Publication')}</h3><Button disabled={busy} onClick={refresh}>{t('Refresh status')}</Button></div>
      <dl className="cs-metadata"><div><dt>{t('Selected saved version')}</dt><dd>{version} · {selected?.status ?? t('Not saved')}</dd></div><div><dt>{t('Published version')}</dt><dd>{published ? `${t('Version')} ${published.versionNumber}` : agent.publishedConfigurationId ? t('Published version unavailable') : t('Not published')}</dd></div></dl>
      {!selected && <FeedbackPanel kind="info">{t('Save a configuration before publication.')}</FeedbackPanel>}
      {dirty && <FeedbackPanel kind="warning">{t('Unsaved changes must be saved and previewed before publication.')}</FeedbackPanel>}
      {selected && !readiness && !readinessError && <FeedbackPanel kind="loading">{t('Checking publication readiness...')}</FeedbackPanel>}
      {readinessError && <FeedbackPanel kind="error">{t(readinessError)}</FeedbackPanel>}
      <ul className="agent-readiness" aria-label={t('Publication readiness')}>{checks.map(([label, value]) => <li key={label}><span>{t(label)}</span><StatusBadge tone={value ? 'success' : value === false ? 'warning' : 'neutral'}>{t(value ? 'Satisfied' : value === false ? 'Blocked' : 'Not verified')}</StatusBadge></li>)}</ul>
      {currentReadiness?.previewSucceeded === false && <p className="cs-caption">{t('Run a successful Admin Preview for this exact version. Failed or unavailable Preview does not qualify.')}</p>}
      <p className="cs-caption">{t('The backend rechecks publication readiness and revision at confirmation. Publishing does not assign Students.')}</p>
      <div className="cs-actions"><Button variant="primary" disabled={busy || !canPublish} onClick={() => setConfirmation({ kind: 'publish', configId: configuration, hash, revision: agent.revision, version: selected.versionNumber })}>{t('Publish version')}{selected ? ` ${selected.versionNumber}` : ''}</Button></div>
      {selected?.status === 'PUBLISHED' && <p role="status">{t('This saved version is published.')}</p>}
    </section>
    <section className="cs-surface cs-stack" aria-label={t('Assignments')}><h3>{t('Assignments')}</h3><p className="cs-caption">{t('Search active Students with a Student profile. AI consent is not an assignment requirement. Access requires an active, published Agent and an explicit assignment.')}</p>
      {agent.status !== 'ACTIVE' && <FeedbackPanel kind="info">{t('Publish and activate this Agent before assigning Students.')}</FeedbackPanel>}
      <form onSubmit={event => { event.preventDefault(); search(query.trim()) }} className="agent-student-search"><FormField label={t('Search Students by name')}><input maxLength={80} value={query} onChange={event => { setQuery(event.target.value); setChoice('') }} /></FormField><Button type="submit" disabled={searching || busy}>{t('Search Students')}</Button></form>
      {(searching || (!students && !searchError)) && <FeedbackPanel kind="loading">{t('Loading eligible Students...')}</FeedbackPanel>}
      {searchError && <FeedbackPanel kind="error">{t(searchError)} <Button disabled={searching} onClick={() => search(query.trim())}>{t('Retry Student search')}</Button></FeedbackPanel>}
      {students?.items.length === 0 && !searching && <FeedbackPanel kind="empty">{t('No eligible Students match this search.')}</FeedbackPanel>}
      {Boolean(students?.items.length) && <FormField label={t('Eligible Student')}><select value={choice} disabled={busy || searching} onChange={event => setChoice(event.target.value)}><option value="">{t('Choose a Student')}</option>{students.items.map(item => <option key={item.id} value={item.id}>{item.displayName} · {t('Account ID')} {item.id}</option>)}</select></FormField>}
      {students?.nextAfterId && <Button disabled={busy || searching} onClick={() => search(cursorQuery.current, students.nextAfterId)}>{t('Load more Students')}</Button>}
      {alreadyAssigned && <p role="status">{t('This Student already has an active assignment.')}</p>}
      <div className="cs-actions"><Button disabled={busy || searching || !selectedStudent || alreadyAssigned || !assignments || agent.status !== 'ACTIVE' || !agent.publishedConfigurationId} onClick={() => setConfirmation({ kind: 'assign', student: selectedStudent })}>{t('Assign selected Student')}</Button></div>
      <h4>{t('Assigned Students')}</h4>
      {!assignments && !assignmentError && <FeedbackPanel kind="loading">{t('Loading assignments...')}</FeedbackPanel>}
      {assignmentError && <FeedbackPanel kind="error">{t(assignmentError)} <Button disabled={busy} onClick={refresh}>{t('Retry assignments')}</Button></FeedbackPanel>}
      {assignments?.length === 0 && <FeedbackPanel kind="empty">{t('No Student assignments yet.')}</FeedbackPanel>}
      <ul className="agent-assignment-list">{assignments?.map(item => <li key={item.id}><div><strong>{item.studentDisplayName ?? t('Student')}</strong><p className="cs-caption">{t('Account ID')} {item.studentUserId}{item.assignedAt ? ` · ${t('Assigned at')}: ${new Date(item.assignedAt).toLocaleString()}` : ''}</p></div><StatusBadge tone={item.status === 'ACTIVE' ? 'success' : 'neutral'}>{item.status}</StatusBadge>{item.status === 'ACTIVE' && <Button variant="danger" disabled={busy} onClick={() => setConfirmation({ kind: 'revoke', assignment: item })}>{t('Revoke assignment')}</Button>}</li>)}</ul>
      <p className="cs-caption">{t('Assignments grant Agent access; they do not pin a separate configuration version. The published Agent version is shown above.')}</p>
    </section>
    {active && <StudentExperiencePreview agent={agent} selected={selected} published={published} execution={execution} />}
    {error && <FeedbackPanel kind="error">{t(error)}</FeedbackPanel>}{notice && <FeedbackPanel kind="success">{t(notice)}</FeedbackPanel>}
    {confirmation && <Dialog title={t(confirmation.kind === 'publish' ? 'Confirm publication' : confirmation.kind === 'assign' ? 'Confirm Student assignment' : 'Confirm assignment revocation')} closeLabel={t('Close dialog')} busy={busy} onRequestClose={() => { if (!pending.current) setConfirmation(null) }} footer={<><Button disabled={busy} onClick={() => setConfirmation(null)}>{t('Cancel')}</Button><Button variant={confirmation.kind === 'revoke' ? 'danger' : 'primary'} busy={busy} onClick={confirm}>{t(confirmation.kind === 'publish' ? 'Confirm publish' : confirmation.kind === 'assign' ? 'Confirm assignment' : 'Confirm revoke')}</Button></>}>
      <p><strong>{agent.name}</strong> · {confirmation.kind === 'publish' ? `${t('Version')} ${confirmation.version}` : `${confirmation.student?.displayName ?? confirmation.assignment?.studentDisplayName ?? t('Student')} · ${t('Account ID')} ${confirmation.student?.id ?? confirmation.assignment?.studentUserId}`}</p>
      <p>{t(confirmation.kind === 'publish' ? 'Publish this exact saved version? No Student access is granted automatically.' : confirmation.kind === 'assign' ? 'Grant this Student access to the published Agent? No Student conversation will be created.' : 'Revoke this Student’s Agent access? Assignment history will remain available.')}</p>
    </Dialog>}
  </div>
}

// Key every read to its Agent/configuration/revision. Obsolete responses never qualify readiness.
function useAdminRead(path, enabled, revision) {
  const [snapshot, setSnapshot] = useState(null)
  const key = `${path}:${revision}`
  useEffect(() => {
    if (!enabled) return undefined
    let current = true
    apiRequest(path).then(value => { if (current) setSnapshot({ key, value, error: '' }) })
      .catch(failure => { if (current) setSnapshot({ key, value: null, error: failure.message }) })
    return () => { current = false }
  }, [path, enabled, key])
  return snapshot?.key === key ? snapshot : { value: null, error: '' }
}
