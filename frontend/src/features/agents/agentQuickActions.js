export const studentQuickActions = [
  { label: 'Explore my results', prompt: 'Which direction could I explore first based on my assessment results?' },
  { label: 'Find a simulation', prompt: 'Which simulation could I try next?' },
  { label: 'My progress', prompt: 'How am I progressing in my simulations?' },
  { label: 'Compare careers', prompt: 'Help me compare two career directions using my evidence.' },
]
export const quickActions = {
  admin: ['Summarize four types of evidence useful for career exploration.', 'Compare Cybersecurity Analyst and Backend Developer.', 'How many items are in the incident summary checklist in the source?', 'What could I try if I am unsure between Data Analyst and QA Automation?'],
  student: studentQuickActions.map(action => action.prompt),
}
