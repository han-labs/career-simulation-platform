import { useState } from 'react'
import { useI18n } from '../../../shared/i18n/useI18n.js'

export default function CompaniesSection({ simulations }) {
  const { t } = useI18n()
  const [missing, setMissing] = useState({})
  const providers = new Map()
  for (const simulation of simulations) {
    if (simulation.enterpriseName && simulation.media?.companyLogoUrl) providers.set(simulation.enterpriseName, simulation.media.companyLogoUrl)
  }
  return <section className="home-companies section-shell" aria-labelledby="companies-heading">
    <h2 id="companies-heading">{t('Featuring career simulations from leading companies')}</h2>
    {providers.size ? <ul tabIndex={0} aria-label={t('Simulation providers')}>
      {[...providers].map(([name, logo]) => <li key={name}>{missing[name] ? name : <img alt={name} src={logo} width="120" height="40" loading="lazy" onError={() => setMissing(current => ({ ...current, [name]: true }))} />}</li>)}
    </ul> : <p>{t('Explore the published simulation catalogue.')}</p>}
  </section>
}
