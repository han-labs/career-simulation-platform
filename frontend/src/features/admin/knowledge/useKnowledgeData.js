import { useCallback, useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { kbPath } from '../agentPlatform/platformApi.js'

export const JOB_POLL_MS = 5000
const running = jobs => jobs.some(job => ['PARSING', 'EMBEDDING'].includes(job.state))

// One scoped read owner. Full reloads and jobs-only polls never overlap.
export default function useKnowledgeData(id, processingJob, onError) {
  const [base, setBase] = useState(null), [versions, setVersions] = useState([]), [jobs, setJobs] = useState([])
  const alive = useRef(false), full = useRef(null), poll = useRef(null), controllers = useRef(new Set())
  useEffect(() => {
    alive.current = true
    const pending = controllers.current
    return () => { alive.current = false; pending.forEach(controller => controller.abort()) }
  }, [])
  const read = useCallback(async paths => {
    const controller = new AbortController(); controllers.current.add(controller)
    try {
      const data = await Promise.all(paths.map(path => apiRequest(path, { signal: controller.signal })))
      return alive.current && !controller.signal.aborted ? data : null
    } finally { controllers.current.delete(controller) }
  }, [])
  const refresh = useCallback(() => {
    if (full.current) return full.current
    const request = (async () => {
      if (poll.current) await poll.current.catch(() => {})
      if (!alive.current) return null
      const data = await read([kbPath(id), `${kbPath(id)}/assets`, `${kbPath(id)}/ingestions`])
      if (data) { setBase(data[0]); setVersions(data[1]); setJobs(data[2]); return data[0] }
      return null
    })()
    full.current = request
    request.finally(() => { if (full.current === request) full.current = null }).catch(() => {})
    return request
  }, [id, read])
  useEffect(() => {
    let active = true
    // Avoid the obsolete StrictMode setup's deferred bootstrap.
    Promise.resolve().then(() => { if (active) refresh().catch(error => { if (active) onError(error.message) }) })
    return () => { active = false }
  }, [refresh, onError])
  const observing = running(jobs) || Boolean(processingJob)
  useEffect(() => {
    if (!observing) return
    let active = true, timer, delay = JOB_POLL_MS
    async function observe() {
      if (!active) return
      try {
        if (full.current) await full.current
        if (!active) return
        const request = poll.current ?? read([`${kbPath(id)}/ingestions`]); poll.current = request
        let data
        try { data = await request } finally { if (poll.current === request) poll.current = null }
        if (!active || !data) return
        setJobs(data[0]); delay = JOB_POLL_MS
        const processing = data[0].find(job => job.id === processingJob)
        if (!running(data[0]) && (!processingJob || (processing && !['QUEUED', 'PARSING', 'EMBEDDING'].includes(processing.state)))) {
          await refresh(); return
        }
      } catch (error) {
        if (!active) return
        onError(error.message)
        if ([401, 403, 404].includes(error.status)) return
        delay = Math.max(Math.min(delay * 2, 60000), error.retryAfterMs ?? 0)
      }
      if (active) timer = setTimeout(observe, delay)
    }
    timer = setTimeout(observe, delay)
    return () => { active = false; clearTimeout(timer) }
  }, [id, observing, processingJob, read, refresh, onError])
  return { base, setBase, versions, jobs, setJobs, refresh }
}
