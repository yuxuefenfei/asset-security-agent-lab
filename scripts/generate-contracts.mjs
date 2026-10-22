import { readFileSync, readdirSync, writeFileSync, mkdirSync } from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const root = path.resolve(import.meta.dirname, '..')
const base = path.join(root, 'lab-contract/src/main/java/lab/contract')
// 只派生浏览器使用的稳定契约，新增 DTO/枚举需在这里显式加入并重新生成。
const names = new Set([
  'AnalysisMode',
  'RiskLevel',
  'AnalysisStatus',
  'Criticality',
  'TicketStatus',
  'Severity',
  'VulnerabilityStatus',
  'ToolName',
  'ToolOrigin',
  'AssetResponse',
  'AnalysisReport',
  'AnalysisSummary',
  'TicketResponse',
  'KnowledgeResponse',
  'VulnerabilityResponse',
  'ToolTrace',
  'ToolDefinition',
  'ToolFunctionDefinition',
  'HealthResponse',
  'DashboardResponse',
  'PageResponse',
  'AnalysisRequest',
  'EditTicketRequest',
  'ApproveRequest',
  'DraftRequest',
])
// Java 的可空语义无法仅靠字段类型完整推导，在此集中维护跨语言例外。
const nullable = new Set([
  'TicketResponse.approvedBy',
  'TicketResponse.approvedAt',
  'TicketResponse.note',
  'EditTicketRequest.note',
])
function files(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((f) =>
    f.isDirectory() ? files(path.join(dir, f.name)) : [path.join(dir, f.name)],
  )
}
// 这是面向当前 record 子集的转换器，不是通用 Java 解析器；未知类型必须使构建失败。
function type(s) {
  if (/^List<(.+)>$/.test(s)) return type(s.slice(5, -1)) + '[]'
  if (['int', 'long', 'double', 'Long', 'Integer'].includes(s)) return 'number'
  if (s === 'String') return 'string'
  if (s === 'boolean') return 'boolean'
  if (s === 'JsonNode') return 'Record<string, unknown>'
  assert(names.has(s) || s === 'T', 'Unsupported Java contract type: ' + s)
  return s
}
let output =
  '// Generated from lab-contract Java records/enums by scripts/generate-contracts.mjs. Do not edit.\n'
const found = new Set()
for (const file of files(base).sort()) {
  const src = readFileSync(file, 'utf8')
  const name = path.basename(file, '.java')
  if (!names.has(name)) continue
  found.add(name)
  const record = src.match(/public record (\w+)(<T>)?\(([\s\S]*?)\)\s*\{/)
  if (record) {
    const fields = record[3]
      .replace(/@\w+(?:\([^)]*\))?\s*/g, '')
      .split(',')
      .map((f) => {
        const match = f.trim().match(/^([\w<>]+)\s+(\w+)$/)
        assert(match, 'Unsupported field in ' + name + ': ' + f)
        return (
          '  ' +
          match[2] +
          ': ' +
          type(match[1]) +
          (nullable.has(name + '.' + match[2]) ? ' | null' : '') +
          '\n'
        )
      })
      .join('')
    output += '\nexport interface ' + name + (record[2] || '') + ' {\n' + fields + '}\n'
  } else {
    const constants = src.split('public enum ' + name + ' {')[1]?.split(';')[0]
    assert(constants, 'Unsupported enum ' + name)
    const values = [...constants.matchAll(/([A-Z][A-Z_]+)(?:\("([^"]+)"\))?/g)].map((m) =>
      JSON.stringify(m[2] || m[1]),
    )
    assert(values.length)
    output += '\nexport type ' + name + ' = ' + values.join(' | ') + '\n'
  }
}
assert.equal(found.size, names.size, 'Missing contracts')
const constraints = readFileSync(path.join(base, 'api/ApiConstraints.java'), 'utf8')
output +=
  '\nexport const constraints = {\n' +
  [...constraints.matchAll(/public static final int (\w+) = (\d+);/g)]
    .map((m) => '  ' + m[1] + ': ' + m[2] + ',\n')
    .join('') +
  '} as const\n'
const protocol = readFileSync(path.join(base, 'api/LabProtocol.java'), 'utf8')
output +=
  '\nexport const protocol = {\n' +
  [...protocol.matchAll(/public static final String (\w+) = "([^"]+)";/g)]
    .map((m) => '  ' + m[1] + ': ' + JSON.stringify(m[2]) + ',\n')
    .join('') +
  '} as const\n'
const dest = path.join(root, 'frontend/src/generated/contracts.ts')
// 构建只比对不改写，避免用自动覆盖掩盖未提交的契约变更。
if (process.argv.includes('--check'))
  assert.equal(readFileSync(dest, 'utf8'), output, 'Java contracts changed; run pnpm contracts:generate')
else {
  mkdirSync(path.dirname(dest), { recursive: true })
  writeFileSync(dest, output)
}
console.log('Java / TypeScript contracts: ' + found.size + ' definitions verified')
