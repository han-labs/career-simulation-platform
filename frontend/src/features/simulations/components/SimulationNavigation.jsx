import { useI18n } from '../../../shared/i18n/useI18n.js'
// Provides keyboard-accessible task navigation for an unfinished simulation attempt.
import { ArrowLeft, ArrowRight } from 'lucide-react'

function SimulationNavigation({ currentTaskIndex, totalTasks, onPrevious, onNext }) {
  const { t } = useI18n()
  const isFirstTask = currentTaskIndex === 0
  const isLastTask = currentTaskIndex === totalTasks - 1

  return (
    <nav className="task-navigation" aria-label={t("Task navigation")}>
      <button className="button button--secondary" type="button" onClick={onPrevious} disabled={isFirstTask}>
        <ArrowLeft size={17} aria-hidden="true" /> {t("Previous")} </button>
      <p aria-live="polite">{t("Task")} {t(currentTaskIndex + 1)} {t("of")} {t(totalTasks)}</p>
      <button className="button button--secondary" type="button" onClick={onNext} disabled={isLastTask}> {t("Next")} <ArrowRight size={17} aria-hidden="true" />
      </button>
    </nav>
  )
}

export default SimulationNavigation
