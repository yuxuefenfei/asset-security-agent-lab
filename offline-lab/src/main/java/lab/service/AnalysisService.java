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

/** 单资产分析的应用编排器：固定授权快照、预查询证据、规则评分、按需调用模型，最后保存完整报告。 业务评分始终由 RiskPolicy 决定；模型仅补充解释，模型失败时不会落库为成功报告。 */
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

    /** 所有分析共享的并发闸门，获取失败立即返回繁忙错误，不让 HTTP 请求无限排队。 */
    private final Semaphore capacity;

    /** 同步执行并保存一次分析；从获取并发名额到保存报告都属于本次调用，finally 保证释放名额。 */
    public AnalysisReport analyze(AnalysisRequest request) {
        String question = request.question() == null ? "分析资产安全风险" : request.question();
        if (question.isBlank() || question.length() > policy.maxQuestionLength())
            throw new IllegalArgumentException("问题不能为空或超过" + policy.maxQuestionLength() + "字");
        AnalysisMode mode = request.mode() == null ? AnalysisMode.DEMO : request.mode();
        // 容量覆盖本次分析全流程；tryAcquire 不排队，避免 HTTP 请求无限等待。
        if (!capacity.tryAcquire()) throw new LabException(ErrorCode.TOO_BUSY, "分析任务繁忙，请稍后重试");
        try {
            // 会话工厂先校验租户并固定资产/漏洞；以下预查询同样进入成功工具轨迹。
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
            // 在联系模型前计算结构化评分，模型文字不能改写这些业务字段。
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
            // 模型异常不会执行到这里，避免把失败流程保存成成功报告。
            reports.save(report);
            return report;
        } finally {
            capacity.release();
        }
    }

    /** 读取当前租户的完整历史报告，内容由保存时快照重组。 */
    public AnalysisReport get(String id) {
        return reports.get(id);
    }

    /** 兼容旧调用方的有限列表；新页面应使用摘要分页，避免重复加载完整报告。 */
    public List<AnalysisReport> list() {
        return reports.list(policy.legacyListLimit());
    }

    /** 分页读取必要的报告摘要列，完整快照仅在打开报告详情时加载。 */
    public PageResponse<AnalysisSummary> page(
            long page, long size, String q, RiskLevel riskLevel, AnalysisMode mode) {
        pages.validate(page, size);
        return reports.page(page, size, pages.keyword(q), riskLevel, mode);
    }

    public PageResponse<AnalysisSummary> page(long page, long size) {
        pages.validate(page, size);
        return reports.page(page, size);
    }
}
