import { useI18n } from '../../../shared/i18n/useI18n.js'
// Presents one mock simulation without rendering answer keys or explanations.
import { ArrowLeft, ArrowRight, Clock3, ListChecks } from 'lucide-react'
import { Link } from 'react-router-dom'

function SimulationDetail({ simulation }) {
  const { t } = useI18n()
  return (
    <article className="detail-panel">
      <nav aria-label={t("Breadcrumb")} className="breadcrumb">
        <Link to="/simulations">{t("Catalog")}</Link><span aria-hidden="true">/</span><span>{t(simulation.title)}</span>
      </nav>
      <p className="eyebrow">{t(simulation.difficulty)}</p>
      <h1>{t(simulation.title)}</h1>
      <p className="detail-panel__lede">{t(simulation.description)}</p>
      <dl className="detail-meta">
        <div><dt><Clock3 size={17} aria-hidden="true" /> {t("Duration")}</dt><dd>{t(simulation.duration)}</dd></div>
        <div><dt><ListChecks size={17} aria-hidden="true" /> {t("Tasks")}</dt><dd>{t(simulation.taskCount)}</dd></div>
      </dl>
      <section className="detail-tasks" aria-labelledby="task-list-title">
        <h2 id="task-list-title">{t("What you will work on")}</h2>
        <ol>{simulation.tasks.map((task) => <li key={task.id}>{t(task.title)}</li>)}</ol>
      </section>
      <div className="detail-actions">
        <Link className="button button--secondary" to="/simulations"><ArrowLeft size={17} aria-hidden="true" /> {t("Back to catalog")}</Link>
        <Link className="button" to={`/simulations/${simulation.id}/attempt`}>{t("Start simulation")} <ArrowRight size={17} aria-hidden="true" /></Link>
      </div>
    </article>
  )
}

export default SimulationDetail
