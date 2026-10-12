import { Code2, Server, Database, CheckSquare, Shield, ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

const tracks = [
  {
    code: 'BACKEND_DEVELOPMENT',
    name: 'Backend & API Engineering',
    icon: Server,
    colorClass: 'track--backend',
    desc: 'Build server-side business logic, design REST APIs, query databases, and diagnose system errors.',
    skills: ['RESTful API', 'SQL', 'Error Diagnosis'],
    simSlug: 'backend-api-triage',
  },
  {
    code: 'FRONTEND_DEVELOPMENT',
    name: 'Frontend & Web Development',
    icon: Code2,
    colorClass: 'track--frontend',
    desc: 'Craft interactive user interfaces, optimize responsiveness, and ensure digital accessibility (a11y).',
    skills: ['HTML/CSS/JS', 'Accessibility', 'Responsive Design'],
    simSlug: 'frontend-accessibility-review',
  },
  {
    code: 'DATA_ANALYSIS',
    name: 'Data Analysis & Engineering',
    icon: Database,
    colorClass: 'track--data',
    desc: 'Clean messy datasets, discover anomalies, and extract evidence-based insights to support decisions.',
    skills: ['Data Cleaning', 'Anomaly Detection', 'Applied Statistics'],
    simSlug: 'data-quality-investigation',
  },
  {
    code: 'SOFTWARE_TESTING',
    name: 'Software Testing & QA/QC',
    icon: CheckSquare,
    colorClass: 'track--testing',
    desc: 'Design comprehensive test scenarios, pinpoint system defects, and safeguard software release quality.',
    skills: ['Test Case Design', 'Bug Reporting', 'Regression Testing'],
    simSlug: 'software-testing-foundations',
  },
  {
    code: 'CYBERSECURITY',
    name: 'Information Security & Cybersecurity',
    icon: Shield,
    colorClass: 'track--cyber',
    desc: 'Identify vulnerabilities, configure least-privilege permissions, and defend data against digital threats.',
    skills: ['Access Control', 'Vulnerability Triage', 'Security Principles'],
    simSlug: 'security-fundamentals',
  },
]

function TracksSection() {
  return (
    <section className="section section--tracks" aria-labelledby="tracks-heading">
      <div className="section-shell">
        <div className="section-heading">
          <span className="eyebrow">Diverse Specializations</span>
          <h2 id="tracks-heading">5 Core Information Technology Pathways</h2>
          <p className="section-heading__sub">
            Explore prominent specialization fields in IT at HCM-UTE. Each pathway exercises distinct cognitive strengths and practical skills.
          </p>
        </div>

        <div className="tracks-grid">
          {tracks.map((track) => {
            const Icon = track.icon
            return (
              <article className={`track-card ${track.colorClass}`} key={track.code}>
                <div className="track-card__icon" aria-hidden="true">
                  <Icon size={24} />
                </div>
                <h3>{track.name}</h3>
                <p>{track.desc}</p>
                <div className="track-card__skills">
                  {track.skills.map((skill) => (
                    <span className="skill-pill" key={skill}>
                      {skill}
                    </span>
                  ))}
                </div>
                <Link
                  className="track-card__link"
                  to={`/simulations/${track.simSlug}`}
                >
                  Try Simulation <ArrowRight size={14} aria-hidden="true" />
                </Link>
              </article>
            )
          })}
        </div>

        <div className="tracks-footer-cta">
          <p>Not sure which specialization fits you best?</p>
          <Link className="button button--primary button--small" to="/assessment">
            Take RIASEC Assessment Now <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>
      </div>
    </section>
  )
}

export default TracksSection
