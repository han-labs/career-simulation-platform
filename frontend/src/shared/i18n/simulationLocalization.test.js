import { readFileSync } from 'node:fs'
import { expect, it } from 'vitest'
import { createTranslator } from './translate.js'
import { localized } from '../api/coreApi.js'
import { simulationMessages } from './simulationMessages.js'

const vi = createTranslator('vi')

// Read public content columns only; do not turn scoring/answer fields into UI copy.
function seedRows(table) {
  const sql=readFileSync(new URL('../../../../backend/src/main/resources/db/migration/V32__seed_nine_versioned_scenarios.sql',import.meta.url),'utf8')
  const pattern=new RegExp(`INSERT INTO public\\.${table} \\(([^)]+)\\) VALUES \\(`,'g'),rows=[]
  let match
  while((match=pattern.exec(sql))) {
    const values=[];let value='',quoted=false,index=pattern.lastIndex
    for(;index<sql.length;index++) {
      const character=sql[index]
      if(character==="'") {
        if(quoted&&sql[index+1]==="'"){value+="''";index++;continue}
        quoted=!quoted
      }
      if(!quoted&&(character===','||character===')')) {
        const raw=value.trim();values.push(raw.startsWith("'")?raw.slice(1,-1).replaceAll("''","'"):raw);value=''
        if(character===')')break
      } else value+=character
    }
    rows.push(Object.fromEntries(match[1].split(',').map((column,index)=>[column.trim(),values[index]])))
  }
  return rows
}

it('covers every current imported simulation overview and public task instruction from the actual seed', () => {
  const versions=seedRows('scenario_version').filter(row=>row.status==='PUBLISHED')
  const ids=new Set(versions.map(row=>row.id)),tasks=seedRows('simulation_task').filter(row=>ids.has(row.scenario_version_id))
  expect(new Set(versions.map(row=>row.scenario_id)).size).toBe(9);expect(tasks.length).toBeGreaterThan(20)
  const missing=[]
  for(const row of versions) for(const column of ['title','target_role','career_track','summary','introduction','learning_objectives']) {
    if(row[column]&&row[column]!=='NULL'&&/[A-Za-z]/.test(row[column])&&vi(row[column])===row[column]) missing.push({title:row.title,column,text:row[column]})
  }
  for(const row of tasks) for(const column of ['title','workplace_context','instructions']) {
    if(row[column]&&row[column]!=='NULL'&&/[A-Za-z]/.test(row[column])&&vi(row[column])===row[column]) missing.push({title:row.title,column,text:row[column]})
  }
  expect(missing).toEqual([])
})

it('localizes current simulation content without modifying API objects or original language', () => {
  const row = { title:'Software Engineering', targetRole:'Software Engineer', summary:'Unknown custom description', translations:{} }
  expect(localized(row,'vi','title')).toBe('Kỹ thuật phần mềm')
  expect(localized(row,'vi','targetRole')).toBe('Kỹ sư phần mềm')
  expect(localized(row,'en','title')).toBe('Software Engineering')
  expect(localized(row,'vi','summary')).toBe('Unknown custom description')
  expect(row.title).toBe('Software Engineering')
  expect(localized({...row,translations:{vi:{title:'Tên đã được tác giả dịch'}}},'vi','title')).toBe('Tên đã được tác giả dịch')
})

it('covers reference labels and their task/program descriptions while retaining proper names and file names', () => {
  const source=readFileSync(new URL('../../features/simulations/data/referenceMaterials.js',import.meta.url),'utf8')
  const titles=[...source.matchAll(/"title": "((?:[^"\\]|\\.)*)"/g)].map(match=>JSON.parse('"'+match[1]+'"'))
  expect(titles.length).toBeGreaterThan(53)
  for(const title of titles) if(!['Cinema Finder','README'].includes(title)) expect(vi(title),title).not.toBe(title)
  for(const name of ['HPE GreenLake','HCM-UTE Career Navigator','Datacom','README','JSON','kb.search','https://example.com']) expect(vi(name)).toBe(name)
})

it('keeps HTML structure, code and links intact when translating reviewed learning objectives', () => {
  const original=Object.keys(simulationMessages).find(key=>key.includes('<ul>')&&key.includes('The basics of Java Spring Boot'))
  expect(original).toBeTruthy()
  const translated=vi(original)
  expect(translated).toContain('Kiến thức cơ bản về Java Spring Boot')
  expect(translated.match(/<[^>]+>/g)).toEqual(original.match(/<[^>]+>/g))
  expect(vi('SELECT * FROM users;')).toBe('SELECT * FROM users;')
})

it('translates Admin lifecycle/usage labels while preserving the encoded contract values', () => {
  for(const value of ['ACTIVE','PUBLISHED','DRAFT','SUCCEEDED','FAILED','ADMIN_PREVIEW','GENERAL_ALLOWED','UNKNOWN']) expect(vi(value)).not.toBe(value)
  expect(vi('Agent is preparing a response')).toBe('Trợ lý đang chuẩn bị phản hồi')
  expect(createTranslator('en')('ACTIVE')).toBe('ACTIVE')
})
