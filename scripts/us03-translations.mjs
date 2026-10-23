import { writeFileSync } from 'node:fs'
import { contentMessages } from '../frontend/src/shared/i18n/contentMessages.js'
import { assessmentMessages } from '../frontend/src/shared/i18n/assessmentMessages.js'
// Reuse the project's existing translations; no new questionnaire copy is invented.
writeFileSync(process.argv[2], JSON.stringify({ ...contentMessages, ...assessmentMessages }), 'utf8')
