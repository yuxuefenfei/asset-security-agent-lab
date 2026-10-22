import { request, queryString, type Query } from '../../shared/api/client'
import type { AssetResponse, PageResponse, VulnerabilityResponse } from '../../generated/contracts'
export const assetsApi = {
  page: (query: Query, signal?: AbortSignal) =>
    request<PageResponse<AssetResponse>>('/api/assets/page?' + queryString(query), { signal }),
  get: (ip: string, signal?: AbortSignal) =>
    request<AssetResponse>('/api/assets?' + queryString({ ip }), { signal }),
  vulnerabilities: (ip: string, signal?: AbortSignal) =>
    request<VulnerabilityResponse[]>('/api/vulnerabilities?' + queryString({ ip }), { signal }),
}
