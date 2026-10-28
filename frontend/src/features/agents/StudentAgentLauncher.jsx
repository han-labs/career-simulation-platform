import { useEffect, useRef, useState } from 'react'
import { Bot, Sparkles } from 'lucide-react'
import { apiRequest } from '../../shared/api/httpClient.js'
import { useI18n } from '../../shared/i18n/useI18n.js'
import Button from '../../shared/ui/Button.jsx'
import FeedbackPanel from '../../shared/ui/FeedbackPanel.jsx'
import StudentAgentChat from './StudentAgentChat.jsx'
import './studentAgent.css'

export default function StudentAgentLauncher({ account, request, onOpen, onClose, onSessionChange }) {
  const { t } = useI18n(), [agents, setAgents] = useState(null), [error, setError] = useState(''), [expanded, setExpanded] = useState(false), [started, setStarted] = useState(false)
  const trigger = useRef(null), popup = useRef(null)
  useEffect(() => { if (request.open) popup.current?.querySelector('button')?.focus() }, [request.open])
  useEffect(() => {
    let active = true
    apiRequest('/v2/agents').then(rows => { if (active) setAgents(Array.isArray(rows) ? rows.filter(agent => agent.runtimeAvailable !== false) : []) }).catch(failure => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [])
  // The route and launcher share this single mounted chat; closing only hides it.
  const agent = request.agentId ? agents?.find(row => row.id === request.agentId) : agents?.[0]
  const open = request.open
  function close() { onClose(); trigger.current?.focus() }
  return <div className="cs-ui student-agent-widget">
    {Boolean(agents?.length) && <Button ref={trigger} className="student-agent-launcher" variant="primary" aria-label={t('Career Agent')} title={t('Career Agent')} aria-expanded={open} aria-controls="student-agent-popup" onClick={() => { if (open) close(); else { setStarted(true); onOpen() } }}><Bot size={25} aria-hidden="true" /><Sparkles className="student-agent-sparkle" size={13} aria-hidden="true" /></Button>}
    {(started || request.activated || open) && <div ref={popup} id="student-agent-popup" className={'student-agent-popup' + (expanded ? ' student-agent-popup--expanded' : '')} data-state={open ? 'open' : 'closed'} aria-hidden={!open} inert={!open ? true : undefined} role="dialog" aria-modal="false" aria-label={t('Career Agent')}>
      {!agent ? <div className="student-chat-empty"><Button onClick={close}>{t('Close')}</Button><FeedbackPanel kind={error ? 'error' : agents ? 'empty' : 'loading'}>{t(error || (agents ? 'No published Agent is active for your role.' : 'Loading available Agent...'))}</FeedbackPanel></div> : <StudentAgentChat key={`${account.id}:${agent.id}`} owner={account.id} visible={open} agent={agent} id={request.sessionId ?? null} onSessionChange={id => onSessionChange(id, agent.id)} onClose={close} expanded={expanded} onExpand={() => setExpanded(value => !value)} />}
    </div>}
  </div>
}
