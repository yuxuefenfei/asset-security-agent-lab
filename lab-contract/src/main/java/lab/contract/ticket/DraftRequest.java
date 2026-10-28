package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;

/**
 * 从已保存报告创建工单草稿的请求；重复提交会返回同一张工单。
 *
 * @param analysisId 来源报告标识，必须能在当前租户中读取
 */
public record DraftRequest(@NotBlank String analysisId) {}
