import { Compass } from 'lucide-react'

function BrandMark({ compact = false }) {
  return (
    <span className={`brand-mark ${compact ? 'brand-mark--compact' : ''}`}>
      <span className="brand-mark__icon" aria-hidden="true">
        <Compass size={compact ? 18 : 22} strokeWidth={2.2} />
      </span>
      <span className="brand-mark__text">
        Career<span>Sim</span>
      </span>
    </span>
  )
}

export default BrandMark
