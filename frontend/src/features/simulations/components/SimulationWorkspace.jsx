import { useI18n } from '../../../shared/i18n/useI18n.js'
// Renders the unfinished task workspace without exposing answer keys or explanations.
function SimulationWorkspace({ task, taskIndex, totalTasks, answers, hasTaskError, errorSummaryRef, onAnswerChange }) {
  const { t } = useI18n()

  return (
    <section className="attempt-workspace" aria-labelledby="workspace-title">
      <div className="attempt-section-heading" id="workspace-title">
        <p className="eyebrow">{t("Task workspace")}</p>
        <h2>{t("Task")} {t(taskIndex + 1)} {t("of")} {t(totalTasks)}: {t(task.title)}</h2>
        <p>{t("Your choices are evaluated only when you submit the completed simulation.")}</p>
      </div>
      {hasTaskError && (
        <p className="form-summary" ref={errorSummaryRef} role="alert" tabIndex="-1"> {t("This task needs an answer before you can submit.")} </p>
      )}
      <fieldset className="task-card" aria-describedby={`${task.id}-prompt`}>
        <legend>{t(task.title)}</legend>
        <p id={`${task.id}-prompt`}>{t(task.prompt)}</p>
        <div className="task-options">
          {task.options.map((option) => (
            <label className="task-option" key={option.id}>
              <input type="radio" name={task.id} value={option.id} checked={answers[task.id] === option.id} onChange={() => onAnswerChange(task.id, option.id)} aria-invalid={hasTaskError} />
              <span>{t(option.label)}</span>
            </label>
          ))}
        </div>
        {hasTaskError && <p className="field-error">{t("An answer is required for this task.")}</p>}
      </fieldset>
    </section>
  )
}

export default SimulationWorkspace
