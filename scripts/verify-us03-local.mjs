import assert from 'node:assert/strict'
import { readFile, writeFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

// Real HTTP, no mocked responses. Run only against this task's localhost demo.
const base = 'http://localhost:8080/api/v2'
const credentials = JSON.parse((await readFile(new URL('../tmp/us03-demo-credentials.json', import.meta.url), 'utf8')).replace(/^\uFEFF/, ''))
function client() {
  const cookies = new Map()
  let token
  async function request(path, { method = 'GET', body, revision, status = 200 } = {}) {
    // Respect the existing 120 requests/minute gateway policy; do not weaken it for tests.
    await delay(600)
    const response = await fetch(base + path, { method, signal: AbortSignal.timeout(15000), headers: {
      Accept: 'application/json', Cookie: [...cookies].map(([k,v]) => `${k}=${v}`).join('; '),
      ...(token ? { 'X-CSRF-TOKEN': token } : {}), ...(body === undefined ? {} : {'Content-Type':'application/json'}),
      ...(revision === undefined ? {} : {'If-Match':String(revision)}),
    }, ...(body === undefined ? {} : {body:JSON.stringify(body)}) })
    for (const cookie of response.headers.getSetCookie()) { const [pair] = cookie.split(';'); const i = pair.indexOf('='); cookies.set(pair.slice(0,i),pair.slice(i+1)) }
    assert.equal(response.status, status, `${method} ${path}: expected ${status}, got ${response.status}`)
    const payload = await response.json()
    return payload.data
  }
  return { request, async login(email) {
    token = (await request('/auth/csrf')).token
    await request('/auth/login',{method:'POST',body:{email,password:credentials.password}})
    token = (await request('/auth/csrf')).token
    await request('/me')
  } }
}
const owner = client(), other = client()
// Public simulation browsing must remain independent of assessment/authentication.
await client().request('/scenarios?paged=true')
await owner.login(credentials.email)
await other.login('us03-other@local.example.test')
const types = await owner.request('/assessment-types')
assert.deepEqual(types.map(t=>t.code).sort(), ['RIASEC','INTERESTS','ABILITIES','WORK_ACTIVITIES'].sort())
if (process.argv.includes('--recheck')) {
  const prior = JSON.parse(await readFile(new URL('../tmp/us03-api-evidence.json',import.meta.url),'utf8'))
  for (const e of prior) {
    const stored = await owner.request(`/assessment-attempts/${e.attemptId}/result`)
    assert.deepEqual(stored.resultData,e.resultData)
    assert.equal(stored.assessmentVersionId,e.version)
  }
  console.log('PASS: exact stored results for all four types survived container restart.')
  process.exit(0)
}
if (process.argv.includes('--conflict')) {
  const draft = (await owner.request('/assessment-attempts')).find(a=>a.status==='IN_PROGRESS')
  assert(draft,'Browser draft required')
  const a = await owner.request(`/assessment-attempts/${draft.id}`)
  await owner.request(`/assessment-attempts/${a.id}/responses`,{method:'PUT',revision:a.revision,body:{items:[{questionId:a.questions[0].id,answerData:{schemaVersion:1,value:1}}]}})
  console.log('PASS: newer server draft revision created for explicit browser conflict test.')
  process.exit(0)
}
const evidence = []
const itemsFor = (a,value) => a.questions.map(q=>({questionId:q.id,answerData:{schemaVersion:1,value}}))
for (const type of types) {
  const requestId = randomUUID()
  const start = () => owner.request(`/assessment-versions/${type.activeVersionId}/attempts`, {method:'POST',body:{requestId}})
  const a = await start()
  assert.equal((await start()).id,a.id)
  assert.equal(a.questions.length, {RIASEC:42,INTERESTS:12,ABILITIES:12,WORK_ACTIVITIES:9}[type.code])
  assert(a.questions.every(q=>q.required && q.translations.vi.prompt && q.responseType==='LIKERT'))
  const path = `/assessment-attempts/${a.id}`
  const valid = itemsFor(a,5)
  const saved = await owner.request(path+'/responses',{method:'PUT',revision:a.revision,body:{items:valid.slice(0,1)}})
  assert.equal((await owner.request(path)).responses.length,1)
  await owner.request(path+'/responses',{method:'PUT',revision:a.revision,body:{items:valid},status:409})
  await owner.request(path+'/submit',{method:'POST',revision:saved.revision,body:{items:valid.slice(0,1)},status:422})
  await owner.request(path+'/result',{status:404})
  await owner.request(path+'/submit',{method:'POST',revision:saved.revision,body:{items:[{...valid[0],answerData:{schemaVersion:1,value:0}}]},status:422})
  const result = await owner.request(path+'/submit',{method:'POST',revision:saved.revision,body:{items:valid}})
  assert.deepEqual(await owner.request(path+'/submit',{method:'POST',revision:saved.revision,body:{items:valid}}), result)
  assert.deepEqual(await owner.request(path+'/result'), result)
  await owner.request(path+'/submit',{method:'POST',revision:saved.revision,body:{items:itemsFor(a,1)},status:409})
  await other.request(path+'/result',{status:404})
  await other.request(path+'/responses',{method:'PUT',revision:0,body:{items:valid},status:404})
  const dimensions = result.resultData.dimensions ?? result.resultData.categories
  for (const d of dimensions) { assert.equal(d.raw,d.max); assert.equal(d.min,type.code==='RIASEC'?7:3); assert.equal(d.max,type.code==='RIASEC'?35:15) }
  evidence.push({type:type.code,attemptId:a.id,version:a.assessmentVersionId,resultData:result.resultData})
  if (type.code==='RIASEC') {
    const retake = await owner.request(`/assessment-versions/${type.activeVersionId}/attempts`,{method:'POST',body:{requestId:randomUUID()}})
    assert.notEqual(retake.id,a.id)
    await owner.request(`/assessment-attempts/${retake.id}/submit`,{method:'POST',revision:retake.revision,body:{items:itemsFor(retake,1)}})
    assert.deepEqual(await owner.request(path+'/result'),result)
  }
  console.log(`${type.code}: PASS real API draft/validation/score/persistence/idempotency/ownership`)
}
await owner.request('/assessment-versions/9223372036854775807/attempts',{method:'POST',body:{requestId:randomUUID()},status:404})
const history = await owner.request('/assessment-attempts')
assert(evidence.every(e=>history.some(row=>row.id===e.attemptId)))
const foreignHistory = await other.request('/assessment-attempts')
assert(!foreignHistory.some(row=>evidence.some(e=>e.attemptId===row.id)))
await writeFile(new URL('../tmp/us03-api-evidence.json',import.meta.url),JSON.stringify(evidence,null,2))
console.log('PASS: four types + independent retake; evidence stored without credentials.')
