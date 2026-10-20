import { useEffect, useState } from 'react'
import { apiRequest } from '../../../shared/api/httpClient.js'
import { executionPath } from './platformApi.js'

// Poll accepted identity only. Reload/reconnect never creates another paid request.
export default function useExecution(id) {
  const [state, setState] = useState(null), [revision, setRevision] = useState(0)
  useEffect(() => {
    if (!id) return undefined
    let active = true, timer
    async function load() {
      try {
        const execution = await apiRequest(executionPath(id))
        if (!active) return
        setState({ id, execution })
        if (['ACCEPTED', 'RUNNING'].includes(execution.status)) timer = setTimeout(load, 1500)
      } catch (error) { if (active) setState({ id, error: error.message }) }
    }
    load()
    return () => { active = false; clearTimeout(timer) }
  }, [id, revision])
  return { execution: state?.id === id ? state.execution : null, error: state?.id === id ? state.error : null, reload: () => setRevision(r => r + 1) }
}
