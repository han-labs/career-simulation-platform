import { useLayoutEffect } from 'react'
import { useLocation } from 'react-router-dom'

export default function NavigationScroll() {
  const { key, pathname, search, hash } = useLocation()
  useLayoutEffect(() => {
    const previous = window.history.scrollRestoration
    window.history.scrollRestoration = 'manual'
    return () => { window.history.scrollRestoration = previous }
  }, [])

  useLayoutEffect(() => {
    if (hash) {
      let id
      try { id = decodeURIComponent(hash.slice(1)) } catch { return }
      // Keep explicit fragment navigation, including native full-page links.
      document.getElementById(id)?.scrollIntoView({ behavior: 'instant', block: 'start' })
      return
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    document.getElementById('main-content')?.focus({ preventScroll: true })
  }, [key, pathname, search, hash])
  return null
}
