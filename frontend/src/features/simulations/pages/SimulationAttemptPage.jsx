// Loads simulation tasks from the API with mock fallback and submits through the API.
import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, CircleCheck, LoaderCircle, TriangleAlert } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import SimulationResult from '../components/SimulationResult.jsx'
import SubmissionConfirmationDialog from '../components/SubmissionConfirmationDialog.jsx'
import SimulationWorkspace from '../components/SimulationWorkspace.jsx'
import SimulationNavigation from '../components/SimulationNavigation.jsx'
import { backendDeveloperSimulation } from '../data/backendDeveloperSimulation.js'
import { fetchSimulationDetail } from '../api/simulationApi.js'
import { useSimulationAttempt } from '../hooks/useSimulationAttempt.js'

function SimulationAttemptPage() {
  const { slug } = useParams()
  const [state, setState] = useState({ status: 'loading', simulation: null })
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const errorSummaryRef = useRef(null)
  const submitButtonRef = useRef(null)

  useEffect(() => {
    let active = true
    fetchSimulationDetail(slug)
      .then((simulation) => {
        if (active) setState({ status: 'ready', simulation })
      })
      .catch((error) => {
        console.warn('Simulation attempt API unavailable; using mock data.', error)
        if (!active) return
        const fallback = slug === backendDeveloperSimulation.id
          ? backendDeveloperSimulation
          : null
        setState({ status: 'ready', simulation: fallback })
      })
    return () => {
      active = false
    }
  }, [slug])

  const simulation = state.simulation
  const attempt = useSimulationAttempt(simulation?.tasks ?? [], slug)
  const currentTask = simulation?.tasks[attempt.currentTaskIndex]
  const hasTaskError = Boolean(
    currentTask
      && attempt.submitAttempted
      && attempt.missingTaskIds.includes(currentTask.id),
  )

  useEffect(() => {
    if (hasTaskError) errorSummaryRef.current?.focus()
  }, [hasTaskError])

  if (state.status === 'loading') {
    return <main className="catalog-page section-shell"><section className="catalog-state"><LoaderCircle className="spin" aria-hidden="true" /><p>Loading simulation...</p></section></main>
  }

  if (!simulation) {
    return (
      <main className="catalog-page section-shell">
        <section className="catalog-state">
          <h1>Simulation not found</h1>
          <Link className="button" to="/simulations">
            <ArrowLeft size={17} aria-hidden="true" /> Back to catalog
          </Link>
        </section>
      </main>
    )
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    setIsDialogOpen(true)
  }

  const handleConfirmSubmit = () => {
    void attempt.submit()
    setIsDialogOpen(false)
  }

  const handleCancelSubmit = () => {
    setIsDialogOpen(false)
    window.setTimeout(() => submitButtonRef.current?.focus(), 0)
  }

  return (
    <main className="attempt-page section-shell">
      <Link className="back-link" to="/simulations">
        <ArrowLeft size={17} aria-hidden="true" /> Back to simulations
      </Link>
      <header className="attempt-intro">
        <p className="eyebrow">Career simulation</p>
        <h1>{simulation.title}</h1>
        <p>Experience three representative backend tasks. Your result appears after you submit.</p>
      </header>
      {attempt.submitError && <p className="form-summary" role="status"><TriangleAlert size={18} aria-hidden="true" /> API submit failed; showing the local deterministic result. {attempt.submitError}</p>}
      {attempt.result ? (
        <SimulationResult result={attempt.result} onRestart={attempt.restart} />
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <SimulationWorkspace
            task={currentTask}
            taskIndex={attempt.currentTaskIndex}
            totalTasks={simulation.tasks.length}
            answers={attempt.answers}
            hasTaskError={hasTaskError}
            errorSummaryRef={errorSummaryRef}
            onAnswerChange={attempt.setAnswer}
          />
          <SimulationNavigation currentTaskIndex={attempt.currentTaskIndex} totalTasks={simulation.tasks.length} onPrevious={attempt.goPrevious} onNext={attempt.goNext} />
          {attempt.currentTaskIndex === simulation.tasks.length - 1 && <footer className="attempt-actions">
            <p><CircleCheck size={18} aria-hidden="true" /> Submit evaluates this mock attempt.</p>
            <button
              className="button"
              type="button"
              ref={submitButtonRef}
              onClick={() => setIsDialogOpen(true)}
            >
              Submit answers
            </button>
          </footer>}
        </form>
      )}
      <SubmissionConfirmationDialog
        open={isDialogOpen}
        onConfirm={handleConfirmSubmit}
        onCancel={handleCancelSubmit}
      />
    </main>
  )
}

export default SimulationAttemptPage
