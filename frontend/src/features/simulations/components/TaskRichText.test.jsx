// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import TaskRichText from './TaskRichText.jsx'

describe('TaskRichText', () => {
  it('renders supported rich text instead of showing raw HTML tags', () => {
    render(<TaskRichText value={'<p>Review the <strong>brief</strong>.</p><ul><li>Write tests</li></ul>'} />)

    expect(screen.getByText('Review the', { exact: false }).tagName).toBe('P')
    expect(screen.getByText('brief').tagName).toBe('STRONG')
    expect(screen.getByText('Write tests').tagName).toBe('LI')
  })

  it('drops executable markup, unsafe links, and source attributes', () => {
    const { container } = render(<TaskRichText value={'<p onclick="alert(1)">Safe</p><script>alert(1)</script><a href="javascript:alert(1)">Unsafe link</a><a href="https://example.com/path">Safe link</a>'} />)

    expect(container.querySelector('script')).toBeNull()
    expect(container.querySelector('[onclick]')).toBeNull()
    expect(screen.getByText('Unsafe link').closest('a')).toBeNull()
    expect(screen.getByRole('link', { name: 'Safe link' }).getAttribute('href')).toBe('https://example.com/path')
    expect(screen.getByRole('link', { name: 'Safe link' }).getAttribute('rel')).toBe('noreferrer')
  })
})
