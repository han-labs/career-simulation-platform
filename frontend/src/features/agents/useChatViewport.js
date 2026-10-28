import { useLayoutEffect, useRef, useState } from 'react'

// Only the transcript moves. A submitted turn is positioned once; generation never follows it.
export default function useChatViewport({ ready, visible = true, scope, content, stream, pending, status, layout }) {
  const viewport = useRef(null), tail = useRef(null), position = useRef(0), positioned = useRef(false), applied = useRef(null), released = useRef(null)
  const [anchor, setAnchor] = useState(null), [following, setFollowing] = useState(true)
  const currentScope = useRef(scope)
  useLayoutEffect(() => {
    if (currentScope.current !== scope) {
      currentScope.current = scope; position.current = 0; positioned.current = false; applied.current = null; released.current = anchor
    }
    const node = viewport.current, spacer = tail.current
    if (!node || !spacer || !ready || !visible || node.closest('[hidden]')) return
    const turn = anchor && released.current !== anchor ? [...node.querySelectorAll('[data-chat-anchor]')].find(item => item.dataset.chatAnchor === anchor) : null
    if (turn) {
      const top = turn.getBoundingClientRect().top - node.getBoundingClientRect().top + node.scrollTop
      const end = spacer.getBoundingClientRect().top - node.getBoundingClientRect().top + node.scrollTop
      spacer.style.height = `${Math.max(0, node.clientHeight - 20 - (end - top))}px`
      if (applied.current !== anchor) {
        node.scrollTop = Math.max(0, top - 20); position.current = node.scrollTop; positioned.current = true; applied.current = anchor
      }
    } else spacer.style.height = '0px'
    node.scrollTop = positioned.current ? position.current : node.scrollHeight
    positioned.current = true; position.current = node.scrollTop
    const nearBottom = node.scrollHeight - node.scrollTop - node.clientHeight < 32
    if (nearBottom !== following) setFollowing(nearBottom)
  }, [scope, ready, visible, anchor, following, content, stream, pending, status, layout])
  function markSent(key) { applied.current = null; released.current = null; setAnchor(key); setFollowing(false) }
  function observeScroll() { const node = viewport.current; position.current = node.scrollTop; setFollowing(node.scrollHeight - node.scrollTop - node.clientHeight < 32) }
  function jump() { const node = viewport.current; released.current = anchor; if (node) { if (tail.current) tail.current.style.height = '0px'; node.scrollTop = node.scrollHeight; position.current = node.scrollTop }; setFollowing(true) }
  return { viewport, tail, following, markSent, observeScroll, jump }
}
