// Provides keyboard-accessible task navigation for an unfinished simulation attempt.
import { ArrowLeft, ArrowRight } from 'lucide-react'

function SimulationNavigation({ currentTaskIndex, totalTasks, onPrevious, onNext }) {
  const isFirstTask = currentTaskIndex === 0
  const isLastTask = currentTaskIndex === totalTasks - 1

  return (
    <nav className="task-navigation" aria-label="Task navigation">
      <button className="button button--secondary" type="button" onClick={onPrevious} disabled={isFirstTask}>
        <ArrowLeft size={17} aria-hidden="true" /> Previous
      </button>
      <p aria-live="polite">Task {currentTaskIndex + 1} of {totalTasks}</p>
      <button className="button button--secondary" type="button" onClick={onNext} disabled={isLastTask}>
        Next <ArrowRight size={17} aria-hidden="true" />
      </button>
    </nav>
  )
}

export default SimulationNavigation
