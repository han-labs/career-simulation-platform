// Presents one mock simulation without rendering answer keys or explanations.
import { ArrowLeft, ArrowRight, Clock3, ListChecks } from 'lucide-react'
import { Link } from 'react-router-dom'

function SimulationDetail({ simulation }) {
  return (
    <article className="detail-panel">
      <nav aria-label="Breadcrumb" className="breadcrumb">
        <Link to="/simulations">Catalog</Link><span aria-hidden="true">/</span><span>{simulation.title}</span>
      </nav>
      <p className="eyebrow">{simulation.difficulty}</p>
      <h1>{simulation.title}</h1>
      <p className="detail-panel__lede">{simulation.description}</p>
      <dl className="detail-meta">
        <div><dt><Clock3 size={17} aria-hidden="true" /> Duration</dt><dd>{simulation.duration}</dd></div>
        <div><dt><ListChecks size={17} aria-hidden="true" /> Tasks</dt><dd>{simulation.taskCount}</dd></div>
      </dl>
      <section className="detail-tasks" aria-labelledby="task-list-title">
        <h2 id="task-list-title">What you will work on</h2>
        <ol>{simulation.tasks.map((task) => <li key={task.id}>{task.title}</li>)}</ol>
      </section>
      <div className="detail-actions">
        <Link className="button button--secondary" to="/simulations"><ArrowLeft size={17} aria-hidden="true" /> Back to catalog</Link>
        <Link className="button" to={`/simulations/${simulation.id}/attempt`}>Start simulation <ArrowRight size={17} aria-hidden="true" /></Link>
      </div>
    </article>
  )
}

export default SimulationDetail
