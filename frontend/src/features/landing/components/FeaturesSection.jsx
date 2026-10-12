import { Check, Compass, ShieldCheck, Sparkles, Terminal, BarChart3, Bot } from 'lucide-react'
import { Link } from 'react-router-dom'

const keyFeatures = [
  {
    icon: Compass,
    tag: 'Scientifically Validated',
    title: 'Holland RIASEC Interest Assessment',
    description:
      'Standardized questionnaire helping you evaluate 6 vocational personality dimensions (R-I-A-S-E-C). The scoring algorithm is deterministic, transparent, and completely unbiased.',
    highlights: [
      'Identifies primary vocational interest traits',
      'Suggests matching technology pathways',
      'Saves progress so you can resume anytime',
    ],
    action: { label: 'Start Assessment', to: '/assessment' },
  },
  {
    icon: Terminal,
    tag: 'Learning by Doing',
    title: 'Authentic IT Job Simulations',
    description:
      'Go beyond reading generic job descriptions: practice hands-on workplace scenarios faced by software engineers, data specialists, and QA testers.',
    highlights: [
      'Realistic tasks achievable within 30-45 minutes',
      'Automated, objective server-side evaluation',
      'Secure evaluation criteria hidden until submission',
    ],
    action: { label: 'Explore IT Simulations', to: '/simulations' },
  },
  {
    icon: Bot,
    tag: 'Intelligent Companion',
    title: 'Syn Assistant & Exploration Dashboard',
    description:
      'Aggregate all assessment and simulation performance data into one single view. Assistant Syn analyzes evidence, clarifies skill gaps, and recommends next learning steps.',
    highlights: [
      'Visual overview of demonstrated strengths and practised skills',
      'Evidence-based reflections without automated judgment',
      'Deterministic fallback guidance when offline or disconnected',
    ],
    action: { label: 'Open Dashboard & Syn', to: '/dashboard' },
  },
]

function FeaturesSection() {
  return (
    <section className="section section--features" aria-labelledby="features-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">
            <Sparkles size={14} aria-hidden="true" /> Core Capabilities
          </span>
          <h2 id="features-heading">A Comprehensive Ecosystem for Career Exploration</h2>
          <p className="section-heading__sub">
            Combining vocational psychology, authentic hands-on workplace scenarios, and transparent AI-assisted guidance by HCM-UTE.
          </p>
        </div>

        <div className="features-grid">
          {keyFeatures.map((feature) => {
            const Icon = feature.icon
            return (
              <article className="feature-card" key={feature.title}>
                <div className="feature-card__badge">
                  <Icon size={20} aria-hidden="true" />
                  <span>{feature.tag}</span>
                </div>
                <h3>{feature.title}</h3>
                <p className="feature-card__desc">{feature.description}</p>
                <ul className="feature-card__list">
                  {feature.highlights.map((item) => (
                    <li key={item}>
                      <Check size={16} aria-hidden="true" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
                <div className="feature-card__action">
                  <Link className="button button--secondary button--small" to={feature.action.to}>
                    {feature.action.label}
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
              <strong>100% Objective &amp; Transparent</strong>
              <small>Scores are calculated via deterministic rules; AI never alters or biases evaluation</small>
            </div>
          </div>
          <div className="features-trust-banner__item">
            <BarChart3 size={22} aria-hidden="true" />
            <div>
              <strong>Student Privacy &amp; Data Protection</strong>
              <small>Your exploration data belongs to you and complies with academic data minimization rules</small>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default FeaturesSection
