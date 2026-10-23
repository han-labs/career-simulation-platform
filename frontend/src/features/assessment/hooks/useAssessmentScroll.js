import { useLayoutEffect, useRef } from 'react'
import { useLocation } from 'react-router-dom'

export function useAssessmentScroll(view) {
  const heading = useRef(null)
  const { hash } = useLocation()
  useLayoutEffect(() => {
    if (hash) {
      let id
      try { id = decodeURIComponent(hash.slice(1)) } catch { return }
      document.getElementById(id)?.scrollIntoView({ behavior: 'instant', block: 'start' })
      return
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    heading.current?.focus({ preventScroll: true })
  }, [view, hash])
  return heading
}
