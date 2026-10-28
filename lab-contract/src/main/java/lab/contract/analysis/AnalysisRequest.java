package lab.contract.analysis;

import jakarta.validation.constraints.NotBlank;

/**
 * 核心分析入口的请求；IP 的授权由服务端校验，客户端不能自行指定租户。
 *
 * @param ip 目标资产 IP，必须属于当前租户
 * @param question 分析问题；为 null 时使用默认问题，非空时由服务校验长度
 * @param mode 分析模式；为 null 时使用规则演示模式
 */
public record AnalysisRequest(@NotBlank String ip, String question, AnalysisMode mode) {}
