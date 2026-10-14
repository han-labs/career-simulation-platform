import { useI18n } from '../../../shared/i18n/useI18n.js'
import { Check, Compass, ShieldCheck, Sparkles, Terminal, BarChart3, Bot } from 'lucide-react'
import { Link } from 'react-router-dom'

const keyFeatures = [
  {
    icon: Compass,
    tag: 'Interest Reflection',
    title: 'Holland RIASEC Interest Assessment',
    description:
      'A 42-question self-reflection questionnaire that helps you explore interests across six RIASEC dimensions. Scores are calculated deterministically and shown as exploratory signals.',
    highlights: [
      'Shows all six interest scores',
      'Offers IT directions to explore further',
      'Keeps unfinished answers in this browser',
    ],
    action: { label: 'Career Assessment', to: '/assessment' },
  },
  {
    icon: Terminal,
    tag: 'Learning by Doing',
    title: 'Authentic IT Job Simulations',
    description:
      'Experience how tech roles approach everyday workplace tasks.',
    highlights: [
      'Short, focused workplace scenarios',
      'Objective task results and feedback',
    ],
    action: { label: 'Explore IT Simulations', to: '/simulations' },
  },
  {
    icon: Bot,
    tag: 'Intelligent Companion',
    title: 'Syn Assistant & Exploration Dashboard',
    description:
      'Bring your results together and explore next steps with Syn.',
    highlights: [
      'Reflect on interests and practised skills',
      'Standard guidance when AI is unavailable',
    ],
    action: { label: 'Open Dashboard & Syn', to: '/dashboard' },
  },
]

function FeaturesSection() {
  const { t } = useI18n()
  return (
    <section className="section section--features" aria-labelledby="features-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">
            <Sparkles size={14} aria-hidden="true" /> {t("Core Capabilities")} </span>
          <h2 id="features-heading">{t("Discover. Practise. Reflect.")}</h2>
          <p className="section-heading__sub"> {t("Three ways to explore your career options.")} </p>
        </div>

        <div className="features-grid">
          {keyFeatures.map((feature) => {
            const Icon = feature.icon
            return (
              <article className="feature-card" key={feature.title}>
                <div className="feature-card__badge">
                  <Icon size={20} aria-hidden="true" />
                  <span>{t(feature.tag)}</span>
                </div>
                <h3>{t(feature.title)}</h3>
                <p className="feature-card__desc">{t(feature.description)}</p>
                <ul className="feature-card__list">
                  {feature.highlights.map((item) => (
                    <li key={item}>
                      <Check size={16} aria-hidden="true" />
                      <span>{t(item)}</span>
                    </li>
                  ))}
                </ul>
                <div className="feature-card__action">
                  <Link className="button button--secondary button--small" to={feature.action.to}>
                    {t(feature.action.label)}
                  </Link>
                </div>
              </article>
            )
          })}
        </div>

        <div className="features-trust-banner">
          <div className="features-trust-banner__item">
            <ShieldCheck size={22} aria-hidden="true" />
            <div>
              <strong>{t("Objective & Transparent")}</strong>
              <small>{t("Scores follow fixed rules. AI does not change them.")}</small>
            </div>
          </div>
          <div className="features-trust-banner__item">
            <BarChart3 size={22} aria-hidden="true" />
            <div>
              <strong>{t("Your choices stay yours")}</strong>
              <small>{t("Guidance helps you explore; it does not decide for you.")}</small>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default FeaturesSection
