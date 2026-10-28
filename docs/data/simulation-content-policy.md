# Simulation Content and External Source Policy

## Decision and purpose

CareerSim is a non-commercial course project. The project lead permits collection of publicly accessible factual metadata about IT-related simulations from The Forage for catalog research and source discovery, provided the source permits automated access at collection time.

This decision does not authorize copying complete simulation content. CareerSim must publish original or separately licensed instructions, tasks, work files, answer keys, evaluation rules, images, and other learning materials.

## Permitted metadata

When current source terms, `robots.txt`, and technical controls permit collection, the research dataset may contain:

- canonical public source URL;
- simulation title;
- publisher or employer name for attribution;
- public category or career track;
- public difficulty and estimated duration;
- public task count and skill labels;
- retrieval timestamp and transformation notes.

Short source descriptions may be used only during internal human review. Do not publish them verbatim unless a compatible license or written permission is recorded.

## Excluded content and behavior

The collector must not:

- sign in, use session cookies, or crawl authenticated simulation/task pages;
- bypass `robots.txt`, CAPTCHA, rate limits, access controls, or anti-automation measures;
- download or republish employer task descriptions, work files, model answers, certificates, screenshots, logos, brand assets, datasets, or videos;
- collect personal data, student reviews, profiles, submitted work, tracking identifiers, or contact details;
- imply that The Forage or any listed employer sponsors, partners with, or endorses CareerSim;
- automatically publish collected records to the student catalog.

## Collection gate

Before each run, the reviewer must record the current source Terms of Use and `robots.txt` decision. The collector uses a descriptive user agent, cached responses, sequential requests, and a default ceiling of one request per second. Any denial, ambiguity, markup mismatch, or missing provenance stops the source.

The collector output is a draft research dataset. A human reviews allowed fields, accuracy, attribution, duplicate handling, and removal/update procedure before any factual metadata is imported. If permission cannot be verified, the team uses manual research links or synthetic/original seed data.

## Initial source record

```text
Source URL: https://www.theforage.com/
Publisher: Forage / EAB
Retrieved date: 2026-09-10
License/terms: No reusable-content license recorded in this repository; re-check the official Terms of Use and robots.txt before collection.
Permitted project use: Public factual IT-simulation metadata for internal non-commercial academic research when current source controls permit automated access.
Fields/content used: URL, title, publisher attribution, category/track, difficulty, duration, task count, and skill labels.
Transformations: Normalize categories and durations; deduplicate by canonical URL; write all CareerSim task content independently.
Reviewer: Project lead or assigned content reviewer.
Removal/update procedure: Disable the source record, remove derived public metadata when required, and replace it with original/synthetic catalog data.
```

## Relationship to product behavior

Externally collected metadata never changes RIASEC scores, simulation scores, answer keys, or AI guidance rules. Public catalog publication remains an explicit CareerSim content decision and must satisfy BR-05, BR-06, and BR-16.

## Curated task reference links — 2026-10-28

The Team Lead supplied `forage_clean_dataset_all_resources.xlsx` and explicitly
requested suitable links in the existing Reference material / Resources section.
The read-only source is sheet `task_resources`; workbook SHA-256:
`966b7393dffa2c553ae239dec0ff98b78ba7aabe4cb1fb214be1f9cd9e2e13df`.

`frontend/src/features/simulations/data/referenceMaterials.js` contains 53 link
placements across 20 tasks in eight simulations: HPE Software Engineering (12),
Datacom Service Desk (2), Introduction to Cloud (13), HPE Presales (6), Digital Sales
(5), Datacom Automation (6), Cyber Security Operations (1), Software Development (8).
The workbook supplies no matching resource for Partnering with AI or several other
tasks; no unrelated replacement is invented.

Selection excludes all Example Answer steps, Model Answer links (including the
previous-task answer), the sandbox-email screenshot, and the supplied URLs under
`devwithus.com/download-upload-files-with-spring-boot/` and
`w3schools.in/restful-web-services/intro`, which returned HTTP404 during checking.
Normal task templates, input datasets and customer briefs remain outbound links.
No third-party files, instructions, answers, images or workbook are copied into the
repository. No Forage asset was downloaded or authenticated source page crawled.
Public-page HEAD checks are availability hints; some returned403 or could not be
verified, and external downloads may still require access at the source.

Matching uses the existing public metadata's original title/targetRole/careerTrack
and the original task title. It does not rely on private provenance, route aliases,
database IDs, or the current UI language. These supplemental UI references also
work with pinned older enrollments without changing sealed versions, hashes,
submissions, APIs or migrations. Server resources retain their release policy;
their URLs take precedence, including when explicitly gated. Only the Resources
presentation merges the supplemental links; model-answer release is unchanged.
