## Feature traceability

- User story / foundation item:
- Owner:
- Source requirement and acceptance criteria:
- Sequence/domain artifact checked:

## Change summary

- Main flow:
- Alternative and exception flows:
- Frontend changes:
- Backend changes:
- Database migration:
- Report/evidence update:

## Verification

- [ ] `cd backend && ./mvnw verify`
- [ ] `cd frontend && npm run check`
- [ ] `docker compose config --quiet`
- [ ] No secrets, private student data, or copied third-party task content
- [ ] API uses DTOs and does not expose entities or evaluation rules
- [ ] AI behavior records provenance and has a tested deterministic fallback

## Known gaps and follow-up

List explicit gaps, deferred decisions, and any report sections that still need evidence.
