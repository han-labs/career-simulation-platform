# UC-05 — Complete Simulation (US-05 and US-06): UAT Checklist

**Status note:** Results below are **NOT RUN** for final browser UAT unless separately evidenced. Automated component, HTTP, and Testcontainers passes do not establish browser UAT PASS. Use newly enrolled simulation versions with valid FILE policies when testing uploads.

### TC-05-01 — Start and Resume
From the catalogue, start a simulation, leave the workspace, and resume it.

**Expected:** The correct simulation opens and the current enrollment and progress are preserved.

**Result:** NOT RUN

### TC-05-02 — Task sidebar and progression states
Inspect the ordered task list, current task, completed/reviewed tasks, and locked tasks.

**Expected:** The active task is clear, and unavailable tasks cannot be opened prematurely.

**Result:** NOT RUN

### TC-05-03 — Task instructions and resources
Read the background, learning objectives, step-by-step instructions, reference resources, and required submission format.

**Expected:** Required information is visible, HTML is safely formatted, and resource links work appropriately.

**Result:** NOT RUN

### TC-05-04 — Save and restore a TEXT draft
Enter a task response, save a draft, leave the workspace, and resume.

**Expected:** The response is restored without being marked FINAL.

**Result:** NOT RUN

### TC-05-05 — Upload a FILE deliverable
On a new enrollment using a scenario version with configured FILE policies, upload a file of an allowed format.

**Expected:** The actual file is accepted and stored server-side; MIME, extension, content, and size rules are enforced.

**Result:** NOT RUN

### TC-05-06 — Save and restore a FILE draft
Upload a file, save a draft, leave the workspace, and resume in a new tab.

**Expected:** The saved file remains linked to the task draft, not merely represented by a filename in browser storage.

**Result:** NOT RUN

### TC-05-07 — Invalid submission
Attempt an empty response, an unsupported file type, or an otherwise invalid task response.

**Expected:** A clear validation error appears; no FINAL submission or model-answer access is granted.

**Result:** NOT RUN

### TC-05-08 — FINAL submission and retry
Submit a valid response, confirm the action, and retry if the interface allows.

**Expected:** Exactly one immutable FINAL submission is recorded; retries do not create duplicates.

**Result:** NOT RUN

### TC-05-09 — Example answer access
Attempt to view an example answer before and after a valid FINAL submission.

**Expected:** The example answer is protected before FINAL and accessible afterward for that task.

**Result:** NOT RUN

### TC-05-10 — Self-review and Next Task
Attempt Next before self-review confirmation, then confirm self-review and select Next.

**Expected:** Progression is blocked until confirmation; afterward, the correct next task becomes available.

**Result:** NOT RUN

### TC-05-11 — Reload and Resume after FINAL
Reload the page, reopen the workspace, and resume after a FINAL submission and self-review.

**Expected:** There is no white screen, saved state is preserved, and FINAL submission is not duplicated.

**Result:** NOT RUN

### TC-05-12 — Zero-score quiz
Submit a valid objectively scored quiz response that receives zero points.

**Expected:** The submission is valid and progression is not blocked by the zero score.

**Result:** NOT RUN

### TC-05-13 — Complete the simulation
Finish all required tasks and confirm the last required self-review.

**Expected:** The simulation is marked completed and the results become accessible without duplicate completion records.

**Result:** NOT RUN

### TC-06-01 — Completed tasks and submission statuses
Open the results page of a completed simulation.

**Expected:** Completed tasks and their recorded submission statuses are displayed accurately.

**Result:** NOT RUN

### TC-06-02 — Objective quiz scores
Inspect results containing both quiz and practical tasks.

**Expected:** Numeric scores appear only for tasks supporting objective scoring.

**Result:** NOT RUN

### TC-06-03 — Practiced skills
Inspect skills shown on the completed simulation results page.

**Expected:** Skills are linked to completed tasks, not assigned using score thresholds.

**Result:** NOT RUN

### TC-06-04 — Completion evidence
Open an eligible completion record or resume snippet, when available.

**Expected:** Evidence accurately describes participation in a simulated experience and does not claim professional certification or competence. Ineligible or unavailable evidence is omitted.

**Result:** NOT RUN

### TC-06-05 — Private results
Sign out or use another Student account to open the results URL.

**Expected:** The owner can view the results; anonymous users are denied; other students cannot view private results.

**Result:** NOT RUN — HTTP authorization tests passed previously (owner 200, anonymous 401, other student 404), but final browser UAT has not been confirmed.

### TC-06-06 — Results loading error
Simulate or encounter a controlled results-loading failure.

**Expected:** A clear error and retry action appear, without a blank screen or leaked private data.

**Result:** NOT RUN

---

**Acceptance status:** UC-05 / US-05 / US-06 browser UAT remains open. Previous automated tests passed targeted workflow and privacy checks, but these must not be recorded as manual UAT PASS.
