// @vitest-environment jsdom
import { afterEach, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { quizLabels } from '../data/profileMetadata.js'
import ProfileAssessmentResult from './ProfileAssessmentResult.jsx'

afterEach(cleanup)
function result(code, topCount = 1) {
  const codes = Object.keys(quizLabels[code].categories)
  return { assessmentTypeCode: code, resultData: {
    categories: codes.map((code, i) => ({ code, raw: i < topCount ? 28 : 12, min: 7, max: 35 })),
    topCategories: codes.slice(0, topCount),
  } }
}
function show(value, locale = 'en') {
  return render(<I18nProvider initialLocale={locale}><MemoryRouter><ProfileAssessmentResult result={value} onRestart={() => {}} /></MemoryRouter></I18nProvider>)
}
it.each(Object.keys(quizLabels))('projects %s saved values, dynamic ranges, metadata and equal highest signals', code => {
  const saved = result(code, 2)
  const before = JSON.stringify(saved)
  show(saved)
  const bars = screen.getAllByRole('progressbar')
  expect(bars).toHaveLength(saved.resultData.categories.length)
  bars.forEach((bar, i) => {
    expect(bar.getAttribute('aria-valuemin')).toBe('7')
    expect(bar.getAttribute('aria-valuemax')).toBe('35')
    expect(bar.getAttribute('aria-valuenow')).toBe(String(saved.resultData.categories[i].raw))
    expect(bar.getAttribute('aria-label')).toBe(quizLabels[code].categories[saved.resultData.categories[i].code][0])
  })
  expect(screen.getByRole('heading', { name: 'Shared highest signals' })).toBeTruthy()
  expect(screen.getAllByText('Highest signal')).toHaveLength(2)
  expect(screen.getByText(quizLabels[code].categories[saved.resultData.categories[0].code][1])).toBeTruthy()
  expect(document.body.textContent).not.toMatch(/WORK_ACTIVITIES|PROBLEM_SOLVING|profile-sum/)
  expect(JSON.stringify(saved)).toBe(before)
})
it.each(Object.keys(quizLabels))('localizes %s title, categories, explanations, limitations and actions', code => {
  show(result(code), 'vi')
  expect(screen.getByRole('heading', { name: 'Tổng quan tự khám phá của bạn' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Bắt đầu lượt làm mới' })).toBeTruthy()
  expect(screen.getAllByText('Khoảng điểm: 7–35')).toHaveLength(Object.keys(quizLabels[code].categories).length)
  expect(document.body.textContent).not.toContain(quizLabels[code].statement)
  Object.values(quizLabels[code].categories).forEach(([label, description]) => {
    expect(document.body.textContent).not.toContain(label)
    expect(document.body.textContent).not.toContain(description)
  })
})
it('clearly presents abilities as confidence, not certified skill', () => {
  show(result('ABILITIES'))
  expect(screen.getByText('These ratings describe your current confidence, not verified professional skills.')).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Highest signal' })).toBeTruthy()
})
it.each(['range', 'unknown', 'missing', 'top'])('does not fabricate an interpretation for invalid %s data', issue => {
  const saved = result('INTERESTS')
  if (issue === 'range') saved.resultData.categories[0].raw = 36
  if (issue === 'unknown') saved.resultData.categories[0].code = 'PRIVATE_CODE'
  if (issue === 'missing') saved.resultData.categories.pop()
  if (issue === 'top') saved.resultData.topCategories = ['PRIVATE_CODE']
  show(saved)
  expect(screen.getByRole('heading', { name: 'Your results are unavailable' })).toBeTruthy()
  expect(screen.queryByRole('progressbar')).toBeNull()
  expect(document.body.textContent).not.toContain('PRIVATE_CODE')
})
