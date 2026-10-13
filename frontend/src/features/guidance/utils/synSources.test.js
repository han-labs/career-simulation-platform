import { describe, expect, it } from 'vitest'
import { safeSourceUrl } from './synSources.js'
describe('Syn source navigation', () => {
  it('accepts HTTPS references and backend source IDs', () => {
    expect(safeSourceUrl('https://example.org/docs')).toBe('https://example.org/docs')
    expect(safeSourceUrl('/api/v1/guidance/knowledge/chunks/12')).toBe('/api/v1/guidance/knowledge/chunks/12')
  })
  it('rejects executable, credential-bearing and arbitrary relative URLs', () => {
    for (const value of ['javascript:alert(1)', '//evil.test', 'data:text/html,x', '/admin', 'https://user:pass@example.org', null]) {
      expect(safeSourceUrl(value)).toBeNull()
    }
  })
})

