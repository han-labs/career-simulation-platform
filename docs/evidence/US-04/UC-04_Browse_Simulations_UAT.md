# UC-04 — Browse Simulations (US-04): UAT Checklist

**Status note:** Results reflect the previously reported UC-04 manual UAT. They do not represent a new regression run after the UC-05 integration.

### TC-04-01 — Simulation catalogue
Open `/simulations` as a guest and inspect the list of published simulations.

**Expected:** Published simulations load from the API without exposing restricted content.

**Result:** PASS

### TC-04-02 — Simulation cards and navigation
Open a simulation card, use browser Back, and navigate using the keyboard.

**Expected:** Cards open the correct simulation detail; Back and keyboard navigation work.

**Result:** PASS

### TC-04-03 — Search and empty state
Search for an existing simulation and a query with no matches.

**Expected:** Matching results are correct and a useful no-results message appears.

**Result:** PASS

### TC-04-04 — Filters, sorting, and URL state
Apply Industry (IT), Career Track, and Difficulty filters; change sorting and refresh the page.

**Expected:** Filtering and sorting are correct, and supported choices persist in the URL.

**Result:** PASS

### TC-04-05 — Cursor pagination
Navigate forward and backward when the catalogue contains more than one page of results.

**Expected:** Pagination does not duplicate or omit simulations.

**Result:** PARTIAL — Only nine simulations were available on one page; browser pagination could not be exercised. Isolated automated pagination testing passed.

### TC-04-06 — Simulation details
Open a published simulation and inspect its role, difficulty, tasks, and resources.

**Expected:** Details belong to the selected simulation and show the necessary public information.

**Result:** PASS

### TC-04-07 — Overview, video, and tasks
Use section anchors, task accordions, and available videos; inspect formatted content.

**Expected:** Navigation and expandable content work; public content renders correctly.

**Result:** PASS

### TC-04-08 — Restricted content protection
Inspect the public simulation detail and public API response before submitting a task.

**Expected:** Locked model answers and evaluation rules are not disclosed.

**Result:** PASS

### TC-04-09 — Guest Start
While signed out, select **Start free simulation**.

**Expected:** The user is sent to sign-in and the selected simulation is preserved.

**Result:** PASS

### TC-04-10 — Login return
Complete ordinary sign-in after starting as a guest.

**Expected:** The user returns to the intended simulation.

**Result:** PASS

### TC-04-11 — Start and Resume
Start an eligible simulation, leave it, and select Resume.

**Expected:** The existing enrollment is resumed rather than duplicated.

**Result:** PASS

### TC-04-12 — Google OAuth return
Start a simulation as a guest and authenticate with Google OAuth.

**Expected:** Authentication returns the user to the selected simulation.

**Result:** BLOCKED — Google OAuth was disabled in the local UAT environment.

### TC-04-13 — Responsive design and keyboard access
Inspect desktop and 320 px mobile layouts; navigate primary controls with Tab and Enter.

**Expected:** Content does not overflow horizontally and primary actions remain keyboard-accessible.

**Result:** PASS

### TC-04-14 — English and Vietnamese localization
Switch languages on catalogue and simulation detail screens.

**Expected:** Main labels and messages use the selected language.

**Result:** PASS

---

**Previously reported UC-04 outcome:** 14 PASS, 1 PARTIAL, 1 BLOCKED, 0 FAIL across the original 16 test cases (including the separately tracked Career Track dropdown and other subcases). This condensed file groups related checks into 14 cases, so its per-case counts differ from that original detailed run.
