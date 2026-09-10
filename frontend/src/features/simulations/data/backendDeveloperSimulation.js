// Phase 1 mock content for the Backend Developer simulation; no API is called here.
export const backendDeveloperSimulation = {
  id: 'backend-developer',
  title: 'Backend Developer',
  description: 'Investigate API behavior, database queries, and common backend failures.',
  duration: '15 minutes',
  difficulty: 'Beginner',
  taskCount: 3,
  tasks: [
    {
      id: 'api-response',
      title: 'Interpreting an API response',
      prompt: 'An API returns HTTP 404. What does this usually mean?',
      options: [
        { id: 'a', label: 'The server successfully created a resource.' },
        { id: 'b', label: 'The requested resource was not found.' },
        { id: 'c', label: 'The user is authenticated but forbidden.' },
        { id: 'd', label: 'The server timed out.' },
      ],
      correctAnswer: 'b',
      explanation: 'HTTP 404 indicates that the requested resource could not be found.',
    },
    {
      id: 'sql-query',
      title: 'Selecting an appropriate SQL query',
      prompt: 'Which query returns every row from the users table?',
      options: [
        { id: 'a', label: 'SELECT * FROM users;' },
        { id: 'b', label: 'GET ALL users;' },
        { id: 'c', label: 'FIND users;' },
        { id: 'd', label: 'RETURN users;' },
      ],
      correctAnswer: 'a',
      explanation: 'SELECT * FROM users is the SQL statement that retrieves all rows and columns.',
    },
    {
      id: 'backend-error',
      title: 'Identifying a backend error',
      prompt: 'A request fails because required input is missing. Which HTTP status fits best?',
      options: [
        { id: 'a', label: '200 OK' },
        { id: 'b', label: '400 Bad Request' },
        { id: 'c', label: '404 Not Found' },
        { id: 'd', label: '500 Internal Server Error' },
      ],
      correctAnswer: 'b',
      explanation: 'HTTP 400 communicates that the submitted request is invalid or incomplete.',
    },
  ],
}
