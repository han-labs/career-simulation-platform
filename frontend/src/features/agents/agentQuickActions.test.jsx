// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import I18nProvider from '../../shared/i18n/I18nProvider.jsx'
import AgentQuickActions from './AgentQuickActions.jsx'
import { createTranslator } from '../../shared/i18n/translate.js'
import { quickActions } from './agentQuickActions.js'
afterEach(cleanup)
it.each(['admin','student'])('%s suggestions populate only, with safe non-submit controls',kind=>{
  const choose=vi.fn(),submit=vi.fn()
  render(<I18nProvider initialLocale="en"><form onSubmit={submit}><AgentQuickActions kind={kind} onChoose={choose}/></form></I18nProvider>)
  expect(screen.getAllByRole('button',{hidden:true})).toHaveLength(4)
  fireEvent.click(screen.getByRole('button',{name:quickActions[kind][0]}))
  expect(choose).toHaveBeenCalledExactlyOnceWith(quickActions[kind][0]);expect(submit).not.toHaveBeenCalled()
  expect(screen.getAllByRole('button',{hidden:true}).every(b=>b.type==='button')).toBe(true)
})
it('disabled suggestions cannot dispatch or change an active run',()=>{
  const choose=vi.fn();render(<I18nProvider initialLocale="vi"><AgentQuickActions kind="admin" disabled onChoose={choose}/></I18nProvider>)
  expect(screen.getByText('Gợi ý nhanh')).toBeTruthy();fireEvent.click(screen.getByText(createTranslator('vi')(quickActions.admin[0])));expect(choose).not.toHaveBeenCalled()
})

it.each(['en', 'vi'])('renders stable translated Student chips and dedicated actions in %s', locale => {
 const choose=vi.fn(),report=vi.fn(),plan=vi.fn(),t=createTranslator(locale)
 render(<I18nProvider initialLocale={locale}><AgentQuickActions kind="student" onChoose={choose} onReport={report} onPlan={plan}/></I18nProvider>)
 fireEvent.click(screen.getByRole('button',{name:t('Request insight report')}));fireEvent.click(screen.getByRole('button',{name:t('Suggest draft plan')}))
 expect(report).toHaveBeenCalledOnce();expect(plan).toHaveBeenCalledOnce();expect(choose).not.toHaveBeenCalled()
 fireEvent.click(screen.getByRole('button',{name:t(quickActions.student[0])}));expect(choose).toHaveBeenCalledWith(t(quickActions.student[0]))
 expect(quickActions.student.every(key=>/^[A-Za-z]/.test(key))).toBe(true)
})

it.each(['en','vi'])('maps every visible Student suggestion to its matching editable prompt in %s',locale=>{
 const choose=vi.fn(),t=createTranslator(locale)
 render(<I18nProvider initialLocale={locale}><AgentQuickActions kind="student" onChoose={choose}/></I18nProvider>)
 for(const [label,prompt] of [
  ['Explore my results','Which direction could I explore first based on my assessment results?'],
  ['Find a simulation','Which simulation could I try next?'],
  ['My progress','How am I progressing in my simulations?'],
  ['Compare careers','Help me compare two career directions using my evidence.'],
 ]){fireEvent.click(screen.getByText(t(label)));expect(choose).toHaveBeenLastCalledWith(t(prompt))}
})
