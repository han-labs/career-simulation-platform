// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import { command } from '../../shared/api/coreApi.js'
import AuthenticationForm from './AuthenticationForm.jsx'

vi.mock('../../shared/api/coreApi.js', () => ({ command: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); localStorage.clear() })
afterEach(cleanup)
function page(locale = 'en', google = false, onAuthenticated = vi.fn()) {
  render(<I18nProvider initialLocale={locale}><MemoryRouter initialEntries={['/account']}><Routes>
    <Route path="/account" element={<AuthenticationForm google={google} onAuthenticated={onAuthenticated} />} />
    <Route path="/" element={<h1>Home</h1>} />
  </Routes></MemoryRouter></I18nProvider>)
  return onAuthenticated
}
function register() { fireEvent.click(screen.getByRole('button', { name: 'Register', exact: true })) }
function fill() {
  fireEvent.change(screen.getByLabelText('Display name *'), { target: { value: 'New Student' } })
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'new@example.test' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'synthetic-password-2026' } })
}
function submit() { fireEvent.submit(screen.getByRole('button', { name: 'Create account', exact: true }).closest('form')) }

it('associates errors with required and invalid fields, focuses the first, and sends no request', () => {
  page(); register(); submit()
  expect(screen.getByRole('alert').textContent).toContain('highlighted fields')
  const name = screen.getByLabelText('Display name *')
  expect(name.getAttribute('aria-invalid')).toBe('true')
  expect(document.getElementById(name.getAttribute('aria-describedby')).textContent).toBe('This field is required.')
  expect(document.activeElement).toBe(name)
  fill()
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'invalid' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'short' } })
  submit()
  expect(screen.getByText('Enter a valid email address.')).toBeTruthy()
  expect(command).not.toHaveBeenCalled()
})

it('registers a Student without optional enterprise fields and returns to a cleared sign-in form', async () => {
  command.mockResolvedValue({ id: '50' })
  page(); register(); fill(); submit()
  expect(await screen.findByRole('status')).toHaveProperty('textContent', 'Account created. Sign in to continue.')
  expect(command).toHaveBeenCalledWith('/v2/auth/register', { displayName: 'New Student', email: 'new@example.test', password: 'synthetic-password-2026', role: 'STUDENT' })
  expect(screen.getByLabelText('Password *').value).toBe('')
  expect(screen.queryByLabelText('Display name *')).toBeNull()
  expect(localStorage.length).toBe(0)
})

it('preserves entries and provides duplicate email guidance, then permits retry', async () => {
  command.mockRejectedValueOnce(Object.assign(new Error('Conflict'), { status: 409 }))
  page(); register(); fill(); submit()
  expect((await screen.findByRole('alert')).textContent).toContain('An account with this email already exists. Please sign in.')
  expect(screen.getByLabelText('Email *').value).toBe('new@example.test')
  expect(document.activeElement).toBe(screen.getByLabelText('Email *'))
  expect(screen.getByLabelText('Display name *').value).toBe('New Student')
  expect(screen.getByLabelText('Password *').value).toBe('synthetic-password-2026')
  expect(screen.getByLabelText('Email *').getAttribute('aria-invalid')).toBe('true')
  expect(screen.getByRole('button', { name: 'Create account', exact: true }).matches(':disabled')).toBe(false)
  command.mockResolvedValueOnce({ id: '51' })
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'another@example.test' } })
  submit()
  expect(await screen.findByRole('status')).toBeTruthy()
  expect(command).toHaveBeenCalledTimes(2)
})

it('preserves login entries after failure and permits a successful retry', async () => {
  command.mockRejectedValueOnce(new Error('Invalid or expired sign-in.')).mockResolvedValueOnce({ id: '50', role: 'STUDENT' })
  const authenticated = page()
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'new@example.test' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'synthetic-password-only' } })
  fireEvent.submit(screen.getByLabelText('Email *').closest('form'))
  expect((await screen.findByRole('alert')).textContent).toBe('Invalid or expired sign-in.')
  expect(screen.getByLabelText('Email *').value).toBe('new@example.test')
  expect(screen.getByLabelText('Password *').value).toBe('synthetic-password-only')
  expect(authenticated).not.toHaveBeenCalled()
  fireEvent.submit(screen.getByLabelText('Email *').closest('form'))
  await waitFor(() => expect(authenticated).toHaveBeenCalledWith({ id: '50', role: 'STUDENT' }))
  expect(command).toHaveBeenCalledTimes(2)
})

it('maps service failure without claiming success and blocks concurrent submissions and cancellation', async () => {
  let reject
  command.mockImplementation(() => new Promise((_, fail) => { reject = fail }))
  page(); register(); fill(); submit()
  fireEvent.submit(screen.getByLabelText('Email *').closest('form'))
  expect(command).toHaveBeenCalledTimes(1)
  expect(screen.getByRole('button', { name: 'Cancel registration' }).disabled).toBe(true)
  reject(Object.assign(new Error('internal details'), { status: 503 }))
  expect((await screen.findByRole('alert')).textContent).toBe('Registration is temporarily unavailable. Please try again later.')
  expect(screen.queryByRole('status')).toBeNull()
  expect(screen.getByLabelText('Password *').value).toBe('synthetic-password-2026')
})

it('cancels to the landing page without creating or storing an account', () => {
  page(); register(); fill()
  fireEvent.click(screen.getByRole('button', { name: 'Cancel registration' }))
  expect(screen.getByRole('heading', { name: 'Home' })).toBeTruthy()
  expect(command).not.toHaveBeenCalled()
  expect(localStorage.length).toBe(0)
})

it('clears secrets on mode changes, permits password visibility, and authenticates', async () => {
  const authenticated = page()
  register(); fill()
  fireEvent.click(screen.getByRole('button', { name: 'Show password' }))
  expect(screen.getByLabelText('Password *').type).toBe('text')
  fireEvent.click(screen.getByRole('button', { name: 'Sign in', exact: true }))
  expect(screen.getByLabelText('Password *').value).toBe('')
  expect(screen.getByLabelText('Password *').type).toBe('password')
  fireEvent.change(screen.getByLabelText('Email *'), { target: { value: 'new@example.test' } })
  fireEvent.change(screen.getByLabelText('Password *'), { target: { value: 'synthetic-password-2026' } })
  command.mockResolvedValue({ id: '50', role: 'STUDENT' })
  fireEvent.submit(screen.getByLabelText('Email *').closest('form'))
  await waitFor(() => expect(authenticated).toHaveBeenCalledWith({ id: '50', role: 'STUDENT' }))
})

it('retains Enterprise registration with conditional required organization', async () => {
  page(); register(); fill()
  fireEvent.change(screen.getByLabelText('Role *'), { target: { value: 'ENTERPRISE' } })
  submit()
  expect(screen.getByLabelText('Enterprise name *').getAttribute('aria-invalid')).toBe('true')
  expect(command).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Enterprise name *'), { target: { value: 'Synthetic Enterprise' } })
  command.mockResolvedValue({ id: '60' }); submit()
  await waitFor(() => expect(command).toHaveBeenCalledWith('/v2/auth/register', expect.objectContaining({ role: 'ENTERPRISE', enterpriseName: 'Synthetic Enterprise' })))
})

it('shows localized validation and the configured Google authorization link in both modes', () => {
  page('vi', true)
  expect(screen.getByRole('link', { name: 'Tiếp tục với Google' }).getAttribute('href')).toBe('/api/oauth2/authorization/google')
  fireEvent.click(screen.getByRole('button', { name: 'Đăng ký', exact: true }))
  expect(screen.getByRole('link', { name: 'Tiếp tục với Google' }).getAttribute('href')).toBe('/api/oauth2/authorization/google')
  fireEvent.submit(screen.getByRole('button', { name: 'Tạo tài khoản', exact: true }).closest('form'))
  expect(screen.getByRole('alert').textContent).toBe('Vui lòng kiểm tra các trường được đánh dấu.')
})

it('keeps a disabled Google option and an explanation visible when unconfigured', () => {
  page()
  expect(screen.getByRole('button', { name: 'Continue with Google' }).disabled).toBe(true)
  expect(screen.getByText(/Google sign-in is not configured/)).toBeTruthy()
  register()
  expect(screen.getByRole('button', { name: 'Continue with Google' }).disabled).toBe(true)
  const primary = screen.getByRole('button', { name: 'Create account', exact: true })
  const google = screen.getByRole('button', { name: 'Continue with Google' })
  const cancel = screen.getByRole('button', { name: 'Cancel registration' })
  expect(primary.compareDocumentPosition(google) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  expect(google.compareDocumentPosition(cancel) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
})

it('starts the backend redirect without submitting local fields and locks repeated actions until retry', () => {
  page('en', true); register()
  const link = screen.getByRole('link', { name: 'Continue with Google' })
  // Observe the real href action without letting jsdom attempt a network navigation.
  const start = new MouseEvent('click', { bubbles: true, cancelable: true })
  link.addEventListener('click', event => event.preventDefault(), { once: true })
  fireEvent(link, start)
  expect(link.getAttribute('href')).toBe('/api/oauth2/authorization/google')
  expect(screen.getByRole('link', { name: 'Connecting to Google…' }).getAttribute('aria-disabled')).toBe('true')
  expect(screen.getByRole('button', { name: 'Create account', exact: true }).matches(':disabled')).toBe(true)
  expect(screen.getByRole('button', { name: 'Cancel registration' }).disabled).toBe(true)
  const repeat = new MouseEvent('click', { bubbles: true, cancelable: true })
  fireEvent(link, repeat)
  expect(repeat.defaultPrevented).toBe(true)
  expect(command).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(screen.getByRole('link', { name: 'Continue with Google' }).getAttribute('aria-disabled')).toBeNull()
})
