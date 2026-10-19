import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { apiRequest, API_BASE_URL } from '../../../shared/api/httpClient.js'
import { command, draftKey, localized, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SubmissionConfirmationDialog from '../components/SubmissionConfirmationDialog.jsx'

function choiceDefinition(task, d) { const properties = d.validationConfig?.jsonSchema?.properties; return task.taskType === 'QUIZ' && properties && Object.keys(properties).length > 0 && Object.values(properties).every(p => p.type === 'string' && Array.isArray(p.enum)) }
function resourceLink(resource) { return resource.fileAssetId ? `${API_BASE_URL}/v2/files/${resource.fileAssetId}/download?useType=TASK_RESOURCE&useId=${resource.id}` : resource.externalUrl }
export default function CoreSimulationPage() {
  const { t, locale } = useI18n(), { slug } = useParams(), [search, setSearch] = useSearchParams()
  const enrollmentId = search.get('enrollment')
  const [catalog, setCatalog] = useState(null), [enrollment, setEnrollment] = useState(null), [owner, setOwner] = useState(null)
  const [taskId, setTaskId] = useState(null), [submission, setSubmission] = useState(null), [values, setValues] = useState({})
  const [released, setReleased] = useState(null), [completion, setCompletion] = useState(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [confirmFinal, setConfirmFinal] = useState(false)
  useEffect(() => { let active = true
    const request = enrollmentId ? Promise.all([apiRequest('/v2/me'), apiRequest(`/v2/enrollments/${enrollmentId}`)]) : apiRequest(`/v2/scenarios/${encodeURIComponent(slug)}`).then(c => [null, c])
    request.then(([me, data]) => { if (!active) return; if (me) { setOwner(me.id); setEnrollment(data); setTaskId(data.progression?.find(p => p.accessible && !p.reviewed)?.taskId ?? data.tasks[0]?.id) } else setCatalog(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [slug, enrollmentId])
  useEffect(() => { if (!owner || !enrollmentId || !taskId) return; let active = true
    apiRequest(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/submission`).then(s => {
      if (!active) return; setSubmission(s); setReleased(null)
      const local = readDraft(draftKey(owner, 'task', `${enrollmentId}_${taskId}`))
      const saved = Object.fromEntries(s.items.map(i => [i.definitionId, i.contentJson ? { ...i, jsonText: JSON.stringify(i.contentJson, null, 2) } : i]))
      setValues(s.status === 'FINAL' ? saved : { ...saved, ...local?.values })
    }).catch(e => { if (active) setError(e.message) }); return () => { active = false }
  }, [owner, enrollmentId, taskId])
  useEffect(() => { if (enrollment?.completionId) apiRequest(`/v2/completions/${enrollment.completionId}`).then(setCompletion).catch(e => setError(e.message)) }, [enrollment?.completionId])
  const progression = enrollment?.progression ?? []
  const task = enrollment?.tasks.find(task => task.id === taskId), locked = submission?.status === 'FINAL'
  const key = owner && taskId && draftKey(owner, 'task', `${enrollmentId}_${taskId}`)
  function update(id, value) { const next = { ...values, [id]: { definitionId: id, ...value } }; setValues(next); writeDraft(key, { ...readDraft(key), values: next }); setNotice('Local draft. Save to keep it on the server.') }
  async function enroll() { setBusy(true); setError(''); try { const me = await apiRequest('/v2/me'); setOwner(me.id); const e = await command(`/v2/scenario-versions/${catalog.scenarioVersionId}/enrollments`, {}); setEnrollment(e); setTaskId(e.tasks[0]?.id); setSearch({ enrollment: e.id }) } catch (e) { setError(e.message) } finally { setBusy(false) } }
  async function upload(id, file) { if (!file) return; setBusy(true); setError(''); try { const body = new FormData(); body.set('file', file); const asset = await apiRequest('/v2/files', { method: 'POST', body }); update(id, { fileAssetId: asset.id }) } catch (e) { setError(e.message) } finally { setBusy(false) } }
  async function save(finalizing) {
    setBusy(true); setError('')
    try {
      const items = Object.values(values).map(({ jsonText, ...item }) => jsonText === undefined ? item : { definitionId: item.definitionId, contentJson: JSON.parse(jsonText) })
      const base = `/v2/enrollments/${enrollmentId}/tasks/${taskId}/submission`
      let body = { items }
      if (finalizing) { const requestId = readDraft(key)?.requestId ?? crypto.randomUUID(); writeDraft(key, { values, requestId }); body = { items, requestId } }
      const s = await command(finalizing ? `${base}/finalize` : base, body, submission.revision, finalizing ? 'POST' : 'PUT')
      setSubmission(s); setNotice('Saved on the server.'); setEnrollment(await apiRequest(`/v2/enrollments/${enrollmentId}`))
    } catch (e) { setError(e.message); setNotice('Your draft is preserved. Reload the attempt before retrying a conflict.') } finally { setBusy(false) }
  }
  const metadata = enrollment?.metadata ?? catalog?.metadata
  return <section className="feature-page section-shell core-flow"><Link to="/simulations">{t('Back to catalog')}</Link><h1>{metadata ? localized(metadata, locale, 'title') : t('Loading simulation...')}</h1>
    {error && <p role="alert">{t(error)} <Link to="/account">{t('Sign in')}</Link></p>}{notice && <p role="status">{t(notice)}</p>}
    {metadata && <p>{localized(metadata, locale, 'introduction')}</p>}
    {!enrollment && catalog && <><p>{localized(metadata, locale, 'summary')}</p><ol>{catalog.taskOutline?.map(tk => <li key={tk.id}>{tk.position}. {tk.title}</li>)}</ol><button className="button" disabled={busy} onClick={enroll}>{t('Enroll in this version')}</button></>}
    {enrollment && <><p>{t('Version')} {enrollment.scenarioVersionId} · {enrollment.finalRequiredTasks}/{enrollment.requiredTasks} {t('required tasks submitted')}</p>
      <label>{t('Task')}<select value={taskId ?? ''} disabled={busy} onChange={e => { setTaskId(e.target.value); setSubmission(null); setReleased(null); setValues({}); setError(''); setNotice('') }}>{enrollment.tasks.map(tk => <option key={tk.id} value={tk.id} disabled={progression.find(p => p.taskId === tk.id)?.accessible === false}>{localized(tk, locale, 'title')}{tk.required ? ' *' : ''}</option>)}</select></label>
      <button type="button" disabled={busy} onClick={() => window.location.reload()}>{t('Reload saved work')}</button>
      {task && <><h2>{localized(task, locale, 'title')}</h2><p>{localized(task, locale, 'workplaceContext')}</p><p className="core-prewrap">{localized(task, locale, 'instructions')}</p>
        <ul>{task.resources.filter(r => r.releasePolicy === 'TASK_ACCESS' || locked).map(r => <li key={r.id}><a href={resourceLink(r)} rel="noreferrer">{r.title}</a></li>)}</ul>
        {submission && <form onSubmit={e => { e.preventDefault(); if (!busy && !locked) setConfirmFinal(true) }}><fieldset disabled={busy || locked}><legend>{t(locked ? 'Final submission' : 'Deliverables')}</legend>
          {task.definitions.map(d => <div key={d.id} role="group" aria-label={d.name}>{d.name} {d.required ? '*' : t('(optional)')}
            {d.format === 'TEXT' && <textarea aria-label={`${d.name} ${d.required ? '*' : t('(optional)')}`} value={values[d.id]?.contentText ?? ''} maxLength={d.validationConfig.maxLength} onChange={e => update(d.id, { contentText: e.target.value })} />}
            {d.format === 'URL' && <input type="url" aria-label={`${d.name} ${d.required ? '*' : t('(optional)')}`} value={values[d.id]?.contentUrl ?? ''} onChange={e => update(d.id, { contentUrl: e.target.value })} />}
            {d.format === 'JSON' && choiceDefinition(task, d) && <fieldset><legend>{d.name}</legend>{Object.entries(d.validationConfig.jsonSchema.properties).map(([name, schema]) => <label key={name}>{name}<select required={d.validationConfig.jsonSchema.required?.includes(name)} value={values[d.id]?.contentJson?.[name] ?? ''} onChange={e => update(d.id, { contentJson: { ...values[d.id]?.contentJson, [name]: e.target.value } })}><option value="">{t('Select an answer')}</option>{schema.enum?.map(option => <option key={option}>{option}</option>)}</select></label>)}</fieldset>}
            {d.format === 'JSON' && !choiceDefinition(task, d) && <><textarea aria-label={`${d.name} JSON`} value={values[d.id]?.jsonText ?? ''} onChange={e => update(d.id, { jsonText: e.target.value })} /><small>{JSON.stringify(d.validationConfig.jsonSchema)}</small></>}
            {d.format === 'FILE' && <><input type="file" aria-label={`${d.name} ${d.required ? '*' : t('(optional)')}`} accept={d.validationConfig.allowedMimeTypes.join(',')} onChange={e => upload(d.id, e.target.files[0])} /><span>{values[d.id]?.fileAssetId ? t('File uploaded') : t('No file selected')}</span></>}
            {!d.required && values[d.id] && <button type="button" onClick={() => { const next = { ...values }; delete next[d.id]; setValues(next); writeDraft(key, { ...readDraft(key), values: next }) }}>{t('Remove optional item')}</button>}
          </div>)}
          <button type="button" onClick={() => save(false)}>{t('Save draft')}</button> <button className="button" type="submit">{t('Submit final task')}</button>
        </fieldset></form>}
        {submission?.quizResult && <p>{t('Quiz score')}: {submission.quizResult.correct}/{submission.quizResult.total} · {t('Completion does not depend on this score.')}</p>}
        {locked && enrollment.progressionMode === 'ORDERED_SELF_REVIEW' && <button disabled={busy || Boolean(submission.reviewedAt)} onClick={async () => { setBusy(true); try { setSubmission(await command(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/self-review`, {})); setEnrollment(await apiRequest(`/v2/enrollments/${enrollmentId}`)) } catch (e) { setError(e.message) } finally { setBusy(false) } }}>{t(submission.reviewedAt ? 'Self-review recorded' : 'I have compared my work and completed self-review')}</button>}
        {locked && <button disabled={busy} onClick={async () => { setBusy(true); try { setReleased(await apiRequest(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/model-answer`)) } catch (e) { setError(e.message) } finally { setBusy(false) } }}>{t('View model answer')}</button>}
        {released && <article><h3>{t('Model answer')}</h3><p className="core-prewrap">{released.answer ? localized(released.answer, locale, 'content') : t('No model answer was provided.')}</p><ul>{released.resources.map(r => <li key={r.id}><a href={resourceLink(r)}>{r.title}</a></li>)}</ul></article>}
      </>}
    </>}
    {completion && <article><h2>{t('Simulation completed')}</h2><p>{t('This records simulation participation, not professional competence.')}</p><label>{t('Fixed resume snippet')}<textarea readOnly value={completion.resumeSnippetText} /></label><a href={`${API_BASE_URL}/v2/completions/${completion.id}/certificate`} target="_blank" rel="noreferrer">{t('View certificate')}</a></article>}
    <SubmissionConfirmationDialog open={confirmFinal} title="Submit final task" message="Final submission cannot be changed. Submit this task?" onCancel={() => setConfirmFinal(false)} onConfirm={() => { setConfirmFinal(false); save(true) }} />
  </section>
}
