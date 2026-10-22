// Generated from lab-contract Java records/enums by scripts/generate-contracts.mjs. Do not edit.

export type AnalysisMode = "demo" | "llm"

export interface AnalysisReport {
  id: string
  time: string
  mode: AnalysisMode
  asset: AssetResponse
  vulnerabilities: VulnerabilityResponse[]
  riskScore: number
  riskLevel: RiskLevel
  knowledgeEvidence: KnowledgeResponse[]
  summary: string
  warning: string
  toolTrace: ToolTrace[]
  status: AnalysisStatus
}

export interface AnalysisRequest {
  ip: string
  question: string
  mode: AnalysisMode
}

export type AnalysisStatus = "ANALYZED"

export interface AnalysisSummary {
  id: string
  time: string
  mode: AnalysisMode
  assetIp: string
  riskScore: number
  riskLevel: RiskLevel
  status: AnalysisStatus
}

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH"

export interface HealthResponse {
  status: string
  timestamp: string
  demo: boolean
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface AssetResponse {
  id: string
  ip: string
  tenantId: string
  hostname: string
  os: string
  business: string
  criticality: Criticality
  internetExposed: boolean
  owner: string
  ports: number[]
}

export type Criticality = "LOW" | "MEDIUM" | "HIGH"

export interface DashboardResponse {
  assetCount: number
  exposedAssetCount: number
  reportCount: number
  highRiskReportCount: number
  pendingTicketCount: number
  approvedTicketCount: number
  knowledgeCount: number
}

export interface KnowledgeResponse {
  id: string
  title: string
  keywords: string
  body: string
  source: string
}

export interface ApproveRequest {
  ticketId: string
  approvalCode: string
  version: number
}

export interface DraftRequest {
  analysisId: string
}

export interface EditTicketRequest {
  title: string
  remediation: string
  note: string | null
  version: number
}

export interface TicketResponse {
  id: string
  analysisId: string
  assetIp: string
  title: string
  status: TicketStatus
  riskLevel: RiskLevel
  createdAt: string
  approvedBy: string | null
  approvedAt: string | null
  note: string | null
  remediation: string
  version: number
}

export type TicketStatus = "DRAFT" | "APPROVED"

export interface ToolDefinition {
  type: string
  function: ToolFunctionDefinition
}

export interface ToolFunctionDefinition {
  name: ToolName
  description: string
  parameters: Record<string, unknown>
}

export type ToolName = "get_asset" | "get_vulnerabilities" | "search_knowledge"

export type ToolOrigin = "java-orchestrator" | "llm-request"

export interface ToolTrace {
  step: number
  origin: ToolOrigin
  tool: ToolName
  arguments: Record<string, unknown>
  resultCount: number
}

export type Severity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"

export interface VulnerabilityResponse {
  id: string
  advisory: string
  name: string
  severity: Severity
  cvss: number
  component: string
  status: VulnerabilityStatus
  scanner: string
  lastSeen: string
  verified: boolean
}

export type VulnerabilityStatus = "OPEN" | "VERIFIED" | "RESOLVED"

export const constraints = {
  MAX_QUESTION_LENGTH: 1000,
  MAX_QUERY_LENGTH: 200,
  MAX_TICKET_TITLE_LENGTH: 200,
  MAX_REMEDIATION_LENGTH: 4000,
  MAX_NOTE_LENGTH: 2000,
  MAX_RISK_SCORE: 100,
} as const

export const protocol = {
  TOKEN_HEADER: "X-Lab-Token",
  TRACE_HEADER: "X-Request-Id",
} as const
