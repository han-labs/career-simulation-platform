# US XX Sequence to Code Trace

Source user story/use-case:  
Sequence artifact:  
Implementation commit:  

| Step/message | Actor/lifeline | Endpoint or frontend action | Method owner | Layer/module | Data/provider interaction | Status | Notes/deviation |
|---|---|---|---|---|---|---|---|
| | | | | | | Planned | |

Status values: `MATCH`, `ADAPTED`, `MISSING`, `EXTRA`, `NEEDS_DECISION`.

Verification:

- [ ] Every main-flow message maps to code.
- [ ] Alternate/exception/fallback fragments map to explicit decisions.
- [ ] Controller does not call DAO/database/provider directly.
- [ ] DAO-to-database interactions are represented when the diagram is implementation-level.
- [ ] AI adapter and deterministic fallback are both shown for guidance flows.
- [ ] Return paths and user-visible states are represented.
- [ ] Intentional differences are updated in the sequence artifact and report.
