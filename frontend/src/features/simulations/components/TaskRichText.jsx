import { createElement } from 'react'

const allowed = new Set(['p', 'div', 'ul', 'ol', 'li', 'strong', 'em', 'b', 'i', 'br', 'pre', 'code', 'blockquote', 'h3', 'h4'])
const discard = new Set(['script', 'style', 'iframe', 'object', 'embed', 'img', 'svg', 'math', 'form', 'input', 'button', 'template'])

// Parse in an inert template; render only newly constructed React nodes and safe links.
// Never mount source DOM/attributes or recursively decode encoded code examples.
export default function TaskRichText({ value, className = '' }) {
  const text = typeof value === 'string' ? value : ''
  if (!/<\/?(?:p|div|br|ul|ol|li|h[1-6]|pre|code|strong|em|b|i|a|blockquote|script|style|iframe|img|svg)\b/i.test(text)) {
    return <div className={`task-rich-text task-rich-text--plain ${className}`}>{text}</div>
  }
  const template = document.createElement('template')
  template.innerHTML = text
  function nodes(parent) {
    return [...parent.childNodes].map((node, key) => {
      if (node.nodeType === 3) return node.textContent
      if (node.nodeType !== 1) return null
      const tag = node.tagName.toLowerCase()
      if (discard.has(tag)) return null
      const children = nodes(node)
      if (tag === 'a') {
        const href = node.getAttribute('href')?.trim() ?? ''
        if (/^https?:\/\//i.test(href)) {
          try {
            const url = new URL(href)
            if (['https:', 'http:'].includes(url.protocol)) return <a key={key} href={url.href} rel="noreferrer" target="_blank">{children}</a>
          } catch { /* Keep link text when the source URL is malformed. */ }
        }
        return <span key={key}>{children}</span>
      }
      const element = /^h[1-6]$/.test(tag) ? 'h3' : tag
      if (!allowed.has(element)) return <span key={key}>{children}</span>
      return createElement(element, { key }, element === 'br' ? undefined : children)
    })
  }
  return <div className={`task-rich-text ${className}`}>{nodes(template.content)}</div>
}
