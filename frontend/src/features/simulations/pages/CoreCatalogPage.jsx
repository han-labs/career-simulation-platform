import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { useI18n } from '../../../shared/i18n/useI18n.js'
import SimulationCatalog from '../components/SimulationCatalog.jsx'

const DEFAULT_SORT = 'ID_ASC'

function supportedQuery(searchParams) {
  return {
    q: searchParams.get('q') ?? '',
    careerTrack: searchParams.get('careerTrack') ?? '',
    difficulty: searchParams.get('difficulty') ?? '',
    sort: searchParams.get('sort') || DEFAULT_SORT,
    cursor: searchParams.get('cursor') ?? '',
  }
}

export default function CoreCatalogPage() {
  const { t } = useI18n()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const query = supportedQuery(searchParams)
  const [filtersOpen, setFiltersOpen] = useState(() => ['q', 'industry', 'careerTrack', 'difficulty', 'sort'].some(key => searchParams.has(key)))
  const [state, setState] = useState({ requestPath: null, status: 'loading', page: null })
  const [retry, setRetry] = useState(0)
  const [tracks, setTracks] = useState({ status: 'loading', values: [] })
  const requestPath = useMemo(() => {
    const current = supportedQuery(new URLSearchParams(queryString))
    const params = new URLSearchParams({ paged: 'true', limit: '12', industry: 'IT', sort: current.sort })
    for (const key of ['q', 'careerTrack', 'difficulty', 'cursor']) {
      if (current[key]) params.set(key, current[key])
    }
    return `/v2/scenarios?${params}`
  }, [queryString])

  useEffect(() => {
    let active = true
    apiRequest(requestPath)
      .then(page => { if (active) setState({ requestPath, status: 'ready', page }) })
      .catch(() => { if (active) setState({ requestPath, status: 'error', page: null }) })
    return () => { active = false }
  }, [requestPath, retry])

  useEffect(() => {
    let active = true
    async function loadTracks() {
      const values = new Set()
      const cursors = new Set()
      let cursor = ''
      while (active) {
        const params = new URLSearchParams({ industry: 'IT', limit: '100' })
        if (cursor) params.set('cursor', cursor)
        const items = await apiRequest(`/v2/scenarios?${params}`)
        if (!Array.isArray(items)) throw new Error('Invalid catalogue response')
        for (const item of items) {
          if (item.metadata?.careerTrack) values.add(item.metadata.careerTrack)
        }
        if (items.length < 100) break
        cursor = items.at(-1).id
        if (!cursor || cursors.has(cursor)) throw new Error('Invalid catalogue cursor')
        cursors.add(cursor)
      }
      if (active) setTracks({ status: 'ready', values: [...values].sort((a, b) => a.localeCompare(b)) })
    }
    loadTracks().catch(() => { if (active) setTracks({ status: 'error', values: [] }) })
    return () => { active = false }
  }, [retry])

  function search(event) {
    event.preventDefault()
    const data = Object.fromEntries(new FormData(event.currentTarget))
    const next = new URLSearchParams({ industry: 'IT', sort: data.sort || DEFAULT_SORT })
    for (const key of ['q', 'careerTrack', 'difficulty']) {
      const value = data[key]?.trim()
      if (value) next.set(key, value)
    }
    setSearchParams(next)
  }

  function clearFilters() {
    setSearchParams({ industry: 'IT', sort: DEFAULT_SORT })
  }

  function moveToCursor(cursor) {
    const next = new URLSearchParams(searchParams)
    if (cursor) next.set('cursor', cursor)
    else next.delete('cursor')
    next.set('industry', 'IT')
    if (!next.get('sort')) next.set('sort', DEFAULT_SORT)
    setSearchParams(next)
  }

  const currentState = state.requestPath === requestPath ? state : { status: 'loading', page: null }
  const simulations = currentState.page?.items?.map(item => ({
    id: item.slug,
    title: item.metadata.title,
    description: item.metadata.summary,
    targetRole: item.metadata.targetRole,
    careerTrack: item.metadata.careerTrack,
    estimatedMinutes: item.metadata.estimatedMinutes,
    difficulty: item.metadata.difficulty,
    translations: item.metadata.translations,
    enterpriseName: item.enterpriseName,
    industryCode: item.industryCode,
    media: item.media,
  })) ?? []
  const hasSearchCriteria = Boolean(query.q || query.careerTrack || query.difficulty)

  return <main className="catalog-page section-shell core-flow core-catalog">
    <header className="core-catalog__header">
      <p className="eyebrow">{t('Explore by doing')}</p>
      <h1>{t('Career simulations')}</h1>
      <p>{t('Explore published IT simulations through representative workplace tasks.')}</p>
    </header>

    <button
      className="button button--secondary core-catalog__filter-toggle"
      type="button"
      aria-expanded={filtersOpen}
      aria-controls="core-catalog-filters"
      onClick={() => setFiltersOpen(value => !value)}
    >{t(filtersOpen ? 'Hide search and filters' : 'Search and filters')}</button>

    {filtersOpen && <form id="core-catalog-filters" className="core-catalog__filters" key={queryString} onSubmit={search}>
      <label className="core-catalog__search">{t('Search simulations')}
        <input name="q" maxLength={200} type="search" defaultValue={query.q} placeholder={t('Search by title, role, or summary')} />
      </label>
      <div className="core-catalog__track">
        <label htmlFor="career-track">{t('Career track')}</label>
        <select id="career-track" name="careerTrack" defaultValue={query.careerTrack} key={`${query.careerTrack}:${tracks.status}`} aria-describedby={tracks.status === 'error' ? 'career-track-error' : undefined}>
          <option value="">{t('All career tracks')}</option>
          {query.careerTrack && !tracks.values.includes(query.careerTrack) && <option value={query.careerTrack}>{t(query.careerTrack)}</option>}
          {tracks.values.map(track => <option key={track} value={track}>{t(track)}</option>)}
        </select>
        {tracks.status === 'loading' && <span role="status">{t('Loading career tracks…')}</span>}
        {tracks.status === 'error' && <span id="career-track-error" role="status">{t('Career tracks could not be loaded.')}{' '}<button type="button" onClick={() => { setTracks({ status: 'loading', values: [] }); setRetry(value => value + 1) }}>{t('Try again')}</button></span>}
      </div>
      <label>{t('Difficulty')}
        <select name="difficulty" defaultValue={query.difficulty}>
          <option value="">{t('All levels')}</option>
          {['INTRODUCTORY', 'INTERMEDIATE', 'ADVANCED'].map(value => <option key={value} value={value}>{t(value)}</option>)}
        </select>
      </label>
      <label>{t('Sort simulations')}
        <select name="sort" defaultValue={query.sort}>
          <option value="ID_ASC">{t('Publication order')}</option>
          <option value="TITLE_ASC">{t('Title')}</option>
          <option value="DURATION_ASC">{t('Duration')}</option>
          <option value="NEWEST">{t('Newest first')}</option>
        </select>
      </label>
      <div className="core-catalog__filter-actions">
        <button className="button" type="submit">{t('Search')}</button>
        <button className="button button--secondary" type="button" onClick={clearFilters}>{t('Clear filters')}</button>
      </div>
      <p className="core-catalog__industry"><span>{t('Industry')}</span> {t('Information technology')}</p>
    </form>}

    <SimulationCatalog
      variant="core"
      status={currentState.status}
      simulations={simulations}
      emptyState={hasSearchCriteria ? 'no-match' : 'empty'}
      onClear={clearFilters}
      onRetry={() => { setState({ requestPath, status: 'loading', page: null }); setRetry(value => value + 1) }}
    />

    {currentState.status === 'ready' && (currentState.page?.nextCursor || query.cursor) && <nav className="core-catalog__pagination" aria-label={t('Simulation catalogue pages')}>
      {query.cursor && <button className="button button--secondary" type="button" onClick={() => moveToCursor('')}>{t('First page')}</button>}
      {currentState.page?.nextCursor && <button className="button" type="button" onClick={() => moveToCursor(currentState.page.nextCursor)}>{t('Next page')}</button>}
    </nav>}
  </main>
}
