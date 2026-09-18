import { ClipboardList, Loader2 } from 'lucide-react'

function AssessmentIntro({ onStartOrResume, isLoading, isReady, status }) {
  return (
    <div className="feature-page__panel" style={{ background: 'var(--card)', borderRadius: '28px', padding: '32px', maxWidth: '720px', margin: '16px auto', border: '1px solid var(--line)', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
      <span className="feature-page__icon" style={{ background: 'var(--sage)', color: 'var(--sage-ink)', display: 'inline-flex', padding: '16px', borderRadius: '16px', marginBottom: '24px' }}>
        <ClipboardList size={32} />
      </span>
      <span className="eyebrow" style={{ color: 'var(--muted)', textTransform: 'uppercase', fontSize: '12px', fontWeight: 'bold', letterSpacing: '1px', display: 'block', marginBottom: '8px' }}>RIASEC Assessment</span>
      <h1 style={{ fontFamily: 'Georgia, serif', fontSize: '3rem', margin: '0 0 16px 0', color: 'var(--ink)' }}>Discover Your Path</h1>
      <p style={{ color: 'var(--muted)', fontSize: '1.1rem', lineHeight: '1.7', marginBottom: '32px' }}>
        This 42-question assessment helps you explore your interests across six dimensions: 
        Realistic, Investigative, Artistic, Social, Enterprising, and Conventional.
        Taking roughly 5-10 minutes, your score will provide objective directions to explore further in tech careers.
      </p>
      <div className="feature-page__contract" style={{ background: 'var(--page-bg)', padding: '16px 20px', borderRadius: '12px', marginBottom: '32px', border: '1px solid var(--line)' }}>
        <strong style={{ display: 'block', marginBottom: '4px', color: 'var(--ink)' }}>Implementation guardrail</strong>
        <span style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>The score is calculated by approved deterministic rules, not by AI.</span>
      </div>
      <button 
        className="button" 
        onClick={onStartOrResume}
        disabled={isLoading || !isReady}
        style={{ background: 'var(--ink)', color: 'white', padding: '16px 32px', borderRadius: '12px', fontSize: '1.1rem', fontWeight: 'bold', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px' }}
      >
        {isLoading ? <><Loader2 className="animate-spin" size={20} /> Loading...</> : (status === 'in_progress' ? 'Resume Assessment' : 'Start Assessment')}
      </button>
    </div>
  )
}

export default AssessmentIntro
