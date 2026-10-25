import { useEffect, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import Button from '../../../shared/ui/Button.jsx'
import Dialog from '../../../shared/ui/Dialog.jsx'
import FeedbackPanel from '../../../shared/ui/FeedbackPanel.jsx'
import FormField from '../../../shared/ui/FormField.jsx'
import Tabs from '../../../shared/ui/Tabs.jsx'
import SourcePassages from '../agentPlatform/SourcePassages.jsx'
import KnowledgeStatus from './KnowledgeStatus.jsx'
import { kbPath } from '../agentPlatform/platformApi.js'

// The keyed parent fences exact KB/version/job identity and every late response.
export default function KnowledgeDocumentInspector({ kb, selection, onReviewed, onClose, onPublish, publishDisabled = true, onRetire }) {
  const { t } = useI18n(), [detail, setDetail] = useState(null), [error, setError] = useState(''), [tab, setTab] = useState('text')
  const [generation, setGeneration] = useState(null)
  const { kind, id, assetKey, revision } = selection
  useEffect(() => {
    let active = true
    const path = kind === 'attempt' ? kbPath(kb) + '/ingestions/' + id : kbPath(kb) + '/assets/' + encodeURIComponent(assetKey) + '/versions/' + id
    apiRequest(path).then(value => { if (active) { setDetail(value); if (kind === 'version') onReviewed({ id, revision }) } }).catch(failure => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [kb, kind, id, assetKey, revision, onReviewed])
  const attempts = detail?.attempts ?? []
  const attempt = attempts.find(item => String(item.generation) === String(generation)) ?? attempts.at(-1)
  const snapshot = kind === 'version' ? detail?.provenance?.extractionSnapshot : attempt?.extractionSnapshot
  const warnings = snapshot?.warnings ?? []
  const metadata = [
    ['Stable asset key', detail?.assetKey], ['Version', kind === 'version' ? detail?.versionNumber : null],
    ['Attempt generation', kind === 'attempt' ? attempt?.generation : null], ['Source license or permission', detail?.license ?? detail?.metadata?.license],
    ['Content language', detail?.language ?? detail?.metadata?.language], ['Reviewer', detail?.reviewer],
    ['Parser profile', snapshot?.parserProfile ?? detail?.provenance?.parserProfile], ['Text unit', snapshot?.textUnit],
    ['Content hash', detail?.contentHash], ['Source HTTPS reference (optional)', detail?.sourceUri ?? detail?.metadata?.sourceUri],
  ].filter(([, value]) => value !== undefined && value !== null && value !== '')
  return <Dialog wide title={t(kind === 'version' ? 'Source version inspection' : 'Ingestion attempt inspection')} description={detail?.title ?? detail?.assetKey ?? id} closeLabel={t('Close dialog')} onRequestClose={onClose} footer={<>{onRetire && <Button variant="danger" disabled={!detail || Boolean(error)} onClick={onRetire}>{t('Retire source version')}</Button>}<Button onClick={onClose}>{t('Close inspector')}</Button>{onPublish && <Button variant="primary" disabled={publishDisabled} onClick={onPublish}>{t('Publish document')}</Button>}</>}>
    <section aria-label={t('Document inspector')} className="cs-stack">
      {!detail ? <FeedbackPanel kind={error ? 'error' : 'loading'}>{t(error || 'Loading document inspection...')}</FeedbackPanel> : <>
        <div className="kb-inspector-identity"><KnowledgeStatus value={detail.status ?? detail.state} /><span className="cs-caption">{t(kind === 'version' ? 'Version' : 'Attempt generation')}: {kind === 'version' ? detail.versionNumber ?? id : attempt?.generation ?? t('Not reported')}</span></div>
        {kind === 'attempt' && <><FormField label={t('Attempt generation')}><select value={attempt?.generation ?? ''} onChange={event => setGeneration(event.target.value)}>{attempts.map(item => <option key={item.generation} value={item.generation}>{item.generation} · {item.stage ?? t('Not reported')}</option>)}</select></FormField>
          {(attempt?.errorCode ?? detail.errorCode) && <FeedbackPanel kind="error" live={false}><p>{attempt?.errorCode ?? detail.errorCode}</p><p>{t('Failed stage')}: {attempt?.stage ?? t('Not reported')}</p></FeedbackPanel>}</>}
        <Tabs label={t('Inspection views')} value={tab} onChange={setTab} items={[
          { value: 'text', label: t('Extracted text'), content: snapshot ? <div className="cs-stack">
            <p className="cs-caption">{t('Normalized extraction snapshot')}</p>
            <div className="kb-extraction" aria-label={t('Normalized extracted text')}><div className="cs-prose">{snapshot.blocks?.map((block, index) => <section key={index}>{block.section && <h4>{block.section}</h4>}<p>{block.text}</p></section>)}</div></div>
            <details className="cs-disclosure"><summary>{t('Extraction locators and warnings')}{warnings.length > 0 ? ' (' + warnings.length + ')' : ''}</summary>
              {warnings.length ? <ul className="kb-warning-list">{warnings.map((warning, index) => <li key={index}>{typeof warning === 'string' ? warning : warning.message ?? warning.code ?? t('Not reported')}</li>)}</ul> : <p className="cs-caption">{t('No extraction warnings recorded.')}</p>}
              <dl className="cs-metadata">{snapshot.blocks?.map((block, index) => <div key={index}><dt>{block.section ?? t('Source locator and profile')}</dt><dd>{Object.entries(block.locator ?? {}).filter(([, value]) => typeof value !== 'object').map(([key, value]) => t(key) + ': ' + value).join(' · ') || t('Not reported')}</dd></div>)}</dl>
              <details className="cs-disclosure"><summary>{t('Technical details')}</summary><pre tabIndex={0} aria-label={t('Extraction locators and warnings')}>{JSON.stringify({ locators: snapshot.blocks?.map(block => ({ section: block.section, locator: block.locator })), warnings }, null, 2)}</pre></details>
            </details>
          </div> : <FeedbackPanel kind="info">{t('No extraction snapshot was recorded for this version or attempt. Chunk text is not the original extraction snapshot.')}</FeedbackPanel> },
          { value: 'chunks', label: t('Chunks'), content: kind === 'version' ? <SourcePassages presentation="studio" passages={detail.chunks} /> : <FeedbackPanel kind="info">{t('Attempt extraction is not an indexed version. Inspect its produced version for stored chunks.')}</FeedbackPanel> },
        ]} />
        <details className="cs-disclosure"><summary>{t('Version or attempt provenance')}</summary><dl className="cs-metadata">{metadata.map(([label, value]) => <div key={label}><dt>{t(label)}</dt><dd>{String(value)}</dd></div>)}</dl>
          <details className="cs-disclosure"><summary>{t('Technical details')}</summary><p className="cs-caption">{t('Source version')}: <code>{id}</code></p><pre tabIndex={0} aria-label={t('Technical details')}>{JSON.stringify(kind === 'version' ? { ...detail, chunks: undefined, provenance: { ...detail.provenance, extractionSnapshot: undefined } } : { metadata: detail.metadata, generation: attempt?.generation, usage: { ...attempt, extractionSnapshot: undefined } }, null, 2)}</pre></details>
        </details>
      </>}
    </section>
  </Dialog>
}
