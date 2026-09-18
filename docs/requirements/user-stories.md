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

## US-03 Explore evidence with Syn

As a student, I want to interact with Syn, an evidence-grounded exploration assistant, so that I can reflect on my assessment and simulation experiences, understand what I have practised, and form my own possible next steps without the system making a career decision for me.

Acceptance baseline:

- The Dashboard concisely presents progress, RIASEC signals, practised skills, recent results, and the current exploration plan.
- Syn introduces its supporting role and explains RIASEC or simulation results only from authorized evidence.
- A new student with no personal evidence receives honest onboarding from reviewed CareerSim paths and simulations; Syn does not fabricate strengths, gaps, or fit.
- Syn can answer in English or Vietnamese, including common Vietnamese shorthand and IT abbreviations, while keeping the product interface in English.
- Syn can compare interests, practised skills, and areas that need more evidence without certifying competence.
- Result details appear as structured cards inside Syn and highlight evidence relevant to the current conversation.
- Syn presents simulations, preparation activities, reflective questions, and plan drafts as options rather than final recommendations.
- Syn uses bounded session memory to support contextual follow-up without retaining unrestricted raw conversation history.
- Syn can answer career-exploration questions, compare exploration paths, identify possible evidence gaps, and suggest reviewed learning resources using the student's available evidence.
- Syn may select from an approved set of read-only evidence, catalog, comparison, gap, and resource tools; it cannot query arbitrary data or perform autonomous background actions.
- Syn exposes a concise activity summary and evidence references for agent-assisted responses without revealing hidden chain-of-thought.
- Saving an exploration plan requires explicit student confirmation.
- Syn never calculates or modifies scores, reveals answer keys, predicts career success, or chooses a career for the student.
- AI and deterministic fallback provenance are visible, and objective results remain available and unchanged when AI is unavailable.
- Response depth adapts within bounded limits: simple evidence questions stay concise while onboarding, comparison, and planning questions may be more explanatory.
- Submitting a message anchors that question in view. A growing reply does not force the conversation to the bottom; the student can choose **Jump to latest**. Reduced-motion users receive the complete validated reply without animation.

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
