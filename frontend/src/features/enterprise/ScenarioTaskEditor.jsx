import { useI18n } from '../../shared/i18n/useI18n.js'
import JsonField from '../../shared/ui/JsonField.jsx'

const configs = {
  TEXT: { schemaVersion: 1, minLength: 1, maxLength: 10000 }, URL: { schemaVersion: 1, schemes: ['https'], maxLength: 2000 },
  FILE: { schemaVersion: 1, maxBytes: 10485760, allowedMimeTypes: ['text/plain', 'application/pdf'] },
  JSON: { schemaVersion: 1, jsonSchema: { type: 'object', properties: { choice: { type: 'string', enum: ['A', 'B'] } }, required: ['choice'], additionalProperties: false } },
}
export default function ScenarioTaskEditor({ task, index, onChange, onRemove, skills, onUpload }) {
  const { t } = useI18n()
  function definition(position, change) { onChange({ definitions: task.definitions.map((d, i) => i === position ? { ...d, ...change } : d) }) }
  function add(format) { onChange({ definitions: [...task.definitions, { id: null, position: task.definitions.length + 1, name: format === 'JSON' ? 'Choices' : 'Response', format, required: true, validationConfig: configs[format] }] }) }
  const choice = task.definitions.find(d => d.format === 'JSON')
  return <fieldset><legend>{t('Task')} {index + 1}</legend>
    <label>{t('Title')}<input required value={task.title} onChange={e => onChange({ title: e.target.value })} /></label><label>{t('Workplace context')}<textarea required value={task.workplaceContext} onChange={e => onChange({ workplaceContext: e.target.value })} /></label><label>{t('Instructions')}<textarea required value={task.instructions} onChange={e => onChange({ instructions: e.target.value })} /></label>
    <label>{t('Task type')}<select value={task.taskType ?? 'PRACTICAL'} onChange={e => onChange({ taskType: e.target.value, rubric: null })}>{['PRACTICAL', 'TEXT', 'QUIZ'].map(v => <option key={v}>{v}</option>)}</select></label>
    <label><input type="checkbox" checked={task.required} onChange={e => onChange({ required: e.target.checked })} />{t('Required task')}</label>
    <p>{t('New ordered versions require all tasks. Deliverables can be optional.')}</p>
    {task.definitions.map((d, i) => <fieldset key={d.id ?? i}><legend>{t('Deliverable')} {i + 1}</legend>
      <label>{t('Name')}<input required maxLength={180} value={d.name} onChange={e => definition(i, { name: e.target.value })} /></label>
      <label>{t('Format')}<select value={d.format} onChange={e => definition(i, { format: e.target.value, validationConfig: configs[e.target.value] })}>{Object.keys(configs).map(v => <option key={v}>{v}</option>)}</select></label>
      <label><input type="checkbox" checked={d.required} onChange={e => definition(i, { required: e.target.checked })} />{t('Required deliverable')}</label>
      {d.format === 'TEXT' && <label>{t('Maximum characters')}<input type="number" required min="1" max="10000" value={d.validationConfig.maxLength} onChange={e => definition(i, { validationConfig: { ...d.validationConfig, maxLength: Number(e.target.value) } })} /></label>}
      <details><summary>{t('Advanced validation')}</summary><JsonField label={t('Validation configuration')} value={d.validationConfig} onChange={validationConfig => definition(i, { validationConfig })} /></details>
      <button type="button" onClick={() => onChange({ definitions: task.definitions.filter((_, n) => n !== i).map((item, n) => ({ ...item, position: n + 1 })), rubric: null })}>{t('Remove deliverable')}</button>
    </fieldset>)}
    <button type="button" onClick={() => add('TEXT')}>{t('Add deliverable')}</button>{task.taskType === 'QUIZ' && <button type="button" onClick={() => add('JSON')}>{t('Add quiz choices')}</button>}
    {task.taskType === 'QUIZ' && <><label><input type="checkbox" checked={Boolean(task.rubric)} disabled={!choice} onChange={e => onChange({ rubric: e.target.checked ? { version: 1, evaluatorKey: 'quiz-exact/1', quizScoringConfig: { schemaVersion: 1, definitionPosition: choice.position, answers: { choice: 'A' } } } : null })} />{t('Enable reviewed quiz scoring')}</label>
      {task.rubric && <details open><summary>{t('Reviewed quiz rubric (optional)')}</summary><JsonField label={t('Correct choices')} value={task.rubric.quizScoringConfig.answers} onChange={answers => onChange({ rubric: { ...task.rubric, quizScoringConfig: { ...task.rubric.quizScoringConfig, answers } } })} /></details>}</>}
    <label>{t('Model answer')}<textarea value={task.modelAnswer?.content ?? ''} onChange={e => onChange({ modelAnswer: e.target.value ? { content: e.target.value, translations: task.modelAnswer?.translations ?? {} } : null })} /></label>
    <fieldset><legend>{t('Practiced skills')}</legend>{skills.map(skill => <label key={skill.id}><input type="checkbox" checked={task.skillIds.includes(skill.id)} onChange={e => onChange({ skillIds: e.target.checked ? [...task.skillIds, skill.id] : task.skillIds.filter(id => id !== skill.id) })} />{skill.name}</label>)}</fieldset>
    {task.resources.map((r, i) => <fieldset key={r.id ?? i}><legend>{t('Task resource')} {i + 1}</legend><label>{t('Title')}<input required value={r.title} onChange={e => onChange({ resources: task.resources.map((item, n) => n === i ? { ...item, title: e.target.value } : item) })} /></label><label>{t('Resource URL')}<input type="url" value={r.externalUrl ?? ''} onChange={e => onChange({ resources: task.resources.map((item, n) => n === i ? { ...item, externalUrl: e.target.value, fileAssetId: null } : item) })} /></label><label>{t('Upload resource')}<input type="file" accept=".txt,.pdf,.png,.jpg,.jpeg" onChange={e => onUpload(i, e.target.files[0])} /></label><label>{t('Release policy')}<select value={r.releasePolicy} onChange={e => onChange({ resources: task.resources.map((item, n) => n === i ? { ...item, releasePolicy: e.target.value } : item) })}><option value="TASK_ACCESS">{t('When task is accessible')}</option><option value="AFTER_SUBMISSION">{t('After final submission (private file)')}</option></select></label><button type="button" onClick={() => onChange({ resources: task.resources.filter((_, n) => n !== i).map((item, n) => ({ ...item, position: n + 1 })) })}>{t('Remove resource')}</button></fieldset>)}
    <button type="button" onClick={() => onChange({ resources: [...task.resources, { id: null, position: task.resources.length + 1, title: '', externalUrl: '', fileAssetId: null, releasePolicy: 'TASK_ACCESS' }] })}>{t('Add resource')}</button>
    <details><summary>{t('Translations and advanced task settings')}</summary><JsonField label={t('Translations (EN/VI)')} value={task.translations} onChange={translations => onChange({ translations })} /><JsonField label={t('Model answer and translations')} value={task.modelAnswer} onChange={modelAnswer => onChange({ modelAnswer })} /><JsonField label={t('Reviewed quiz rubric (optional)')} value={task.rubric} onChange={rubric => onChange({ rubric })} /></details>
    <button type="button" onClick={onRemove}>{t('Remove task')}</button>
  </fieldset>
}
