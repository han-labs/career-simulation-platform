// Renders the unfinished task workspace without exposing answer keys or explanations.
function SimulationWorkspace({ task, taskIndex, totalTasks, answers, hasTaskError, errorSummaryRef, onAnswerChange }) {

  return (
    <section className="attempt-workspace" aria-labelledby="workspace-title">
      <div className="attempt-section-heading" id="workspace-title">
        <p className="eyebrow">Task workspace</p>
        <h2>Task {taskIndex + 1} of {totalTasks}: {task.title}</h2>
        <p>Your choices are evaluated only when you submit the completed simulation.</p>
      </div>
      {hasTaskError && (
        <p className="form-summary" ref={errorSummaryRef} role="alert" tabIndex="-1">
          This task needs an answer before you can submit.
        </p>
      )}
      <fieldset className="task-card" aria-describedby={`${task.id}-prompt`}>
        <legend>{task.title}</legend>
        <p id={`${task.id}-prompt`}>{task.prompt}</p>
        <div className="task-options">
          {task.options.map((option) => (
            <label className="task-option" key={option.id}>
              <input type="radio" name={task.id} value={option.id} checked={answers[task.id] === option.id} onChange={() => onAnswerChange(task.id, option.id)} aria-invalid={hasTaskError} />
              <span>{option.label}</span>
            </label>
          ))}
        </div>
        {hasTaskError && <p className="field-error">An answer is required for this task.</p>}
      </fieldset>
    </section>
  )
}

export default SimulationWorkspace
