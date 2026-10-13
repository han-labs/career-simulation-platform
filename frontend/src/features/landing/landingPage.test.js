import React from 'react'
import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import HeroSection from './components/HeroSection.jsx'
import FeaturesSection from './components/FeaturesSection.jsx'
import JourneySection from './components/JourneySection.jsx'
import TracksSection from './components/TracksSection.jsx'
import CatalogSection from './components/CatalogSection.jsx'
import CtaSection from './components/CtaSection.jsx'
import AppShell from '../../shared/layout/AppShell.jsx'

describe('Landing Page components & institutional identity', () => {
  it('HeroSection renders English headline, HCM-UTE badge, and call to action', () => {
    const html = renderToStaticMarkup(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(HeroSection, { status: 'intro', result: null }),
      ),
    )

    expect(html).toContain('Practical Career Exploration')
    expect(html).toContain('Experience IT careers before choosing your')
    expect(html).toContain('Career Assessment')
    expect(html).not.toContain('Scientific Holland framework')
    expect(html).not.toContain('5 real-world IT tracks')
    expect(html).not.toContain('Explainable AI guidance')
    expect(html).not.toContain('hero__highlights')
    expect(html).not.toContain('high school')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
  })

  it('HeroSection reflects completed assessment state when result is present', () => {
    const mockResult = { topDimension: 'INVESTIGATIVE' }
    const html = renderToStaticMarkup(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(HeroSection, { status: 'completed', result: mockResult }),
      ),
    )

    expect(html).toContain('View Career Assessment Results')
    expect(html).toContain('Investigative')
  })

  it('FeaturesSection renders the 3 core pillars of the platform', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(FeaturesSection)),
    )

    expect(html).toContain('Career Assessment')
    expect(html).toContain('Holland RIASEC questionnaire')
    expect(html).toContain('Authentic IT Job Simulations')
    expect(html).toContain('Syn Assistant &amp; Exploration Dashboard')
    expect(html).toContain('Objective &amp; Transparent')
    expect(html).toContain('AI does not change them.')
  })

  it('JourneySection renders the 3-step feedback loop with correct route links', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(JourneySection)),
    )

    expect(html).toContain('01')
    expect(html).toContain('02')
    expect(html).toContain('03')
    expect(html).toContain('Identify Interests &amp; Personality')
    expect(html).toContain('Experience Authentic IT Tasks')
    expect(html).toContain('Analyze Evidence &amp; Plan Next Steps')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
    expect(html).toContain('href="/dashboard"')
  })

  it('TracksSection renders all 5 approved IT career tracks', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(TracksSection)),
    )

    expect(html).toContain('Backend &amp; API Engineering')
    expect(html).toContain('Frontend &amp; Web Development')
    expect(html).toContain('Data Analysis &amp; Engineering')
    expect(html).toContain('Software Testing &amp; QA/QC')
    expect(html).toContain('Information Security &amp; Cybersecurity')
    expect(html).toContain('/simulations/backend-api-triage')
  })

  it('CatalogSection renders simulations with track tags and difficulty levels', () => {
    const mockSimulations = [
      {
        id: 'sim-1',
        slug: 'backend-api-triage',
        title: 'Backend API Triage',
        careerTrack: 'BACKEND_DEVELOPMENT',
        summary: 'Inspect API response.',
        difficulty: 'INTRODUCTORY',
        estimatedMinutes: 35,
      },
    ]

    const html = renderToStaticMarkup(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(CatalogSection, {
          simulations: mockSimulations,
          catalogStatus: 'ready',
        }),
      ),
    )

    expect(html).toContain('Backend API Triage')
    expect(html).toContain('Backend Development')
    expect(html).toContain('Level: Introductory')
    expect(html).toContain('35 min')
    expect(html).toContain('href="/simulations/backend-api-triage"')
  })

  it('CtaSection guides students to next steps with accessible buttons', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(CtaSection)),
    )

    expect(html).toContain('Start exploring your future')
    expect(html).toContain('Career Assessment')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
  })

  it('AppShell renders top institutional bar, HCM-UTE branding, and educational footer', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(AppShell)),
    )

    expect(html).toContain('Ho Chi Minh City University of Technology and Engineering (HCM-UTE) · Career Simulation Platform')
    expect(html).toContain('Empowering students to explore tech careers and shape their future')
    expect(html).not.toContain('Technology and Education')
    expect(html).not.toContain('CareerSim')
    expect(html).not.toContain('RIASEC Assessment')
    expect(html).toContain('/images/hcmute-logo.png')
    expect(html).toContain('Career Simulation Platform')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
    expect(html).toContain('href="/dashboard"')
    expect(html).toContain('Developed for OOSE / Scientific Research Project')
  })

  it('uses Career Assessment consistently on all landing assessment links', () => {
    const sections = [HeroSection, FeaturesSection, JourneySection, TracksSection, CtaSection]
    for (const Section of sections) {
      const html = renderToStaticMarkup(
        React.createElement(MemoryRouter, null, React.createElement(Section, { status: 'intro' })),
      )
      const assessmentLinks = [...html.matchAll(/<a\b[^>]*href="\/assessment"[^>]*>([\s\S]*?)<\/a>/g)]
      expect(assessmentLinks.length).toBeGreaterThan(0)
      for (const [, content] of assessmentLinks) {
        expect(content.replace(/<[^>]*>/g, '').trim()).toBe('Career Assessment')
      }
    }
  })
})
