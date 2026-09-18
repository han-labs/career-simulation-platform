function AssessmentDemoControls({ onDemo }) {
  return (
    <div style={{ display: 'flex', gap: '12px', marginBottom: '24px', justifyContent: 'flex-end' }}>
      <button type="button" onClick={() => onDemo('backend')} className="button button--small button--secondary">Demo Backend Profile</button>
      <button type="button" onClick={() => onDemo('frontend')} className="button button--small button--secondary">Demo Frontend Profile</button>
      <button type="button" onClick={() => onDemo('data')} className="button button--small button--secondary">Demo Data Profile</button>
    </div>
  )
}

export default AssessmentDemoControls
