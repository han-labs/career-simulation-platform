# Security Privacy and Product AI Rules

## Authorization

- Backend authorization is mandatory for every protected read and write.
- Student ownership and enterprise content ownership must be checked in service/domain policy.
- Account status is revalidated for protected actions.
- Public catalog reads expose published metadata only.
- Evaluation configuration, answer keys, drafts, and private results are never public.

## Data minimization

- Send only evidence necessary for the requested explanation.
- Prefer scores and task outcomes over raw free text when raw text adds no value.
- Exclude credentials, internal IDs that are not needed, unrelated history, and other users' data.
- Respect recorded consent before external AI processing.
- Define retention/deletion before collecting additional sensitive profile data.

## AI boundary

- The LLM has no database connection or unrestricted retrieval tool.
- System instructions and schema constraints cannot be overridden by student input.
- Treat user and simulation content as untrusted data, not instructions.
- Validate response structure, length, allowed fields, and safety policy.
- Do not make claims of certainty, diagnosis, guaranteed employment, or mandated career choice.
- Provider exceptions are mapped to internal categories; raw provider errors are not exposed.
- A timeout and deterministic fallback are required before enabling a provider.

## Secrets and logs

- Keys remain backend environment variables or approved secret storage.
- Never prefix a backend secret with `VITE_`.
- Logs must not contain credentials, full prompts with private data, raw model responses with private data, or assessment answer payloads.
- Test fixtures are synthetic.

## Security tests

- unauthenticated and wrong-role access;
- horizontal ownership access;
- inactive/locked accounts;
- answer-key/evaluation-rule non-disclosure;
- prompt-injection/out-of-domain handling;
- malformed provider response;
- secret scan and safe error serialization.
