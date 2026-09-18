function AssessmentProgressBar({ answeredCount, totalCount, progressPercent }) {
  return (
    <div style={{ 
      position: 'sticky', 
      top: '20px', 
      background: 'var(--card)', 
      padding: '16px 24px', 
      borderRadius: '16px', 
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)', 
      zIndex: 10,
      marginBottom: '32px',
      border: '1px solid var(--line)'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.9rem', fontWeight: 'bold', color: 'var(--ink)' }}>
        <span>Progress</span>
        <span>{answeredCount} / {totalCount} completed</span>
      </div>
      <div style={{ height: '8px', background: 'var(--line)', borderRadius: '4px', overflow: 'hidden' }}>
        <div style={{ 
          height: '100%', 
          background: 'var(--tech)', 
          width: `${progressPercent}%`,
          transition: 'width 0.3s ease-out'
        }} />
      </div>
    </div>
  )
}

export default AssessmentProgressBar
