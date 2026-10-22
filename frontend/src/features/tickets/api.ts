import { request, queryString, type Query } from '../../shared/api/client'
import type {
  TicketResponse,
  PageResponse,
  EditTicketRequest,
  ApproveRequest,
} from '../../generated/contracts'
export const ticketsApi = {
  page: (query: Query, signal?: AbortSignal) =>
    request<PageResponse<TicketResponse>>('/api/tickets/page?' + queryString(query), { signal }),
  get: (id: string, signal?: AbortSignal) =>
    request<TicketResponse>('/api/tickets/' + encodeURIComponent(id), { signal }),
  draft: (analysisId: string, signal?: AbortSignal) =>
    request<TicketResponse>('/api/tickets/draft', { method: 'POST', data: { analysisId }, signal }),
  edit: (id: string, data: EditTicketRequest, signal?: AbortSignal) =>
    request<TicketResponse>('/api/tickets/' + encodeURIComponent(id), { method: 'PUT', data, signal }),
  approve: (data: ApproveRequest, signal?: AbortSignal) =>
    request<TicketResponse>('/api/tickets/approve', { method: 'POST', data, signal }),
}
