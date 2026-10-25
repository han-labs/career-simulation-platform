import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiRequest, API_BASE_URL } from '../../../shared/api/httpClient.js'
import { command, draftKey, localized, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SubmissionConfirmationDialog from '../components/SubmissionConfirmationDialog.jsx'

function choiceDefinition(task, d) { const properties = d.validationConfig?.jsonSchema?.properties; return task.taskType === 'QUIZ' && properties && Object.keys(properties).length > 0 && Object.values(properties).every(p => p.type === 'string' && Array.isArray(p.enum)) }
function resourceLink(resource) { return resource.fileAssetId ? `${API_BASE_URL}/v2/files/${resource.fileAssetId}/download?useType=TASK_RESOURCE&useId=${resource.id}` : resource.externalUrl }
function readableObjectives(value) {
  if (typeof value !== 'string' || !value.trim()) return { items: [], text: '' }
  const document = new DOMParser().parseFromString(value, 'text/html')
  const items = [...document.body.querySelectorAll('li')].map(item => {
    const copy = item.cloneNode(true)
    copy.querySelectorAll('ul, ol').forEach(list => list.remove())
    return copy.textContent.replace(/\s+/g, ' ').trim()
  }).filter(Boolean)
  return items.length ? { items, text: '' } : { items: [], text: document.body.textContent.trim() }
}
function wistiaEmbed(value) {
  return typeof value === 'string' && /^https:\/\/fast\.wistia\.(net|com)\/embed\/iframe\/[A-Za-z0-9]+$/.test(value) ? value : null
}
function readablePublicText(value) {
  if (typeof value !== 'string' || !value.trim()) return ''
  return new DOMParser().parseFromString(value, 'text/html').body.textContent.replace(/\s+/g, ' ').trim()
}
export default function CoreSimulationPage() {
  const { t, locale } = useI18n(), { slug } = useParams(), [search, setSearch] = useSearchParams()
  const navigate = useNavigate()
  const enrollmentId = search.get('enrollment')
  const [catalog, setCatalog] = useState(null), [enrollment, setEnrollment] = useState(null), [owner, setOwner] = useState(null)
  const [taskId, setTaskId] = useState(null), [submission, setSubmission] = useState(null), [values, setValues] = useState({})
  const [released, setReleased] = useState(null), [completion, setCompletion] = useState(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [confirmFinal, setConfirmFinal] = useState(false), [detailRetry, setDetailRetry] = useState(0)
  const [selectedTaskId, setSelectedTaskId] = useState(null)
  useEffect(() => { let active = true
    const request = enrollmentId ? Promise.all([apiRequest('/v2/me'), apiRequest(`/v2/enrollments/${enrollmentId}`)]) : apiRequest(`/v2/scenarios/${encodeURIComponent(slug)}`).then(c => [null, c])
    request.then(([me, data]) => { if (!active) return; if (me) { setOwner(me.id); setEnrollment(data); setTaskId(data.progression?.find(p => p.accessible && !p.reviewed)?.taskId ?? data.tasks[0]?.id) } else { setCatalog(data); setSelectedTaskId(data.taskOutline?.[0]?.id ?? null) } }).catch(e => { if (active) setError(enrollmentId ? e.message : [403, 404].includes(e.status) ? 'This simulation is unavailable.' : 'Simulation details could not be loaded. Please try again.') })
    return () => { active = false }
  }, [slug, enrollmentId, detailRetry])
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
  async function enroll() { setBusy(true); setError(''); try { const me = await apiRequest('/v2/me'); setOwner(me.id); const e = await command(`/v2/scenario-versions/${catalog.scenarioVersionId}/enrollments`, {}); setEnrollment(e); setTaskId(e.tasks[0]?.id); setSearch({ enrollment: e.id }) } catch (e) { if (e.status === 401) { try { sessionStorage.setItem('careersim_pending_simulation_path', `/simulations/${encodeURIComponent(slug)}`) } catch { /* Login remains available when tab storage is blocked. */ } navigate('/account') } else setError(e.message) } finally { setBusy(false) } }
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
  const learningObjectives = readableObjectives(metadata ? localized(metadata, locale, 'learningObjectives') : '')
  const overviewVideo = wistiaEmbed(catalog?.media?.overviewVideoUrl)
  return <main className={`feature-page section-shell core-flow${!enrollment ? ' core-simulation-public' : ''}`}><Link className="back-link" to="/simulations">{t('Back to catalog')}</Link>
    {error && <p role="alert">{t(error)} {enrollmentId && <Link to="/account">{t('Sign in')}</Link>}</p>}
    {!enrollmentId && error === 'Simulation details could not be loaded. Please try again.' && <button className="button button--secondary" type="button" onClick={() => { setError(''); setCatalog(null); setDetailRetry(value => value + 1) }}>{t('Try again')}</button>}
    {notice && <p role="status">{t(notice)}</p>}
    {!metadata && !error && <p>{t('Loading simulation...')}</p>}
    {!enrollment && catalog && <>
      <header className="core-simulation-public__hero">
        <div>
          <p className="core-simulation-public__company">
            {catalog.media?.companyLogoUrl && <img src={catalog.media.companyLogoUrl} alt="" />}
            <span>{catalog.enterpriseName || t('CareerSim partner')}</span>
          </p>
          <p className="eyebrow">{t(catalog.industryCode || 'IT')} · {localized(metadata, locale, 'careerTrack')}</p>
          <h1>{localized(metadata, locale, 'title')}</h1>
          <p className="core-simulation-public__summary">{localized(metadata, locale, 'summary')}</p>
        </div>
        <div className="core-simulation-public__side">
          {(catalog.media?.heroImageUrl || catalog.media?.cardImageUrl) && <img className="core-simulation-public__hero-image" src={catalog.media.heroImageUrl || catalog.media.cardImageUrl} alt="" />}
          <div className="core-simulation-public__action">
            <button className="button" disabled={busy} onClick={enroll}>{t(busy ? 'Starting…' : 'Start free simulation')}</button>
            <small>{t('Sign in is required to save your progress.')}</small>
          </div>
        </div>
      </header>
      <dl className="core-simulation-public__facts">
        <div><dt>{t('Role')}</dt><dd>{localized(metadata, locale, 'targetRole')}</dd></div>
        <div><dt>{t('Duration')}</dt><dd>{t(`${metadata.estimatedMinutes} minutes`)}</dd></div>
        <div><dt>{t('Difficulty')}</dt><dd>{t(metadata.difficulty)}</dd></div>
      </dl>
      <nav className="core-simulation-public__tabs" aria-label={t('Simulation information')}>
        <a href="#simulation-overview">{t('Overview')}</a>
        <a href="#simulation-video">{t('Video')}</a>
        <a href="#simulation-tasks">{t('Tasks')}</a>
      </nav>
      <section id="simulation-overview" className="core-simulation-public__section core-simulation-public__overview">
        <h2>{t('Why complete this Job Simulation')}</h2>
        <p>{localized(metadata, locale, 'introduction') || localized(metadata, locale, 'summary')}</p>
        {(learningObjectives.items.length > 0 || learningObjectives.text) && <div className="core-simulation-public__objectives"><h3>{t('Learning objectives')}</h3>{learningObjectives.items.length > 0 ? <ul>{learningObjectives.items.map((objective, index) => <li key={`${index}_${objective}`}>{objective}</li>)}</ul> : <p className="core-prewrap">{learningObjectives.text}</p>}</div>}
        <div className="core-simulation-public__supporting">
          <article>
            <h3>{t('Available Achievements')}</h3>
            <p>{t('Complete the simulation to record your participation and the skills you practiced. This does not certify professional competence.')}</p>
          </article>
          <article>
            <h3>{t('How it works')}</h3>
            <p>{t('Work through self-paced tasks in order and save your submissions. Example answers unlock after a valid final submission; self-review advances the ordered progression.')}</p>
          </article>
        </div>
        <div className="core-simulation-public__skills">
          <h3>{t('Skills you will learn and practice')}</h3>
          {catalog.skills?.length ? <ul>{catalog.skills.map(skill => <li key={skill}>{skill}</li>)}</ul> : <p>{t('Skill information is not available for this simulation.')}</p>}
        </div>
      </section>
      <section id="simulation-video" className="core-simulation-public__section">
        <h2>{t('Video')}</h2>
        {overviewVideo ? <div className="core-simulation-public__video"><iframe src={overviewVideo} title={t('Simulation overview video')} allow="autoplay; fullscreen" allowFullScreen /></div> : <p>{t('No overview video is available for this simulation.')}</p>}
      </section>
      <section id="simulation-tasks" className="core-simulation-public__section">
        <h2>{t('Simulation tasks')}</h2>
        <ol className="core-simulation-public__tasks">{catalog.taskOutline?.map(taskOutline => <li key={taskOutline.id}>
          <button type="button" aria-expanded={selectedTaskId === taskOutline.id} aria-controls={selectedTaskId === taskOutline.id && readablePublicText(taskOutline.description) ? `public-task-${taskOutline.id}` : undefined} onClick={() => setSelectedTaskId(current => current === taskOutline.id ? null : taskOutline.id)}><span>{taskOutline.position}</span><strong>{taskOutline.title}</strong></button>
          {selectedTaskId === taskOutline.id && readablePublicText(taskOutline.description) && <p id={`public-task-${taskOutline.id}`}>{readablePublicText(taskOutline.description)}</p>}
        </li>)}</ol>
      </section>
    </>}
    {enrollment && metadata && <><h1>{localized(metadata, locale, 'title')}</h1><p>{localized(metadata, locale, 'introduction')}</p></>}
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
  </main>
}
