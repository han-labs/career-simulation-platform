import React from 'react'
import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import BrandMark from './BrandMark.jsx'

describe('BrandMark institutional branding component', () => {
  it('renders official HCM-UTE logo image path and accessible alt text', () => {
    const html = renderToStaticMarkup(React.createElement(BrandMark))

    expect(html).toContain('src="/images/hcmute-logo.png"')
    expect(html).toContain('alt="Ho Chi Minh City University of Technology and Engineering - HCM-UTE Logo"')
    expect(html).toContain('brand-mark__logo')
  })

  it('renders institutional title and product name in English', () => {
    const html = renderToStaticMarkup(React.createElement(BrandMark))

    expect(html).toContain('HCM-UTE')
    expect(html).toContain('Career Simulation Platform')
    expect(html).not.toContain('CareerSim')
    expect(html).not.toContain('brand-mark__sub')
  })

  it('applies compact styling class when compact prop is provided', () => {
    const defaultHtml = renderToStaticMarkup(React.createElement(BrandMark))
    const compactHtml = renderToStaticMarkup(React.createElement(BrandMark, { compact: true }))

    expect(defaultHtml).not.toContain('brand-mark--compact')
    expect(compactHtml).toContain('brand-mark--compact')
  })
})
