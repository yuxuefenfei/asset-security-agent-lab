// VITE_* 是公开的构建期配置；模型地址和密钥应配置在 Java 端。
const timeout = Number(import.meta.env.VITE_API_TIMEOUT_MS || 150000)
if (!Number.isFinite(timeout) || timeout <= 0) throw new Error('VITE_API_TIMEOUT_MS 必须为正数')
export const config = {
  baseUrl: (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, ''),
  timeout,
  timezone: 'Asia/Shanghai',
  defaultToken: 'learn-agent-local',
} as const
