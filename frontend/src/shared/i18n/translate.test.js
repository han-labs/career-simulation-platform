import { readFileSync } from 'node:fs'
import { expect, it } from 'vitest'
import { createTranslator } from './translate.js'
import { messages } from './messages.js'
import { contentMessages } from './contentMessages.js'

const sourceFiles = import.meta.glob('../../**/*.jsx', { query: '?raw', import: 'default', eager: true })
const t = createTranslator('vi')
it('covers every current static UI lookup except language codes and product names', () => {
  const untranslated = []
  for (const [file, source] of Object.entries(sourceFiles)) {
    if (file.includes('.test.')) continue
    for (const match of source.matchAll(/\bt\(("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')\)/g)) {
      const text = match[1].startsWith('"') ? JSON.parse(match[1]) : match[1].slice(1, -1)
      if (t(text) === text && !['HCM-UTE', 'Syn', 'EN', 'VI'].includes(text)) untranslated.push(`${file}: ${text}`)
    }
  }
  expect(untranslated).toEqual([])
})
it('covers all 42 seeded assessment prompts', () => {
  const sql = readFileSync(new URL('../../../../backend/src/main/resources/db/migration/V4__update_riasec_questions_to_42.sql', import.meta.url), 'utf8')
  const prompts = [...sql.matchAll(/'(I (?:like|enjoy|prefer)[^']*)'/g)].map(m => m[1])
  expect(prompts).toHaveLength(42)
  for (const prompt of prompts) expect(t(prompt), prompt).not.toBe(prompt)
})
it('covers all text options in the twelve additional seeded simulation tasks', () => {
  const sql = readFileSync(new URL('../../../../backend/src/main/resources/db/migration/V8__seed_complete_local_demo.sql', import.meta.url), 'utf8')
  const options = [...sql.matchAll(/'(\[\{"id"[^']+\])'::jsonb/g)].flatMap(m => JSON.parse(m[1]))
  expect(options).toHaveLength(48)
  for (const option of options) expect(t(option.label), option.label).not.toBe(option.label)
})
it('preserves future unknown content, technical code, numbers and data objects', () => {
  for (const value of ['Unreviewed future content', 'SELECT * FROM users;', '400 Bad Request', 'https://example.org', 35, { id: 42 }]) expect(t(value)).toBe(value)
  expect(createTranslator('en')('Career Assessment')).toBe('Career Assessment')
  expect(t('Please answer all questions before submitting. (3 left)')).toContain('Còn 3 câu')
  expect(t('45 minutes')).toBe('45 phút')
  expect(t('Show all 12')).toBe('Xem tất cả 12')
})
it('contains valid non-empty translations without combining-mark corruption', () => {
  for (const [source, target] of Object.entries({ ...messages, ...contentMessages })) {
    expect(source.length).toBeGreaterThan(0)
    expect(typeof target).toBe('string')
    expect(target.length).toBeGreaterThan(0)
    expect(target.normalize('NFC')).toBe(target)
  }
})
it('covers public titles and instructions but does not ship evaluated explanations for the twelve additional tasks', () => {
  const sql = readFileSync(new URL('../../../../backend/src/main/resources/db/migration/V8__seed_complete_local_demo.sql', import.meta.url), 'utf8')
  const copy = [...sql.matchAll(/^\s{12}'([^'\n]+)',?\r?$/gm)].map(m => m[1]).filter(text => text.includes(' ') && !text.startsWith('['))
  expect(copy).toHaveLength(36)
  for (let index = 0; index < copy.length; index += 3) {
    expect(t(copy[index])).not.toBe(copy[index])
    expect(t(copy[index + 1])).not.toBe(copy[index + 1])
    expect(Object.hasOwn(contentMessages, copy[index + 2])).toBe(false)
    expect(t(copy[index + 2])).toBe(copy[index + 2])
  }
})
