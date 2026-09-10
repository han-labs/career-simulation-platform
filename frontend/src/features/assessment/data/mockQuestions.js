export const mockQuestions = [
  { id: 'q1', dimension: 'R', text: 'I like to work with machines and tools.' },
  { id: 'q2', dimension: 'R', text: 'I enjoy fixing things that are broken.' },
  { id: 'q3', dimension: 'I', text: 'I like to solve complex math or science problems.' },
  { id: 'q4', dimension: 'I', text: 'I enjoy analyzing data to find patterns.' },
  { id: 'q5', dimension: 'A', text: 'I like to express myself through art, music, or writing.' },
  { id: 'q6', dimension: 'A', text: 'I enjoy designing new and creative solutions.' },
  { id: 'q7', dimension: 'S', text: 'I like to teach or train others.' },
  { id: 'q8', dimension: 'S', text: 'I enjoy helping people solve their personal problems.' },
  { id: 'q9', dimension: 'E', text: 'I like to lead teams and manage projects.' },
  { id: 'q10', dimension: 'E', text: 'I enjoy convincing people to buy a product or idea.' },
  { id: 'q11', dimension: 'C', text: 'I like to keep things highly organized and structured.' },
  { id: 'q12', dimension: 'C', text: 'I enjoy following clear rules and procedures.' },
]

export const riasecDescriptions = {
  R: {
    name: 'Realistic',
    color: 'var(--sage-ink)',
    bgColor: 'var(--sage)',
    description: 'You prefer practical, hands-on, tool-driven tasks.',
    directions: ['Cloud Infrastructure', 'Hardware Engineering', 'Network Administration']
  },
  I: {
    name: 'Investigative',
    color: 'var(--tech-deep)',
    bgColor: 'var(--tech-soft)',
    description: 'You prefer analytical, intellectual, and scientific tasks.',
    directions: ['Data Science', 'Cybersecurity Research', 'Algorithm Design']
  },
  A: {
    name: 'Artistic',
    color: 'var(--accent-dark)',
    bgColor: '#fcebe6',
    description: 'You prefer creative, original, and unstructured tasks.',
    directions: ['UI/UX Design', 'Frontend Development', 'Game Design']
  },
  S: {
    name: 'Social',
    color: 'var(--violet-ink)',
    bgColor: 'var(--violet)',
    description: 'You prefer helping, teaching, and collaborating with others.',
    directions: ['Developer Advocacy', 'IT Teaching', 'User Research']
  },
  E: {
    name: 'Enterprising',
    color: 'var(--gold-ink)',
    bgColor: 'var(--gold)',
    description: 'You prefer persuading, leading, and managing.',
    directions: ['Product Management', 'Tech Sales', 'IT Leadership']
  },
  C: {
    name: 'Conventional',
    color: 'var(--blue-ink)',
    bgColor: 'var(--blue)',
    description: 'You prefer structured, orderly, and detail-oriented tasks.',
    directions: ['Quality Assurance', 'Database Administration', 'DevOps Compliance']
  }
}
