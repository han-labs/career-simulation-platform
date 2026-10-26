// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import I18nProvider from '../../../shared/i18n/I18nProvider.jsx'
import { apiRequest } from '../../../shared/api/httpClient.js'
import SourcePassages from '../agentPlatform/SourcePassages.jsx'
vi.mock('../../../shared/api/httpClient.js', () => ({ apiRequest:vi.fn() }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
const passage = { referenceId:'K1',knowledgeBaseId:'kb',chunkId:'7',versionId:'v1',title:'Hướng dẫn 🔎',section:'Mục 1',text:'Nguyên văn 🔎 tiếng Việt.',sourceLocator:{ lineStart:1,lineEnd:2 },embeddingProfile:'synthetic:768' }
function page() { return render(<I18nProvider initialLocale="en"><SourcePassages presentation="citations" passages={[passage,{...passage,referenceId:'K2',chunkId:'8',title:'Uncited source',text:'Do not display uncited evidence here'}]} citations={[{referenceId:'K1',quote:passage.text}]} onUnavailable={vi.fn()} /></I18nProvider>) }
it('attaches only current-answer cited sources and resolves exact Unicode through the CareerSim API', async () => {
  apiRequest.mockResolvedValue(passage); page()
  expect(screen.queryByText('Uncited source')).toBeNull(); expect(screen.queryByText('Do not display uncited evidence here')).toBeNull()
  expect(screen.getByText('Supporting excerpt').closest('details').open).toBe(false)
  fireEvent.click(screen.getByRole('button',{name:'Hướng dẫn 🔎 · K1'}))
  const dialog = await screen.findByRole('dialog',{name:'Source evidence'})
  await waitFor(() => expect(dialog.textContent).toContain(passage.text))
  expect(apiRequest).toHaveBeenCalledWith('/v2/admin/knowledge-bases/kb/sources/7')
  expect(dialog.querySelector('mark')).toBeNull(); expect(dialog.textContent).toContain('lineStart')
})
it.each(['revoked','changed-version','changed-text','changed-chunk','changed-scope'])('hides cached supporting evidence after %s revalidation', async kind => {
  apiRequest.mockResolvedValueOnce(passage)
  if (kind === 'revoked') apiRequest.mockRejectedValueOnce(new Error('Revoked'))
  else apiRequest.mockResolvedValueOnce({...passage,...({'changed-version':{versionId:'v2'},'changed-text':{text:'Changed content'},'changed-chunk':{chunkId:'99'},'changed-scope':{knowledgeBaseId:'other-kb'}}[kind])})
  page(); fireEvent.click(screen.getByRole('button',{name:'Hướng dẫn 🔎 · K1'}))
  await waitFor(() => expect(screen.getByRole('dialog').textContent).toContain(passage.text))
  await act(async () => fireEvent.focus(window))
  await screen.findByRole('button',{name:'Source unavailable'})
  expect(screen.getByRole('button',{name:'Source unavailable'}).disabled).toBe(true)
  expect(screen.queryByText(passage.text)).toBeNull(); expect(screen.queryByText('Supporting excerpt')).toBeNull()
  expect(screen.getByRole('alert').textContent).toContain('Source unavailable')
})
