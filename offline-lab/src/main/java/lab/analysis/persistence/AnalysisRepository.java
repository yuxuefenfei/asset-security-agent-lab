package lab.analysis.persistence;

import static lab.analysis.persistence.table.AnalysisEvidenceTableDef.ANALYSIS_EVIDENCE;
import static lab.analysis.persistence.table.AnalysisTableDef.ANALYSIS;
import static lab.tool.persistence.table.ToolTraceTableDef.TOOL_TRACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lab.config.SecurityProperties;
import lab.contract.analysis.*;
import lab.contract.api.PageResponse;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.tool.ToolTrace;
import lab.contract.vulnerability.VulnerabilityResponse;
import lab.tool.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AnalysisRepository {
    private final AnalysisMapper analyses;
    private final AnalysisEvidenceMapper evidence;
    private final ToolTraceMapper traces;
    private final ObjectMapper json;
    private final SecurityProperties security;

    @Transactional
    public void save(AnalysisReport report) {
        var row = new AnalysisPO();
        row.setId(report.id());
        row.setTenantId(security.tenantId());
        row.setAssetIp(report.asset().ip());
        row.setCreatedAt(Instant.parse(report.time()));
        row.setMode(report.mode());
        row.setRiskScore(report.riskScore());
        row.setRiskLevel(report.riskLevel());
        row.setSummary(report.summary());
        row.setWarning(report.warning());
        row.setStatus(report.status());
        row.setAssetSnapshot(encode(report.asset()));
        row.setVulnerabilitiesSnapshot(encode(report.vulnerabilities()));
        analyses.insert(row);
        int position = 0;
        for (var document : report.knowledgeEvidence()) {
            var item = new AnalysisEvidencePO();
            item.setId(UUID.randomUUID().toString());
            item.setAnalysisId(report.id());
            item.setPosition(++position);
            item.setKnowledgeId(document.id());
            item.setTitle(document.title());
            item.setKeywords(document.keywords());
            item.setBody(document.body());
            item.setSource(document.source());
            evidence.insert(item);
        }
        for (var trace : report.toolTrace()) {
            var item = new ToolTracePO();
            item.setId(UUID.randomUUID().toString());
            item.setAnalysisId(report.id());
            item.setStep(trace.step());
            item.setOrigin(trace.origin());
            item.setTool(trace.tool());
            item.setArgumentsJson(encode(trace.arguments()));
            item.setResultCount(trace.resultCount());
            traces.insert(item);
        }
    }

    @Transactional(readOnly = true)
    public AnalysisReport get(String id) {
        var row =
                analyses.selectOneByQuery(
                        QueryWrapper.create()
                                .where(ANALYSIS.ID.eq(id))
                                .and(ANALYSIS.TENANT_ID.eq(security.tenantId())));
        if (row == null) throw new IllegalArgumentException("分析结果不存在");
        var documents =
                evidence
                        .selectListByQuery(
                                QueryWrapper.create()
                                        .where(ANALYSIS_EVIDENCE.ANALYSIS_ID.eq(id))
                                        .orderBy(ANALYSIS_EVIDENCE.POSITION.asc()))
                        .stream()
                        .map(
                                k ->
                                        new KnowledgeResponse(
                                                k.getKnowledgeId(),
                                                k.getTitle(),
                                                k.getKeywords(),
                                                k.getBody(),
                                                k.getSource()))
                        .toList();
        var toolTrace =
                traces
                        .selectListByQuery(
                                QueryWrapper.create()
                                        .where(TOOL_TRACE.ANALYSIS_ID.eq(id))
                                        .orderBy(TOOL_TRACE.STEP.asc()))
                        .stream()
                        .map(
                                t ->
                                        new ToolTrace(
                                                t.getStep(),
                                                t.getOrigin(),
                                                t.getTool(),
                                                tree(t.getArgumentsJson()),
                                                t.getResultCount()))
                        .toList();
        try {
            return new AnalysisReport(
                    row.getId(),
                    row.getCreatedAt().toString(),
                    row.getMode(),
                    json.readValue(row.getAssetSnapshot(), AssetResponse.class),
                    json.readValue(
                            row.getVulnerabilitiesSnapshot(),
                            new TypeReference<List<VulnerabilityResponse>>() {}),
                    row.getRiskScore(),
                    row.getRiskLevel(),
                    documents,
                    row.getSummary(),
                    row.getWarning(),
                    toolTrace,
                    row.getStatus());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("报告快照读取失败", ex);
        }
    }

    public List<AnalysisReport> list(int limit) {
        return analyses.selectListByQuery(baseQuery().limit(limit)).stream()
                .map(row -> get(row.getId()))
                .toList();
    }

    public PageResponse<AnalysisSummary> page(long page, long size) {
        var result =
                analyses.paginate(
                        page,
                        size,
                        baseQuery()
                                .select(
                                        ANALYSIS.ID,
                                        ANALYSIS.CREATED_AT,
                                        ANALYSIS.MODE,
                                        ANALYSIS.ASSET_IP,
                                        ANALYSIS.RISK_SCORE,
                                        ANALYSIS.RISK_LEVEL,
                                        ANALYSIS.STATUS));
        return new PageResponse<>(
                result.getRecords().stream()
                        .map(
                                row ->
                                        new AnalysisSummary(
                                                row.getId(),
                                                row.getCreatedAt().toString(),
                                                row.getMode(),
                                                row.getAssetIp(),
                                                row.getRiskScore(),
                                                row.getRiskLevel(),
                                                row.getStatus()))
                        .toList(),
                page,
                size,
                result.getTotalRow());
    }

    private QueryWrapper baseQuery() {
        return QueryWrapper.create()
                .where(ANALYSIS.TENANT_ID.eq(security.tenantId()))
                .orderBy(ANALYSIS.CREATED_AT.desc(), ANALYSIS.ID.asc());
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("报告快照保存失败", ex);
        }
    }

    private JsonNode tree(String value) {
        try {
            return json.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("工具快照读取失败", ex);
        }
    }
}
