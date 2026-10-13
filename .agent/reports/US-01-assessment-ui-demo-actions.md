# US-01 Assessment UI Consistency and Quick Demo Actions — Evidence

Date: 2026-10-14  
Scope: Career Assessments selection page and existing assessment forms  
Status: Implemented; visual browser check blocked by local browser-to-dev-server connectivity

## Changes

- The four available assessments remain in one shared two-column grid on desktop/tablet and one column on narrow screens. The selection content now fills the existing 1160px `section-shell`; cards stretch to equal row heights, and normal/demo actions align at the card bottom.
- Each card has its existing primary assessment action plus a secondary `Quick demo` action. RIASEC retains its three existing profile-fill controls.
- Interests, Abilities, and Work Activities provide named, category-specific answer presets in their question forms. The card-level quick demo uses the same preset data, opens the right quiz, starts the existing attempt flow, fills that profile's responses, and leaves review/edit/submit to the user.
- Demo state uses a separate browser-storage key per assessment/profile. It is not submitted automatically; completed scores/results still come from the backend. Normal routes and storage keys remain separate.

## Mapping and boundaries

- Layout and card links: `AssessmentIntro.jsx`, `assessmentCatalog.js`, assessment CSS in `index.css`.
- Preset generation and controls: `demoAnswers.js`, `demoProfiles.js`, `AssessmentDemoControls.jsx`, `AssessmentForm.jsx`.
- Demo route initialization and state isolation: `CareerQuizPage.jsx`, `RiasecAssessmentFlow.jsx`, `useCareerQuiz.js`, `useAssessment.js`.
- Backend APIs, scoring, and database: unchanged.

## Verification

| Command/check | Result |
|---|---|
| `npm run lint` | Passed after final edits |
| `npm run test:run -- --maxWorkers=1` | Passed: 9 files, 33 tests |
| `npm run build` | Passed: Vite production build |
| `git diff --check` | Passed |
| Browser responsive inspection | Attempted with local Vite at `127.0.0.1:5173`; browser reported `ERR_CONNECTION_TIMED_OUT`, so no visual claim is made |

The selection component test asserts four active cards and four distinct Quick demo links. Preset tests assert complete, category-specific answers for Interests, Abilities, and Work Activities and preserve the existing RIASEC answer pattern.

## Limitations

- An interactive browser check of card dimensions and the full demo route could not be completed because the browser process could not reach the local Vite server. The browser window on the existing port 8080 was an older running build and was not used as evidence for these edits.
- Demos create/use the existing backend attempt when launched; the form does not auto-submit. This mirrors the existing RIASEC flow while keeping demo answers separate from normal browser state.
