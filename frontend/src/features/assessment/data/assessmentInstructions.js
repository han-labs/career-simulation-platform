const options = labels => labels.map((label, index) => ({ value: index + 1, label }))
const generic = {
  description: 'Read each activity and choose one response from 1 to 5 based on how you feel today. These are exploratory self-reports, not a measure of proficiency.',
  options: options(['1', '2', '3', '4', '5']),
}
const instructions = {
  RIASEC: { description: 'Read each activity and choose how much you like it. Answer based on how you feel today.', options: options(['Strongly Dislike', 'Dislike', 'Neutral', 'Like', 'Strongly Like']) },
  INTERESTS: { description: 'Rate how much you would enjoy each activity.', options: options(['Not interested', 'Slightly interested', 'Moderately interested', 'Very interested', 'Extremely interested']) },
  ABILITIES: { description: 'Rate how confident you feel doing each activity today.', options: options(['Not confident yet', 'A little confident', 'Somewhat confident', 'Very confident', 'Highly confident']) },
  WORK_ACTIVITIES: { description: 'Rate how much you would prefer each kind of work activity.', options: options(['Strongly prefer to avoid', 'Prefer to avoid', 'No strong preference', 'Prefer this', 'Strongly prefer this']) },
}
export function assessmentInstructions(code) { return instructions[code] ?? generic }

export function assessmentTitle(code, fallback = 'Career Assessment') {
  return code === 'RIASEC' ? 'RIASEC Assessment' : quizLabels[code]?.title ?? fallback
}
import { quizLabels } from './profileMetadata.js'

