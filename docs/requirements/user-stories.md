# Essential user stories

## US-01 Complete a RIASEC assessment

As a student, I want to complete a RIASEC-based assessment and receive several IT career directions to explore further.

Acceptance baseline:

- The student can start, resume, and submit an assessment.
- Every answer uses the approved scale and belongs to one of the six RIASEC dimensions.
- Scoring is deterministic, reproducible, and covered by boundary tests.
- The result shows all six scores and clearly identifies the strongest signals.
- Career directions are presented as exploration options, not predictions or final decisions.
- AI may explain a completed score but must not calculate or change it.

## US-02 Complete a career simulation

As a student, I want to complete representative career tasks so that I can experience the work before deciding whether to explore it further.

Acceptance baseline:

- Only published simulations appear in the student catalog.
- The student can start or resume one attempt and complete tasks in a defined order.
- Public task DTOs never expose answer keys or evaluation configuration.
- Submission validation, scoring, and task-level outcomes are deterministic.
- The completed attempt and evaluation result are persisted transactionally.
- The two-minute demo slice supports Backend API Triage with three tasks.
- Any externally researched catalog metadata has a source URL, retrieval date, permitted-use review, and human approval before publication; CareerSim task content remains original or separately licensed.

## US-03 Receive evidence-grounded guidance

As a student, I want the system to analyze my assessment and simulation results and explain strengths, difficulties, and possible next steps without making the career decision for me.

Acceptance baseline:

- The backend constructs a minimal, authorized context from persisted evidence.
- The LLM cannot query the database and never receives secrets or unrelated profile data.
- The response follows a validated schema with strengths, difficulties, evidence references, limitations, and next steps.
- The result identifies whether it came from AI or deterministic fallback.
- Timeout, unavailable provider, malformed output, unsafe output, and empty evidence all have tested behavior.
- Objective scores remain unchanged regardless of AI availability.

## Supporting MVP capabilities

- Authentication, student profiles, roles, and account status.
- Enterprise/content-provider authoring, editing, publishing, and monitoring.
- Persistent PostgreSQL data and append-only Flyway migrations.
- Containerized Ubuntu VM deployment through Docker Compose and Nginx.
- Governed collection of permitted public factual metadata for IT-simulation catalog research in this non-commercial course project, subject to current source terms, `robots.txt`, provenance, rate limits, and human review.

## Out of scope for the initial MVP

- VR/AR or full 3D environments.
- Multiplayer simulations.
- Recruitment or direct job matching.
- A dedicated mobile application.
- Training a proprietary LLM.
- Full offline operation.
- AI-generated objective scoring.
