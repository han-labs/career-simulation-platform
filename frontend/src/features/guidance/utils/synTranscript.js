const DEFAULT_MAX_MESSAGES = 24

function isDisplayMessage(message) {
  return message
    && typeof message.id === 'string'
    && (message.role === 'user' || message.role === 'assistant')
    && typeof message.text === 'string'
}

function boundedTranscript(messages, welcomeMessage, maxMessages = DEFAULT_MAX_MESSAGES) {
  const conversation = messages
    .filter(isDisplayMessage)
    .filter((message) => message.id !== welcomeMessage.id)
    .slice(-(maxMessages - 1))
    .map((message) => ({ ...message, isRevealing: false }))

  return [welcomeMessage, ...conversation]
}

export function loadSynTranscript(storage, storageKey, welcomeMessage) {
  try {
    const saved = JSON.parse(storage.getItem(storageKey) ?? '[]')
    return Array.isArray(saved)
      ? boundedTranscript(saved, welcomeMessage)
      : [welcomeMessage]
  } catch {
    return [welcomeMessage]
  }
}

export function persistSynTranscript(storage, storageKey, messages, welcomeMessage) {
  try {
    storage.setItem(storageKey, JSON.stringify(boundedTranscript(messages, welcomeMessage)))
  } catch {
    // The chat remains usable when browser storage is unavailable or full.
  }
}

export function clearSynTranscript(storage, storageKey) {
  try {
    storage.removeItem(storageKey)
  } catch {
    // Server-side memory clearing and the current UI reset can still continue.
  }
}
