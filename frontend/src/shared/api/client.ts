import { protocol } from '../../generated/contracts'
import { config } from '../config'
import { token } from '../session'
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status = 0,
    readonly code = '',
    readonly traceId = '',
  ) {
    super(message)
  }
}
export function errorText(error: unknown) {
  if (error instanceof ApiError)
    return error.message + (error.traceId ? '（请求编号：' + error.traceId + '）' : '')
  return error instanceof Error ? error.message : '请求未完成，请稍后重试'
}
export type Query = Record<string, string | number | boolean | undefined | null>
export function queryString(query: Query) {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
  })
  return params.toString()
}
/** 统一处理实验口令、请求编号、响应包装、错误与取消；页面只消费业务 data。 */
export async function request<T>(
  path: string,
  options: { method?: string; data?: unknown; signal?: AbortSignal } = {},
): Promise<T> {
  const timeout = AbortSignal.timeout(config.timeout)
  // 页面取消和等待超时任一触发都结束本次 fetch，不需要在各业务 API 重复实现。
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout
  try {
    const response = await fetch(config.baseUrl + path, {
      method: options.method || 'GET',
      signal,
      headers: {
        [protocol.TOKEN_HEADER]: token.value,
        [protocol.TRACE_HEADER]: crypto.randomUUID(),
        ...(options.data === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: options.data === undefined ? undefined : JSON.stringify(options.data),
    })
    const result = await response.json().catch(() => {
      throw new ApiError('服务返回了无效响应，请重试', response.status)
    })
    if (!response.ok || !result.success)
      throw new ApiError(result.error || '请求失败，请重试', response.status, result.code, result.traceId)
    return result.data as T
  } catch (error) {
    // 主动离开不作为错误提示；等待超时无法判断服务端是否已保存，提示用户先查报告。
    if (options.signal?.aborted) throw new DOMException('请求已取消', 'AbortError')
    if (timeout.aborted) throw new ApiError('请求等待超时，请查看报告列表确认结果后再重试')
    if (error instanceof TypeError) throw new ApiError('无法连接实验服务，请检查服务是否启动')
    throw error
  }
}
