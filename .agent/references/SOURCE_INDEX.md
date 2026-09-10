# Supplied Proposal Source Index

## Product intent

- Experiential career exploration rather than career prediction.
- Initial domain: frontend development, backend development, data analysis, software testing, and cybersecurity.
- Core loop: assess interests, perform representative tasks, evaluate performance, and reflect.

## Essential user stories

- US-01: RIASEC assessment and several IT exploration directions.
- US-02: representative career tasks and experiential evidence.
- US-03: evidence-grounded strengths, difficulties, and possible next steps without deciding for the student.

## Architecture decisions

- React + Vite frontend.
- Java 17 + Spring Boot backend.
- PostgreSQL database.
- Modular monolith for the eight-week MVP.
- Docker Compose and Nginx on an Ubuntu Server VM.
- External hosted LLM through a backend-only adapter.
- No frontend-to-LLM or LLM-to-database access.
- Rule/template fallback when the LLM is unavailable, slow, invalid, or unsafe.

## Deterministic boundary

- Authentication, database access, RIASEC scoring, and simulation scoring are non-AI.
- AI adds natural-language interpretation and exploration suggestions only.
- Objective results must remain available without AI.

## Minimum demo slice

Backend Developer simulation with three tasks: interpret an API response, choose an SQL query, and identify a backend error. Persist the attempt/result, then produce validated AI guidance or fallback.

## Evidence expected by the proposal

- Repository URL and exact commit.
- Reproducible README.
- Architecture matching implementation.
- Persistent runnable frontend/backend.
- Complete vertical slice and tests/acceptance evidence.
- At least ten AI evaluation cases and failure analysis.
- AI assistance log and human review evidence.
- Docker/Ubuntu VM deployment evidence.
- Executed smoke-test prompt, actual response, exact model, measured latency, and result.

## Important unresolved item

The proposal mentions a hosted Gemini direction and an example/specific model label in different sections, while also stating that the exact model should be selected after a real smoke test and comparison. Treat provider/model choice and all measurements as unresolved until executed evidence exists.
