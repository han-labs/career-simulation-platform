import { useEffect, useState } from 'react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { command, draftKey, readDraft, writeDraft } from '../../shared/api/coreApi.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import JsonField from '../../shared/ui/JsonField.jsx'
import ScenarioTaskEditor from './ScenarioTaskEditor.jsx'

const blankMetadata = { title: '', targetRole: '', careerTrack: '', summary: '', introduction: '', learningObjectives: '', difficulty: 'INTRODUCTORY', estimatedMinutes: 30, translations: {}, provenance: {} }
const textDefinition = () => ({ id: null, position: 1, name: 'Response', format: 'TEXT', required: true, validationConfig: { schemaVersion: 1, minLength: 1, maxLength: 10000 } })
const blankTask = position => ({ id: null, position, title: '', workplaceContext: '', instructions: '', required: true, taskType: 'PRACTICAL', rubric: null, translations: {}, definitions: [textDefinition()], resources: [], skillIds: [], modelAnswer: null })

export default function EnterpriseWorkspace() {
  const { t } = useI18n()
  const [me, setMe] = useState(null), [membership, setMembership] = useState(null), [versions, setVersions] = useState([]), [stats, setStats] = useState([])
  const [skills, setSkills] = useState([])
  const [version, setVersion] = useState(null), [draft, setDraft] = useState({ metadata: blankMetadata, tasks: [] }), [slug, setSlug] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [preview, setPreview] = useState(false), [dirty, setDirty] = useState(false)
  useEffect(() => { let active = true; Promise.all([apiRequest('/v2/me'), apiRequest('/v2/enterprises/me'), apiRequest('/v2/enterprise/scenario-versions'), apiRequest('/v2/career-skills')]).then(async ([user, member, list, vocabulary]) => { const summary = await apiRequest(`/v2/enterprises/${member.enterpriseId}/participation-summary`); if (active) { setMe(user); setMembership(member); setVersions(list); setStats(summary); setSkills(vocabulary.filter(s => s.status === 'ACTIVE')); setDraft(readDraft(draftKey(user.id, 'enterprise-scenario', 'new')) || { metadata: blankMetadata, tasks: [] }); setSlug(readDraft(draftKey(user.id, 'enterprise-slug', 'new')) || '') } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  const key = me && draftKey(me.id, 'enterprise-scenario', version?.id ?? 'new')
  function edit(next) { setDraft(next); setDirty(true); setPreview(false); if (key) writeDraft(key, next) }
  function select(v) {
    const local = v.status === 'DRAFT' && readDraft(draftKey(me.id, 'enterprise-scenario', v.id))
    setVersion(v); setDraft(local || { metadata: v.metadata, tasks: v.tasks }); setDirty(Boolean(local)); setPreview(false); setError('')
  }
  async function action(kind) {
    setBusy(true); setError('')
    try {
      let v
      if (kind === 'clone') v = await command(`/v2/enterprise/scenarios/${version.scenarioId}/versions`, { sourceVersionId: version.id })
      else if (kind === 'publish') v = await command(`/v2/enterprise/scenario-versions/${version.id}/publish`, {}, version.revision)
      else if (!version) v = await command('/v2/enterprise/scenarios', { slug, draft })
      else v = await command(`/v2/enterprise/scenario-versions/${version.id}`, draft, version.revision, 'PUT')
      setVersion(v); setDraft({ metadata: v.metadata, tasks: v.tasks }); setDirty(false); setPreview(kind === 'publish'); writeDraft(draftKey(me.id, 'enterprise-scenario', v.id), null)
      setVersions(await apiRequest('/v2/enterprise/scenario-versions'))
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const field = (name, label, multiline = false) => <label key={name}>{t(label)}{multiline ? <textarea required value={draft.metadata[name]} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, [name]: e.target.value } })} /> : <input required value={draft.metadata[name]} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, [name]: e.target.value } })} />}</label>
  function taskField(index, values) { edit({ ...draft, tasks: draft.tasks.map((task, i) => i === index ? { ...task, ...values } : task) }) }
  return <section className="section-shell core-flow"><h1>{t('Enterprise workspace')}</h1>{error && <p role="alert">{t(error)}</p>}
    {membership && <><p>{membership.name} · {membership.verificationStatus} · {membership.publishingStatus}</p><p>{t('Publishing permission is separate from enterprise registration.')}</p>
      <label>{t('Scenario version')}<select value={version?.id ?? ''} onChange={e => { if (e.target.value) select(versions.find(v => v.id === e.target.value)); else { setVersion(null); setDraft(readDraft(draftKey(me.id, 'enterprise-scenario', 'new')) || { metadata: blankMetadata, tasks: [] }); setPreview(false); setDirty(true) } }}><option value="">{t('New scenario draft')}</option>{versions.map(v => <option key={v.id} value={v.id}>{v.metadata.title} · V{v.versionNumber} · {v.status}</option>)}</select></label>
      <button disabled={busy || !version} onClick={() => action('clone')}>{t('Clone as new draft')}</button>
      <form onSubmit={e => { e.preventDefault(); action('save') }}><fieldset disabled={busy || version?.status === 'PUBLISHED'}><legend>{t('Scenario draft')}</legend>
        {!version && <label>{t('Slug')}<input required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxLength={120} value={slug} onChange={e => { setSlug(e.target.value); writeDraft(draftKey(me.id, 'enterprise-slug', 'new'), e.target.value) }} /></label>}
        {field('title', 'Title')}{field('targetRole', 'Target role')}{field('careerTrack', 'Career track')}{field('summary', 'Summary', true)}{field('introduction', 'Introduction', true)}{field('learningObjectives', 'Learning objectives', true)}
        <label>{t('Difficulty')}<select value={draft.metadata.difficulty} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, difficulty: e.target.value } })}>{['INTRODUCTORY', 'INTERMEDIATE', 'ADVANCED'].map(v => <option key={v}>{v}</option>)}</select></label>
        <label>{t('Estimated minutes')}<input required type="number" min="1" max="4800" value={draft.metadata.estimatedMinutes} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, estimatedMinutes: Number(e.target.value) } })} /></label>
        <JsonField label={t('Translations (EN/VI)')} value={draft.metadata.translations} onChange={translations => edit({ ...draft, metadata: { ...draft.metadata, translations } })} />
        <label>{t('Content origin')}<select value={draft.metadata.provenance.origin ?? 'ORIGINAL'} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, provenance: { ...draft.metadata.provenance, origin: e.target.value } } })}><option value="ORIGINAL">{t('Original content')}</option><option value="LICENSED">{t('Licensed content')}</option></select></label>
        <label>{t('License or authorship statement')}<input value={draft.metadata.provenance.license ?? ''} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, provenance: { ...draft.metadata.provenance, license: e.target.value } } })} /></label>
        {draft.metadata.provenance.origin === 'LICENSED' && <label>{t('Source URL')}<input type="url" value={draft.metadata.provenance.sourceUrl ?? ''} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, provenance: { ...draft.metadata.provenance, sourceUrl: e.target.value } } })} /></label>}
        <label><input type="checkbox" checked={draft.metadata.provenance.reviewedByUserId === me.id} onChange={e => edit({ ...draft, metadata: { ...draft.metadata, provenance: { ...draft.metadata.provenance, schemaVersion: 1, origin: draft.metadata.provenance.origin ?? 'ORIGINAL', reviewedByUserId: e.target.checked ? me.id : null, reviewedAt: e.target.checked ? new Date().toISOString() : null } } })} />{t('I have reviewed the content, answers and publication rights.')}</label>
        {draft.tasks.map((task, i) => <ScenarioTaskEditor key={task.id ?? i} task={task} index={i} skills={skills} onChange={values => taskField(i, values)} onRemove={() => edit({ ...draft, tasks: draft.tasks.filter((_, index) => index !== i).map((tk, index) => ({ ...tk, position: index + 1 })) })} onUpload={async (resource, file) => { if (!file) return; setBusy(true); try { const body = new FormData(); body.set('file', file); const asset = await apiRequest('/v2/files', { method: 'POST', body }); taskField(i, { resources: task.resources.map((r, n) => n === resource ? { ...r, fileAssetId: asset.id, externalUrl: null } : r) }) } catch (e) { setError(e.message) } finally { setBusy(false) } }} />)}
        <button type="button" onClick={() => edit({ ...draft, tasks: [...draft.tasks, blankTask(draft.tasks.length + 1)] })}>{t('Add task')}</button><button>{t('Save draft')}</button>
      </fieldset></form>
      <button disabled={busy || dirty || !version} onClick={() => setPreview(v => !v)}>{t('Preview saved version')}</button>
      {preview && <article><h2>{draft.metadata.title}</h2><p>{draft.metadata.introduction}</p>{draft.tasks.map(task => <section key={task.id}><h3>{task.position}. {task.title}</h3><p>{task.workplaceContext}</p><p>{task.instructions}</p><ul>{task.definitions.map(d => <li key={d.id}>{d.name} · {d.format}</li>)}</ul><p>{task.modelAnswer?.content}</p></section>)}</article>}
      <button disabled={busy || dirty || !preview || version?.status !== 'DRAFT' || !membership.publishingPermission} onClick={() => action('publish')}>{t('Publish reviewed version')}</button>
      <h2>{t('Aggregate participation')}</h2><p>{t('Only aggregate counts are shown; private student evidence requires separate consent.')}</p><ul>{stats.map(s => <li key={s.scenarioVersionId}>{s.title} · V{s.versionNumber}: {s.enrollments} {t('Enrollments')} · {s.participants} {t('Participants')} · {s.completions} {t('Completions')}</li>)}</ul>
    </>}
  </section>
}
