# US-03 Sequence to Code Trace

Source user story/use-case: `docs/requirements/user-stories.md` US-03  
Sequence artifact: approved Dashboard/Syn interaction flow recorded in the feature plan  
Implementation commit: pending

| Step/message | Actor/lifeline | Endpoint/action | Method owner | Layer/module | Data/provider interaction | Status |
|---|---|---|---|---|---|---|
| Load dashboard | Student/browser | `GET /v1/guidance/dashboard` | `GuidanceController.getDashboard` | guidance/controller | none | Implemented |
| Validate student | Guidance service | principal email | `StudentAccountAccess.findByEmail` plus service checks | identity/access | `app_users`, `student_profiles` | Implemented |
| Read completed evidence | Guidance service | focused access calls | assessment/simulation access | owning modules | completed scores/evaluated results only | Implemented |
| Read current plan | Guidance service | DAO call | `GuidanceDao.findCurrentPlan` | guidance/dao | `exploration_plans` | Implemented |
| Ask Syn | Student/browser | `POST /v1/guidance/syn/messages` | `GuidanceController.sendMessage` | guidance/controller | none | Implemented |
| Build safe reply | Guidance service | validated request | `StandardGuidancePolicy.createReply` | guidance/domain | minimized/response evidence; no provider call | Implemented |
| Record provenance | Guidance service | eligible evidence-grounded reply | `GuidanceDao.recordGuidance` | guidance/dao | `guidance_reports` | Implemented |
| Confirm plan | Student/browser | `POST /v1/guidance/plans` | `GuidanceController.savePlan` | guidance/controller | none | Implemented |
| Persist current plan | Guidance service | validated confirmed draft | `GuidanceDao.saveCurrentPlan` | guidance/dao | `exploration_plans` upsert | Implemented |

Verification: backend verify passed with 30 tests; Flyway V3 applied successfully to
the Docker PostgreSQL instance; unauthenticated gateway access returned `401`.
