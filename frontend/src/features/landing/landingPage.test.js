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

    expect(html).toContain('Career Simulation Platform')
    expect(html).toContain('Explore real careers.')
    expect(html).toContain('Build your brighter future.')
    expect(html).toContain('Explore career assessments')
    expect(html).not.toContain('Scientific Holland framework')
    expect(html).not.toContain('5 real-world IT tracks')
    expect(html).not.toContain('Explainable AI guidance')
    expect(html).not.toContain('hero__highlights')
    expect(html).toContain('Experience real-world tasks, discover your strengths')
    expect(html).toContain('Explore career assessments')
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

    expect(html).toContain('Holland RIASEC Interest Assessment')
    expect(html).toContain('calculated deterministically')
    expect(html).toContain('Authentic IT Job Simulations')
    expect(html).toContain('Exploration Dashboard')
    expect(html).toContain('Objective &amp; Transparent')
    expect(html).toContain('AI does not change them.')
  })

  it('JourneySection renders four steps while preserving the existing route links', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(JourneySection)),
    )

    expect([...html.matchAll(/<li>/g)]).toHaveLength(4)
    expect(html).toContain('Get to know yourself')
    expect(html).toContain('Explore the career simulation')
    expect(html).toContain('Analyze your results &amp; Plan for education/training')
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

    expect(html).toContain('We believe in a future where you can discover a fitting career path')
    expect(html).toContain('Start your journey today')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
  })

  it('AppShell renders compact HCM-UTE branding, real account entry and educational footer', () => {
    const html = renderToStaticMarkup(
      React.createElement(MemoryRouter, null, React.createElement(AppShell)),
    )

    expect(html).toContain('HCMC University of Technology and Engineering')
    expect(html).toContain('Sign In / Sign Up')
    expect(html).not.toContain('institutional-bar')
    expect(html).not.toContain('Technology and Education')
    expect(html).not.toContain('CareerSim')
    expect(html).not.toContain('RIASEC Assessment')
    expect(html).toContain('/images/hcmute/logo.jpg')
    expect(html).toContain('Career Simulation Platform')
    expect(html).toContain('href="/assessment"')
    expect(html).toContain('href="/simulations"')
    expect(html).toContain('href="/dashboard"')
    expect(html).toContain('Developed for OOSE / Scientific Research Project')
  })

  it('uses clear assessment actions on all landing assessment links', () => {
    const sections = [HeroSection, FeaturesSection, JourneySection, TracksSection, CtaSection]
    for (const Section of sections) {
      const html = renderToStaticMarkup(
        React.createElement(MemoryRouter, null, React.createElement(Section, { status: 'intro' })),
      )
      const assessmentLinks = [...html.matchAll(/<a\b[^>]*href="\/assessment"[^>]*>([\s\S]*?)<\/a>/g)]
      expect(assessmentLinks.length).toBeGreaterThan(0)
      expect(assessmentLinks.every(([, content]) => content.replace(/<[^>]*>/g, '').trim().length > 0)).toBe(true)
    }
  })
})
