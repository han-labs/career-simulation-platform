# AI Assisted Development Rules

This file governs AI used to build the software. Product AI behavior is additionally governed by `security-privacy-and-ai.md`.

## Before accepting generated code

- Confirm the source user story, acceptance criteria, sequence/domain design, and owner.
- Ask for or create a file-level plan and record unresolved assumptions.
- Limit the change to one feature/foundation item.
- Inspect existing code to avoid duplicate entities, endpoints, rules, or migrations.

## Human review requirements

The responsible human must be able to explain:

- why each changed class owns its responsibility;
- every business/security/AI fallback decision;
- the public API and schema impact;
- the tests and their boundaries;
- any copied/generated dependency, query, prompt, or configuration;
- known limitations and deferred work.

Generated code is not evidence until it compiles, tests pass, and the behavior is manually verified where appropriate.

## Prohibited shortcuts

- Invented requirements, results, citations, model names, latency, or test outcomes.
- Large unrelated refactors bundled with a story.
- Silencing tests/lint/architecture gates.
- Returning entities directly because mapping is “too much work.”
- Client-side-only authorization or scoring.
- Secrets in source, prompts, screenshots, logs, tests, or examples.
- Uploading local/private project artifacts to third-party tools without approval.
- Copying third-party task text, model answers, images, or datasets without license/provenance.

## Required log

For substantive AI assistance, copy `templates/ai-assistance-log.md` into `reports/ai/` and record:

- task and source artifacts;
- tool/model if disclosure is required by the course;
- files and decisions influenced;
- commands/tests actually run;
- human verification;
- rejected/modified suggestions;
- remaining limitations.

Never paste tokens, passwords, private prompts/data, or unrestricted raw model transcripts into the log.
