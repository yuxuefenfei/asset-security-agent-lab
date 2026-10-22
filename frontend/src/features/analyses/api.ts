import { request, queryString, type Query } from '../../shared/api/client'
import type {
  AnalysisReport,
  AnalysisSummary,
  AnalysisRequest,
  PageResponse,
} from '../../generated/contracts'
export const analysesApi = {
  page: (query: Query, signal?: AbortSignal) =>
    request<PageResponse<AnalysisSummary>>('/api/analyses/page?' + queryString(query), { signal }),
  get: (id: string, signal?: AbortSignal) =>
    request<AnalysisReport>('/api/analyses/' + encodeURIComponent(id), { signal }),
  run: (data: AnalysisRequest, signal?: AbortSignal) =>
    request<AnalysisReport>('/api/analyze', { method: 'POST', data, signal }),
}
