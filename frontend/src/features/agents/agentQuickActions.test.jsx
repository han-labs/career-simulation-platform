// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import AgentQuickActions from './AgentQuickActions.jsx'
import { quickActions } from './agentQuickActions.js'
afterEach(cleanup)
it.each(['admin','student'])('%s suggestions populate only, with safe non-submit controls',kind=>{
  const choose=vi.fn(),submit=vi.fn()
  render(<I18nProvider initialLocale="en"><form onSubmit={submit}><AgentQuickActions kind={kind} onChoose={choose}/></form></I18nProvider>)
  expect(screen.getAllByRole('button',{hidden:true})).toHaveLength(4)
  fireEvent.click(screen.getByText(quickActions[kind][0]))
  expect(choose).toHaveBeenCalledExactlyOnceWith(quickActions[kind][0]);expect(submit).not.toHaveBeenCalled()
  expect(screen.getAllByRole('button',{hidden:true}).every(b=>b.type==='button')).toBe(true)
})
it('disabled suggestions cannot dispatch or change an active run',()=>{
  const choose=vi.fn();render(<I18nProvider initialLocale="vi"><AgentQuickActions kind="admin" disabled onChoose={choose}/></I18nProvider>)
  expect(screen.getByText('Gợi ý nhanh')).toBeTruthy();fireEvent.click(screen.getByText(quickActions.admin[0]));expect(choose).not.toHaveBeenCalled()
})
