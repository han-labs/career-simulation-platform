import { useI18n } from '../../../shared/i18n/useI18n.js'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import SourcePassages from '../agentPlatform/SourcePassages.jsx'

// Presentation only: no request, conversation, memory, impersonation or Student tool access.
export default function StudentExperiencePreview({ agent, selected, published, execution }) {
  const { t } = useI18n()
  const successful = selected && execution?.status === 'SUCCEEDED' && execution.executionMode === 'ADMIN_PREVIEW' && execution.configurationId === selected.id && execution.configurationHash === selected.contentHash && execution.outputSnapshot?.outcome === 'ANSWER'
  const capabilities = (selected?.toolPolicy.enabled ?? []).map(code => t(({ 'kb.search': 'Knowledge search', 'assessment.lookup': 'Assessment evidence', 'simulation.explore': 'Simulation catalogue', 'simulation.myEvidence': 'My simulation evidence' })[code] ?? code))
  const passages = [...new Map((successful ? execution.toolStepsAndSources ?? [] : []).flatMap(step => step.sources ?? []).map(passage => [passage.chunkId, passage])).values()]
  return <section className="cs-surface cs-stack student-experience-preview" aria-label={t('Student Experience Preview')}>
    <header><p className="agent-eyebrow">{t('PRESENTATION ONLY')}</p><h3>{t('Student Experience Preview')}</h3></header>
    <p className="cs-caption">{t('A visual example only. No Student is impersonated and no Student conversation, message or memory is created.')}</p>
    <article className="student-agent-shell cs-stack"><header><h4>{agent.name}</h4><p>{agent.purpose}</p></header>
      <dl className="cs-metadata"><div><dt>{t('Selected saved version')}</dt><dd>{selected ? `${t('Version')} ${selected.versionNumber}` : t('Not saved')}</dd></div><div><dt>{t('Published version')}</dt><dd>{published ? `${t('Version')} ${published.versionNumber}` : t('Not published')}</dd></div></dl>
      <p className="cs-caption">{t('Student access requires publication, an active Agent and an active role binding. This preview grants no access.')}</p>
      {selected && <div><p>{t('Granted knowledge bases')}: {selected.knowledgeBaseIds.length}</p><p>{t('Enabled capabilities')}: {capabilities.length ? capabilities.join(', ') : t('No domain tools enabled')}</p></div>}
      {successful ? <section className="agent-message agent-message--agent cs-stack" aria-label={t('Example Agent answer')}><p className="cs-caption">{t('Example from a successful Admin Preview of the selected saved version. This is not a personalized Student response.')}</p><p className="cs-prose">{execution.outputSnapshot.text}</p><SourcePassages presentation="citations" passages={passages} citations={execution.outputSnapshot.citations ?? []} /></section> : <FeedbackPanel kind="empty">{t('No successful answer for this exact version is available. Run Admin Preview to see an example here.')}</FeedbackPanel>}
    </article>
  </section>
}
