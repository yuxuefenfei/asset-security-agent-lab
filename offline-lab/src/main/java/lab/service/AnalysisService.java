package lab.service;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.Semaphore;
import lab.client.ModelClient;
import lab.config.PolicyProperties;
import lab.contract.analysis.*;
import lab.contract.api.PageResponse;
import lab.contract.tool.*;
import lab.dao.AnalysisRepository;
import lab.enums.ErrorCode;
import lab.exception.LabException;
import lab.tools.ToolSessionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final ToolSessionFactory sessions;
    private final RiskPolicy risk;
    private final AnalysisRepository reports;
    private final ModelClient model;
    private final PolicyProperties policy;
    private final PagePolicy pages;
    private final Clock clock;
    private final Semaphore capacity;

    public AnalysisReport analyze(AnalysisRequest request) {
        String question = request.question() == null ? "分析资产安全风险" : request.question();
        if (question.isBlank() || question.length() > policy.maxQuestionLength())
            throw new IllegalArgumentException("问题不能为空或超过" + policy.maxQuestionLength() + "字");
        AnalysisMode mode = request.mode() == null ? AnalysisMode.DEMO : request.mode();
        if (!capacity.tryAcquire()) throw new LabException(ErrorCode.TOO_BUSY, "分析任务繁忙，请稍后重试");
        try {
            var tools = sessions.create(request.ip());
            tools.call(ToolName.GET_ASSET, tools.ipArguments(), ToolOrigin.JAVA_ORCHESTRATOR);
            tools.call(
                    ToolName.GET_VULNERABILITIES,
                    tools.ipArguments(),
                    ToolOrigin.JAVA_ORCHESTRATOR);
            for (var vulnerability : tools.vulnerabilities())
                tools.call(
                        ToolName.SEARCH_KNOWLEDGE,
                        tools.queryArguments(vulnerability.advisory()),
                        ToolOrigin.JAVA_ORCHESTRATOR);
            int score = risk.score(tools.asset(), tools.vulnerabilities());
            var level = risk.level(score);
            String summary =
                    "【离线教学规则】资产"
                            + request.ip()
                            + "有"
                            + tools.vulnerabilities().size()
                            + "条模拟扫描记录。当前风险等级"
                            + level
                            + "（示例评分"
                            + score
                            + "/100）；请在正式整改前确认实际版本、暴露面及影响。";
            if (mode == AnalysisMode.LLM) {
                var explanation = model.reason(request.ip(), question, tools);
                summary = explanation.isBlank() ? "模型未输出解释，已保留规则分析。" : explanation;
            }
            var report =
                    new AnalysisReport(
                            UUID.randomUUID().toString(),
                            clock.instant().toString(),
                            mode,
                            tools.asset(),
                            tools.vulnerabilities(),
                            score,
                            level,
                            tools.evidence(),
                            summary,
                            "本系统为演示数据，LAB-* 非真实 CVE。此评分仅用于学习，内容必须人工复核。",
                            tools.trace(),
                            AnalysisStatus.ANALYZED);
            reports.save(report);
            return report;
        } finally {
            capacity.release();
        }
    }

    public AnalysisReport get(String id) {
        return reports.get(id);
    }

    public List<AnalysisReport> list() {
        return reports.list(policy.legacyListLimit());
    }

    public PageResponse<AnalysisSummary> page(long page, long size) {
        pages.validate(page, size);
        return reports.page(page, size);
    }
}
