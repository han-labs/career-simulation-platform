import { useEffect, useState } from 'react'
import { AlertCircle, Check, CheckCircle2, Circle, Clock3, ExternalLink, FileText, Info, LockKeyhole, Upload } from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiRequest, API_BASE_URL } from '../../../shared/api/httpClient.js'
import { command, draftKey, localized, readDraft, writeDraft } from '../../../shared/api/coreApi.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SubmissionConfirmationDialog from '../components/SubmissionConfirmationDialog.jsx'
import TaskRichText from '../components/TaskRichText.jsx'
import { taskReferenceMaterials } from '../data/referenceMaterials.js'

function choiceDefinition(task, d) { const properties = d.validationConfig?.jsonSchema?.properties; return task.taskType === 'QUIZ' && properties && Object.keys(properties).length > 0 && Object.values(properties).every(p => p.type === 'string' && Array.isArray(p.enum)) }
function resourceLink(resource) { return resource.fileAssetId ? `${API_BASE_URL}/v2/files/${resource.fileAssetId}/download?useType=TASK_RESOURCE&useId=${resource.id}` : resource.externalUrl }
function requiredFormat(definition, t) {
  const formats = { TEXT: t('Text response'), URL: t('Web link'), JSON: t('JSON document'), FILE: t('File upload') }
  return `${formats[definition.format] || definition.format} · ${t(definition.required ? 'Required' : 'Optional')}`
}
function allowedFileFormats(mimeTypes) {
  const labels = {
    'text/plain': 'TXT/PY',
    'application/pdf': 'PDF',
    'image/png': 'PNG',
    'image/jpeg': 'JPEG',
    'application/zip': 'ZIP',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'XLSX',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'PPTX',
  }
  return mimeTypes.map(mime => labels[mime] ?? mime).join(', ')
}
function definitionHasValue(definition, value) {
  if (!definition.required) return true
  if (!value) return false
  if (definition.format === 'TEXT') return Boolean(value.contentText?.trim()) && (!definition.validationConfig?.maxLength || value.contentText.length <= definition.validationConfig.maxLength)
  if (definition.format === 'URL') {
    try { return ['http:', 'https:'].includes(new URL(value.contentUrl).protocol) } catch { return false }
  }
  if (definition.format === 'FILE') return Boolean(value.fileAssetId)
  if (definition.format === 'JSON') {
    if (value.contentJson) {
      const required = definition.validationConfig?.jsonSchema?.required ?? []
      return required.every(name => value.contentJson[name] !== undefined && value.contentJson[name] !== '')
    }
    try { return Boolean(value.jsonText?.trim()) && JSON.parse(value.jsonText) !== null } catch { return false }
  }
  return false
}
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
  const [catalog, setCatalog] = useState(null), [loadedEnrollment, setEnrollment] = useState(null), [owner, setOwner] = useState(null)
  const enrollment = loadedEnrollment?.id === enrollmentId ? loadedEnrollment : null
  const [taskId, setTaskId] = useState(null), [submission, setSubmission] = useState(null), [values, setValues] = useState({})
  const [released, setReleased] = useState(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [confirmFinal, setConfirmFinal] = useState(false), [detailRetry, setDetailRetry] = useState(0)
  const [selectedTaskId, setSelectedTaskId] = useState(null)
  const [loadedResults, setResults] = useState({ status: 'idle', data: null })
  const results = loadedResults.enrollmentId === enrollment?.id ? loadedResults : { status: 'idle', data: null }
  const completion = results.status === 'ready' ? results.data?.completion : null
  const [resultsRetry, setResultsRetry] = useState(0)
  const [accessDenied, setAccessDenied] = useState(false)
  const [requiresSignIn, setRequiresSignIn] = useState(false)
  useEffect(() => { let active = true
    const request = enrollmentId ? Promise.all([apiRequest('/v2/me'), apiRequest(`/v2/enrollments/${enrollmentId}`)]) : apiRequest(`/v2/scenarios/${encodeURIComponent(slug)}`).then(c => [null, c])
    request.then(([me, data]) => { if (!active) return; setRequiresSignIn(false); if (me) { setAccessDenied(false); setOwner(me.id); setEnrollment(data); setTaskId(data.progression?.find(p => p.accessible && !p.reviewed)?.taskId ?? data.tasks[0]?.id) } else { setCatalog(data); setSelectedTaskId(data.taskOutline?.[0]?.id ?? null) } }).catch(e => { if (active) { setRequiresSignIn(e.status === 401); setAccessDenied(Boolean(enrollmentId && [403, 404].includes(e.status))); setError(enrollmentId ? [403, 404].includes(e.status) ? 'You do not have permission to view these simulation results.' : e.message : [403, 404].includes(e.status) ? 'This simulation is unavailable.' : 'Simulation details could not be loaded. Please try again.') } })
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
  useEffect(() => {
    if (!enrollment || !owner) return
    let active = true
    apiRequest(`/v2/enrollments/${enrollment.id}/results`).then(data => {
      if (active) setResults({ enrollmentId: enrollment.id, status: 'ready', data })
    }).catch(e => { if (active) setResults({ enrollmentId: enrollment.id, status: [403, 404].includes(e.status) ? 'denied' : 'error', data: null }) })
    return () => { active = false }
  }, [enrollment, owner, resultsRetry])
  const progression = enrollment?.progression ?? []
  const orderedTasks = [...(enrollment?.tasks ?? [])].sort((a, b) => a.position - b.position)
  const task = orderedTasks.find(task => task.id === taskId), locked = submission?.status === 'FINAL'
  const key = owner && taskId && draftKey(owner, 'task', `${enrollmentId}_${taskId}`)
  const canFinalize = Boolean(submission && !locked && task?.definitions.every(definition => definitionHasValue(definition, values[definition.id])))
  function selectTask(id) {
    if (busy || id === taskId) return
    if (progression.find(p => p.taskId === id)?.accessible === false) { setNotice(''); setError('Please confirm your self-review before continuing.'); return }
    setTaskId(id); setSubmission(null); setReleased(null); setValues({}); setError(''); setNotice('')
  }
  function taskStatus(item) {
    const progress = progression.find(p => p.taskId === item.id)
    const saved = results.data?.tasks?.find(p => p.taskId === item.id)
    return progress?.accessible === false ? 'Locked' : progress?.reviewed || saved?.reviewedAt ? 'Self-review recorded' : progress?.finalized || saved?.status === 'FINAL' ? 'Final submission' : saved?.status === 'DRAFT' ? 'Draft' : 'Not started'
  }
  function statusIcon(status) {
    if (status === 'Locked') return <LockKeyhole aria-hidden="true" />
    if (status === 'Self-review recorded' || status === 'Final submission') return <Check aria-hidden="true" />
    if (status === 'Draft') return <Clock3 aria-hidden="true" />
    return <Circle aria-hidden="true" />
  }
  function update(id, value) { const next = { ...values, [id]: { definitionId: id, ...value } }; setValues(next); writeDraft(key, { ...readDraft(key), values: next }); setNotice('Local draft. Save to keep it on the server.') }
  async function enroll() { setBusy(true); setError(''); try { const me = await apiRequest('/v2/me'); setOwner(me.id); const e = await command(`/v2/scenario-versions/${catalog.scenarioVersionId}/enrollments`, {}); setEnrollment(e); setTaskId(e.tasks[0]?.id); setSearch({ enrollment: e.id }) } catch (e) { if (e.status === 401) { try { sessionStorage.setItem('careersim_pending_simulation_path', `/simulations/${encodeURIComponent(slug)}`) } catch { /* Login remains available when tab storage is blocked. */ } navigate('/account') } else setError(e.message) } finally { setBusy(false) } }
  async function upload(id, file, maxBytes) {
    if (!file) return
    if (file.size > maxBytes) { setError('The selected file exceeds the 10 MiB limit.'); return }
    setBusy(true); setError('')
    try { const body = new FormData(); body.set('file', file); const asset = await apiRequest('/v2/files', { method: 'POST', body }); update(id, { fileAssetId: asset.id }) }
    catch (e) { setError(e.status === 413 ? 'The selected file exceeds the 10 MiB limit.' : e.message) }
    finally { setBusy(false) }
  }
  async function save(finalizing) {
    setBusy(true); setError(''); setRequiresSignIn(false)
    try {
      const items = Object.values(values).map(({ jsonText, ...item }) => jsonText === undefined ? item : { definitionId: item.definitionId, contentJson: JSON.parse(jsonText) })
      const base = `/v2/enrollments/${enrollmentId}/tasks/${taskId}/submission`
      let body = { items }
      if (finalizing) { const requestId = readDraft(key)?.requestId ?? crypto.randomUUID(); writeDraft(key, { values, requestId }); body = { items, requestId } }
      const s = await command(finalizing ? `${base}/finalize` : base, body, submission.revision, finalizing ? 'POST' : 'PUT')
      setSubmission(s); setNotice('Saved on the server.'); setEnrollment(await apiRequest(`/v2/enrollments/${enrollmentId}`)); setResultsRetry(value => value + 1)
    } catch (e) {
      const revisionConflict = e.status === 409 && e.code === 'CONFLICT'
      setRequiresSignIn(e.status === 401)
      setError(e.status === 422 || e instanceof SyntaxError ? 'Please complete the required task response before submitting.' : revisionConflict ? 'The server has a newer version of this task. Your local work is preserved.' : e.code === 'FINAL_SUBMISSION_LOCKED' ? e.message : finalizing ? 'Your submission could not be confirmed. Please try again.' : e.message)
      if (revisionConflict) {
        try {
          const latest = await apiRequest(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/submission`)
          setSubmission(latest)
          setNotice('The latest server revision was loaded. Review your preserved work and try again.')
        } catch { setNotice('Your draft is preserved. Reload the attempt before retrying a conflict.') }
      } else setNotice('Your work is preserved locally. Review the error and try again.')
    } finally { setBusy(false) }
  }
  const metadata = enrollment?.metadata ?? catalog?.metadata
  const resources = taskReferenceMaterials(metadata, task, locked)
  const learningObjectives = readableObjectives(metadata ? localized(metadata, locale, 'learningObjectives') : '')
  const overviewVideo = wistiaEmbed(catalog?.media?.overviewVideoUrl)
  return <main className={`feature-page core-flow${enrollment ? ' core-simulation-page' : ' section-shell core-simulation-public'}`}>
    <div className={enrollment ? 'core-simulation-page__topbar' : ''}><Link className="back-link" to="/simulations" aria-label={t('Back to catalog')}><span aria-hidden="true">← </span>{t('Back to catalog')}</Link>{enrollment && <span>{enrollment.finalRequiredTasks}/{enrollment.requiredTasks} {t('required tasks submitted')}</span>}</div>
    {error && <div className="core-feedback core-feedback--error" role="alert"><AlertCircle aria-hidden="true" /><div><strong>{t('Something needs your attention')}</strong><p>{t(error)} {requiresSignIn && <Link to="/account">{t('Sign in')}</Link>}</p></div></div>}
    {accessDenied && <Link to="/dashboard">{t('Return to my simulation history')}</Link>}
    {!enrollmentId && error === 'Simulation details could not be loaded. Please try again.' && <button className="button button--secondary" type="button" onClick={() => { setError(''); setCatalog(null); setDetailRetry(value => value + 1) }}>{t('Try again')}</button>}
    {notice && <div className="core-feedback core-feedback--status" role="status"><Info aria-hidden="true" /><div><strong>{t('Progress update')}</strong><p>{t(notice)}</p></div></div>}
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
          <dl className="core-simulation-public__facts">
            <div><dt>{t('Role')}</dt><dd>{localized(metadata, locale, 'targetRole')}</dd></div>
            <div><dt>{t('Duration')}</dt><dd>{t(`${metadata.estimatedMinutes} minutes`)}</dd></div>
            <div><dt>{t('Difficulty')}</dt><dd>{t(metadata.difficulty)}</dd></div>
          </dl>
        </div>
        <div className="core-simulation-public__side">
          {(catalog.media?.heroImageUrl || catalog.media?.cardImageUrl) && <img className="core-simulation-public__hero-image" src={catalog.media.heroImageUrl || catalog.media.cardImageUrl} alt="" />}
          <div className="core-simulation-public__action">
            <button className="button" disabled={busy} onClick={enroll}>{t(busy ? 'Starting…' : 'Start free simulation')}</button>
            <small>{t('Sign in is required to save your progress.')}</small>
          </div>
        </div>
      </header>
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
          {catalog.skills?.length ? <ul>{catalog.skills.map(skill => <li key={skill}>{t(skill)}</li>)}</ul> : <p>{t('Skill information is not available for this simulation.')}</p>}
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
    {enrollment && <div className="core-simulation-layout">
      <aside className="core-task-sidebar" aria-label={t('Simulation tasks')}>
        <div className="core-task-sidebar__brand"><span aria-hidden="true">CS</span><div><strong>{localized(metadata, locale, 'title')}</strong><small>{localized(metadata, locale, 'targetRole') || t('Career simulation')}</small></div></div>
        <nav aria-label={t('Ordered task list')}>{orderedTasks.map((item, index) => { const status = taskStatus(item); return <button key={item.id} type="button" className={item.id === taskId ? 'is-active' : ''} disabled={busy || status === 'Locked'} aria-label={`${index + 1}. ${localized(item, locale, 'title')} — ${t(status)}`} aria-current={item.id === taskId ? 'step' : undefined} onClick={() => selectTask(item.id)}><span className={`core-task-status core-task-status--${status.toLowerCase().replaceAll(' ', '-')}`}>{statusIcon(status)}</span><span><small>{t('Task')} {index + 1}</small><strong>{localized(item, locale, 'title')}</strong><em>{t(status)}</em></span></button>})}</nav>
      </aside>
      <section className="core-task-main" aria-label={t('Task workspace')}>
      {task && <>
        <header className="core-task-header"><div><p>{t('Task')} {orderedTasks.findIndex(item => item.id === task.id) + 1} / {orderedTasks.length}</p><h1>{localized(task, locale, 'title')}</h1></div></header>
        <section className="core-task-section" aria-labelledby="task-background-heading"><p className="core-eyebrow">{t('Task overview')}</p><h2 id="task-background-heading">{t('Here is the background information on your task')}</h2><div className="core-background-grid"><article><span className="core-section-icon" aria-hidden="true"><FileText /></span><div><h3>{t("What you'll learn")}</h3>{learningObjectives.items.length ? <ul className="core-compact-list">{learningObjectives.items.map((objective, index) => <li key={`${index}_${objective}`}>{objective}</li>)}</ul> : <p>{learningObjectives.text}</p>}</div></article><article><span className="core-section-icon" aria-hidden="true"><CheckCircle2 /></span><div><h3>{t("What you'll do")}</h3><TaskRichText value={localized(task, locale, 'workplaceContext')} /></div></article></div></section>
        <section className="core-task-section" aria-labelledby="task-instructions-heading"><p className="core-eyebrow">{t('Instructions')}</p><h2 id="task-instructions-heading">{t('Here is your task')}</h2><TaskRichText className="core-task-instructions" value={localized(task, locale, 'instructions')} /></section>
        <section className="core-task-section" aria-labelledby="task-resources-heading"><p className="core-eyebrow">{t('Reference material')}</p><h2 id="task-resources-heading">{t('Resources')}</h2><div className="core-resource-list">{resources.map(r => <a key={r.id} href={resourceLink(r)} target="_blank" rel="noreferrer"><FileText aria-hidden="true" /><span><strong>{t(r.title)}</strong><small>{t('Opens in a new tab')}</small></span><ExternalLink aria-hidden="true" /></a>)}</div>{!resources.length && <p>{t('No resources are available for this task.')}</p>}</section>
        {submission && <section className="core-task-section core-submission" aria-labelledby="task-submission-heading"><p className="core-eyebrow">{t('Required work')}</p><h2 id="task-submission-heading">{t('Your submission')}</h2><p>{t('Save draft keeps your work editable. Final submission locks this task.')}</p><form onSubmit={e => { e.preventDefault(); if (!busy && !locked) setConfirmFinal(true) }}><fieldset disabled={busy || locked}>
          {task.definitions.map(d => { const allowedMimeTypes = Array.isArray(d.validationConfig?.allowedMimeTypes) && d.validationConfig.allowedMimeTypes.length ? d.validationConfig.allowedMimeTypes : null; return <div className="core-deliverable" key={d.id} role="group" aria-label={t(d.name)}><span><strong>{t(d.name)}</strong><small>{requiredFormat(d, t)}</small></span>
            {d.format === 'TEXT' && <textarea aria-label={`${t(d.name)} ${d.required ? '*' : t('(optional)')}`} value={values[d.id]?.contentText ?? ''} maxLength={d.validationConfig.maxLength} onChange={e => update(d.id, { contentText: e.target.value })} />}
            {d.format === 'URL' && <input type="url" aria-label={`${t(d.name)} ${d.required ? '*' : t('(optional)')}`} value={values[d.id]?.contentUrl ?? ''} onChange={e => update(d.id, { contentUrl: e.target.value })} />}
            {d.format === 'JSON' && choiceDefinition(task, d) && <fieldset><legend>{t(d.name)}</legend>{Object.entries(d.validationConfig.jsonSchema.properties).map(([name, schema]) => <label key={name}>{t(name)}<select required={d.validationConfig.jsonSchema.required?.includes(name)} value={values[d.id]?.contentJson?.[name] ?? ''} onChange={e => update(d.id, { contentJson: { ...values[d.id]?.contentJson, [name]: e.target.value } })}><option value="">{t('Select an answer')}</option>{schema.enum?.map(option => <option key={option} value={option}>{t(option)}</option>)}</select></label>)}</fieldset>}
            {d.format === 'JSON' && !choiceDefinition(task, d) && <><textarea aria-label={`${t(d.name)} JSON`} value={values[d.id]?.jsonText ?? ''} onChange={e => update(d.id, { jsonText: e.target.value })} /><small>{JSON.stringify(d.validationConfig.jsonSchema)}</small></>}
            {d.format === 'FILE' && (allowedMimeTypes ? <><small>{t('Allowed formats')}: {allowedFileFormats(allowedMimeTypes)} · {t('Maximum file size')}: 10 MiB</small><span className="core-file-control"><Upload aria-hidden="true" /><input type="file" aria-label={`${t(d.name)} ${d.required ? '*' : t('(optional)')}`} accept={allowedMimeTypes.join(',')} onChange={e => upload(d.id, e.target.files[0], d.validationConfig.maxBytes)} /></span><span>{values[d.id]?.fileAssetId ? t('File uploaded') : t('No file selected')}</span></> : <p role="alert">{t('This task has an invalid file-upload configuration. Please contact the administrator.')}</p>)}
            {!d.required && values[d.id] && <button type="button" onClick={() => { const next = { ...values }; delete next[d.id]; setValues(next); writeDraft(key, { ...readDraft(key), values: next }) }}>{t('Remove optional item')}</button>}
          </div> })}
          <div className="core-submission__actions"><button className="button button--secondary" type="button" onClick={() => save(false)}>{t('Save draft')}</button>{canFinalize && <button className="button" type="submit">{t('Submit task')}</button>}</div>
          {!locked && !canFinalize && <p className="core-submission__hint">{t('Complete all required deliverables to submit this task as final.')}</p>}
        </fieldset></form>{locked && <div className="core-submission-complete"><CheckCircle2 aria-hidden="true" /><span><strong>{t('Submission complete')}</strong><small>{t('Final work is read only.')}</small></span></div>}</section>}
        {submission?.quizResult && <p>{t('Quiz score')}: {submission.quizResult.correct}/{submission.quizResult.total} · {t('Completion does not depend on this score.')}</p>}
        <section className="core-task-section core-example-answer" aria-labelledby="model-answer-heading"><p className="core-eyebrow">{t('Self-review')}</p><h2 id="model-answer-heading">{t('Example answer')}</h2>{!locked && <p><LockKeyhole aria-hidden="true" /> {t('Finalize this task before viewing its model answer.')}</p>}{locked && !released && <><p>{t('Your final work is locked. Open the reviewed answer when you are ready to compare approaches.')}</p><button className="button button--secondary" disabled={busy} onClick={async () => { setBusy(true); setError(''); try { setReleased(await apiRequest(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/model-answer`)) } catch (e) { setError(e.message) } finally { setBusy(false) } }}>{t('View model answer')}</button></>}{released && <article><TaskRichText value={released.answer ? localized(released.answer, locale, 'content') : t('No model answer was provided.')} /><div className="core-resource-list">{released.resources.map(r => <a key={r.id} href={resourceLink(r)} target="_blank" rel="noreferrer"><FileText aria-hidden="true" /><span><strong>{t(r.title)}</strong><small>{t('Opens in a new tab')}</small></span><ExternalLink aria-hidden="true" /></a>)}</div></article>}{locked && enrollment.progressionMode === 'ORDERED_SELF_REVIEW' && <div className="core-self-review-actions"><p>{t(submission.reviewedAt ? 'Self-review recorded. The next task is available.' : 'Compare your work with the example, then confirm self-review to unlock the next task.')}</p><button className="button" disabled={busy || Boolean(submission.reviewedAt)} onClick={async () => { setBusy(true); setError(''); setNotice(''); try { const reviewed = await command(`/v2/enrollments/${enrollmentId}/tasks/${taskId}/self-review`, {}); setSubmission(reviewed); setEnrollment(await apiRequest(`/v2/enrollments/${enrollmentId}`)); setResultsRetry(value => value + 1); setNotice('Self-review recorded. The next task is now available.') } catch (e) { setError(e.message) } finally { setBusy(false) } }}>{t(submission.reviewedAt ? 'Self-review recorded' : 'Confirm self-review')}</button></div>}</section>
        <nav className="core-task-footer" aria-label={t('Task navigation')}><button className="button button--secondary" type="button" disabled={busy || orderedTasks[0]?.id === taskId} onClick={() => selectTask(orderedTasks[orderedTasks.findIndex(item => item.id === taskId) - 1].id)}>{t('Previous')}</button><button className="button" type="button" disabled={busy || orderedTasks.at(-1)?.id === taskId} onClick={() => selectTask(orderedTasks[orderedTasks.findIndex(item => item.id === taskId) + 1].id)}>{t('Next task')}</button></nav>
      </>}
      {completion && <section className="core-completion-action" aria-labelledby="complete-simulation-heading"><CheckCircle2 aria-hidden="true" /><div><h2 id="complete-simulation-heading">{t('All required tasks are complete')}</h2><p>{t('Finish the simulation to view your completion record.')}</p></div><button className="button" type="button" onClick={() => navigate(`/simulations/${encodeURIComponent(slug)}/results?enrollment=${encodeURIComponent(enrollmentId)}`)}>{t('Complete simulation')}</button></section>}
      </section></div>}
    <SubmissionConfirmationDialog open={confirmFinal} title="Submit this task?" message="A final submission becomes read only and cannot be changed." onCancel={() => setConfirmFinal(false)} onConfirm={() => { setConfirmFinal(false); save(true) }} />
  </main>
}
