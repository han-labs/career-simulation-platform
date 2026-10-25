import { messages } from './messages.js'
import { contentMessages } from './contentMessages.js'
import { coreMessages } from './coreMessages.js'
import { agentMessages } from './agentMessages.js'
import { identityMessages } from './identityMessages.js'
import { assessmentMessages } from './assessmentMessages.js'

const dictionary = { ...messages, ...contentMessages, ...coreMessages, ...agentMessages, ...assessmentMessages, ...identityMessages }
// Templates are presentation-only. Numbers, IDs and API payloads never change.
const templates = [
  [/^(\d+) minutes$/, (m) => `${m[1]} phút`],
  [/^(\d+) points$/, (m) => `${m[1]} điểm`],
  [/^Show all (\d+)$/, (m) => `Xem tất cả ${m[1]}`],
  [/^Please answer all questions before submitting\. \((\d+) left\)$/, (m) => `Vui lòng trả lời đủ câu hỏi trước khi gửi. (Còn ${m[1]} câu)`],
  [/^Request failed with status (\d+)\.?$/, (m) => `Yêu cầu thất bại với mã ${m[1]}`],
  [/^Review my (.+) result$/, (m, t) => `Xem lại kết quả ${t(m[1])} của tôi`],
  [/^Complete one focused task involving (.+)\.$/, (m, t) => `Hoàn thành một nhiệm vụ về ${t(m[1])}.`],
  [/^(.+) result details$/, (m, t) => `Chi tiết kết quả ${t(m[1])}`],
  [/^Retrieved (.+)$/, (m) => `Truy xuất lúc ${m[1]}`],
  [/^ · Version (.+)$/, (m) => ` · Phiên bản ${m[1]}`],
]

export function createTranslator(locale) {
  function translate(value) {
    if (locale !== 'vi' || typeof value !== 'string') return value
    const key = value.replace(/\s+/g, ' ').trim()
    if (Object.hasOwn(dictionary, key)) return dictionary[key]
    for (const [pattern, render] of templates) {
      const match = value.match(pattern)
      if (match) return render(match, translate)
    }
    return value
  }
  return translate
}
