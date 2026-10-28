package lab.contract.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lab.contract.api.ApiConstraints;

/**
 * 独立 Spring Agent 的输入；与核心分析请求分开，因该入口只返回解释而不保存报告。
 *
 * @param ip 目标 IP，需满足 Agent 白名单和核心服务授权
 * @param question 用户希望模型解释的问题，不能为空且有长度限制
 */
public record AgentRequest(
        @NotBlank String ip,
        @NotBlank @Size(max = ApiConstraints.MAX_QUESTION_LENGTH) String question) {}
