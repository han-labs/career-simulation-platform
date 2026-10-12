function BrandMark({ compact = false }) {
  return (
    <span className={`brand-mark ${compact ? 'brand-mark--compact' : ''}`}>
      <img
        src="/images/hcmute-logo.png"
        alt="Ho Chi Minh City University of Technology and Education - HCM-UTE Logo"
        className="brand-mark__logo"
        width={compact ? 36 : 46}
        height={compact ? 46 : 59}
        loading="eager"
      />
      <span className="brand-mark__info">
        <span className="brand-mark__institution">HCM-UTE</span>
        <span className="brand-mark__title">
          Career Simulation Platform
        </span>
        <span className="brand-mark__sub">CareerSim</span>
      </span>
    </span>
  )
}

export default BrandMark
