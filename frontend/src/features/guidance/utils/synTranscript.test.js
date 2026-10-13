import { describe, expect, it } from 'vitest'
import {
  clearSynTranscript,
  loadSynTranscript,
  persistSynTranscript,
} from './synTranscript.js'

const welcome = { id: 'welcome', role: 'assistant', text: 'Hello' }

function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
}

describe('Syn transcript persistence', () => {
  it('restores a bounded conversation after the widget remounts', () => {
    const storage = memoryStorage()
    const messages = [
      welcome,
      ...Array.from({ length: 30 }, (_, index) => ({
        id: `message-${index}`,
        role: index % 2 ? 'assistant' : 'user',
        text: `Message ${index}`,
      })),
    ]

    persistSynTranscript(storage, 'transcript', messages, welcome)
    const restored = loadSynTranscript(storage, 'transcript', welcome)

    expect(restored).toHaveLength(24)
    expect(restored[0]).toEqual(welcome)
    expect(restored.at(-1).text).toBe('Message 29')
  })

  it('only returns to the welcome message after an explicit clear', () => {
    const storage = memoryStorage()
    persistSynTranscript(storage, 'transcript', [
      welcome,
      { id: 'student-1', role: 'user', text: 'Compare frontend and backend' },
    ], welcome)

    clearSynTranscript(storage, 'transcript')

    expect(loadSynTranscript(storage, 'transcript', welcome)).toEqual([welcome])
  })

  it('falls back safely when browser storage contains malformed data', () => {
    const storage = memoryStorage()
    storage.setItem('transcript', '{not-json')

    expect(loadSynTranscript(storage, 'transcript', welcome)).toEqual([welcome])
  })
})
