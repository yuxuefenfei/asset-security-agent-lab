import { config } from './config'
/** API 保留 UTC 时间，统一在展示时转为北京时间，避免改写历史数据的时间语义。 */
export function formatTime(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.valueOf())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: config.timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date)
}
export function shortId(value: string) {
  return value.replace('TICKET-', '').slice(0, 8).toUpperCase()
}
export const riskLabels: Record<string, string> = {
  HIGH: '高风险',
  MEDIUM: '中风险',
  LOW: '低风险',
  CRITICAL: '严重',
  DRAFT: '待审批',
  APPROVED: '已审批',
  ANALYZED: '分析完成',
  OPEN: '未处理',
  VERIFIED: '已核实',
  RESOLVED: '已解决',
  demo: '规则分析',
  llm: '模型分析',
}
export function label(value: string) {
  return riskLabels[value] || value || '未知状态'
}
