// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { useState } from 'react'
import I18nProvider from './I18nProvider.jsx'
import LanguageSwitcher from './LanguageSwitcher.jsx'
import { LANGUAGE_KEY } from './context.js'
import HeroSection from '../../features/landing/components/HeroSection.jsx'
import AppShell from '../layout/AppShell.jsx'
import AssessmentQuestionCard from '../../features/assessment/components/AssessmentQuestionCard.jsx'
import SimulationWorkspace from '../../features/simulations/components/SimulationWorkspace.jsx'
import SimulationCatalog from '../../features/simulations/components/SimulationCatalog.jsx'
import { backendDeveloperSimulation } from '../../features/simulations/data/backendDeveloperSimulation.js'

beforeEach(() => localStorage.clear())
afterEach(() => { cleanup(); vi.restoreAllMocks() })
function StatefulQuestion() {
  const [answer, setAnswer] = useState(0)
  return <AssessmentQuestionCard question={{ id: 13, prompt: 'I like to work on cars or machinery.' }} index={0} answer={answer}
    setAnswer={(_id, value) => setAnswer(value)} scaleOptions={[{ value: 5, label: 'Strongly Like' }]} />
}
it('switches content and metadata, stores the choice, and preserves an in-progress answer', () => {
  render(<I18nProvider><LanguageSwitcher /><StatefulQuestion /></I18nProvider>)
  fireEvent.click(screen.getByRole('radio', { name: 'Strongly Like' }))
  fireEvent.change(screen.getByRole('combobox', { name: 'Website language' }), { target: { value: 'vi' } })
  expect(screen.getByRole('radio', { name: 'Rất thích' }).checked).toBe(true)
  expect(screen.getByText('Tôi thích làm việc với ô tô hoặc máy móc.')).toBeTruthy()
  expect(document.documentElement.lang).toBe('vi')
  expect(document.title).toBe('Nền tảng mô phỏng nghề nghiệp | HCM-UTE')
  expect(localStorage.getItem(LANGUAGE_KEY)).toBe('vi')
  cleanup()
  render(<I18nProvider><LanguageSwitcher /></I18nProvider>)
  expect(screen.getByRole('combobox', { name: 'Ngôn ngữ website' }).value).toBe('vi')
})
it('rejects invalid stored language and still switches when storage is unavailable', () => {
  localStorage.setItem(LANGUAGE_KEY, 'invalid')
  render(<I18nProvider><LanguageSwitcher /></I18nProvider>)
  expect(screen.getByRole('combobox', { name: 'Website language' }).value).toBe('en')
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked') })
  fireEvent.change(screen.getByRole('combobox', { name: 'Website language' }), { target: { value: 'vi' } })
  expect(screen.getByRole('combobox', { name: 'Ngôn ngữ website' }).value).toBe('vi')
})
it('loads safely when preference reads fail', () => {
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked') })
  render(<I18nProvider><LanguageSwitcher /></I18nProvider>)
  expect(screen.getByRole('combobox', { name: 'Website language' }).value).toBe('en')
})
it('uses the requested Vietnamese school identity and translated hero without changing navigation', () => {
  render(<I18nProvider initialLocale="vi"><MemoryRouter><AppShell /><HeroSection status="intro" /></MemoryRouter></I18nProvider>)
  expect(screen.getByText('Đại học Công nghệ Kỹ thuật TP. HCM · Nền tảng mô phỏng nghề nghiệp')).toBeTruthy()
  expect(screen.getByText('Khám phá nghề qua trải nghiệm')).toBeTruthy()
  expect(screen.getAllByRole('link', { name: /Đánh giá hướng nghiệp/ }).every(link => link.getAttribute('href') === '/assessment')).toBe(true)
  expect(screen.getByRole('combobox', { name: 'Ngôn ngữ website' })).toBeTruthy()
})
it('localizes task labels without changing selected option values or technical snippets', () => {
  const onAnswerChange = vi.fn()
  render(<I18nProvider initialLocale="vi"><SimulationWorkspace task={backendDeveloperSimulation.tasks[1]} taskIndex={1} totalTasks={3}
    answers={{ 'sql-query': 'a' }} onAnswerChange={onAnswerChange} /></I18nProvider>)
  expect(screen.getByText('Truy vấn nào trả về mọi dòng trong bảng users?')).toBeTruthy()
  expect(screen.getByRole('radio', { name: 'SELECT * FROM users;' }).checked).toBe(true)
  fireEvent.click(screen.getByRole('radio', { name: 'GET ALL users;' }))
  expect(onAnswerChange).toHaveBeenCalledWith('sql-query', 'b')
})
it('shows loading, empty and failure states in Vietnamese with a usable retry', () => {
  const onRetry = vi.fn()
  const view = render(<I18nProvider initialLocale="vi"><MemoryRouter><SimulationCatalog status="loading" simulations={[]} onRetry={onRetry} /></MemoryRouter></I18nProvider>)
  expect(screen.getByLabelText('Đang tải mô phỏng')).toBeTruthy()
  view.rerender(<I18nProvider initialLocale="vi"><MemoryRouter><SimulationCatalog status="ready" simulations={[]} onRetry={onRetry} /></MemoryRouter></I18nProvider>)
  expect(screen.getByText('Chưa có mô phỏng')).toBeTruthy()
  view.rerender(<I18nProvider initialLocale="vi"><MemoryRouter><SimulationCatalog status="error" simulations={[]} onRetry={onRetry} /></MemoryRouter></I18nProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
  expect(onRetry).toHaveBeenCalledTimes(1)
})
it('synchronizes a valid preference change from another tab', () => {
  render(<I18nProvider><LanguageSwitcher /></I18nProvider>)
  fireEvent(window, new StorageEvent('storage', { key: LANGUAGE_KEY, newValue: 'vi' }))
  expect(screen.getByRole('combobox', { name: 'Ngôn ngữ website' }).value).toBe('vi')
})
