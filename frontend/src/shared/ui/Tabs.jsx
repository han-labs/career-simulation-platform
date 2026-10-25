import { useId, useRef, useState } from 'react'

// Manual activation: arrow keys move focus without triggering a fetch or reset.
export default function Tabs({ label, value, onChange, items }) {
  const id = useId(), refs = useRef({}), [focused, setFocused] = useState(value)
  function keyboard(event, key) {
    const position = items.findIndex(item => item.value === key)
    const keys = { ArrowRight: (position + 1) % items.length, ArrowLeft: (position - 1 + items.length) % items.length, Home: 0, End: items.length - 1 }
    if (event.key in keys) {
      event.preventDefault(); const next = items[keys[event.key]].value; setFocused(next); refs.current[next]?.focus()
    } else if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); onChange(key) }
  }
  return <div className="cs-tabs">
    <div className="cs-tablist" role="tablist" aria-label={label}>{items.map(item => <button key={item.value} ref={node => { refs.current[item.value] = node }} type="button" role="tab" id={id + '-tab-' + item.value} aria-controls={id + '-panel-' + item.value} aria-selected={value === item.value} tabIndex={(items.some(i => i.value === focused) ? focused : value) === item.value ? 0 : -1} onFocus={() => setFocused(item.value)} onKeyDown={event => keyboard(event, item.value)} onClick={() => { setFocused(item.value); onChange(item.value) }}>{item.label}</button>)}</div>
    {items.map(item => <div key={item.value} role="tabpanel" id={id + '-panel-' + item.value} aria-labelledby={id + '-tab-' + item.value} hidden={value !== item.value} className="cs-tabpanel">{item.content}</div>)}
  </div>
}
