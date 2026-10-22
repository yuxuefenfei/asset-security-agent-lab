import { request } from '../../shared/api/client'
import type { DashboardResponse, HealthResponse, ToolDefinition } from '../../generated/contracts'
export const workbenchApi = {
  dashboard: (signal?: AbortSignal) => request<DashboardResponse>('/api/dashboard', { signal }),
  health: (signal?: AbortSignal) => request<HealthResponse>('/api/health', { signal }),
  tools: (signal?: AbortSignal) => request<ToolDefinition[]>('/api/tools', { signal }),
}
