import { request, queryString, type Query } from '../../shared/api/client'
import type { KnowledgeResponse, PageResponse } from '../../generated/contracts'
export const knowledgeApi = {
  page: (query: Query, signal?: AbortSignal) =>
    request<PageResponse<KnowledgeResponse>>('/api/knowledge/page?' + queryString(query), { signal }),
  get: (id: string, signal?: AbortSignal) =>
    request<KnowledgeResponse>('/api/knowledge/' + encodeURIComponent(id), { signal }),
}
