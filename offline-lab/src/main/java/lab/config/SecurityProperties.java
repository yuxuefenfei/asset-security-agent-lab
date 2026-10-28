package lab.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 本地实验的访问配置，绑定 lab.security；当前以单个服务端租户作为数据隔离边界。
 *
 * @param apiToken 受保护 HTTP 请求需要携带的实验口令，不是用户身份系统
 * @param approvalCode 人工审批需要提供的本地口令，与 API 访问口令用途不同
 * @param tenantId 当前服务代表的租户，不能由请求参数或模型自由指定
 * @param reviewer 审批成功时写入工单的审核人展示名
 */
@Validated
@ConfigurationProperties("lab.security")
public record SecurityProperties(
        @NotBlank String apiToken,
        @NotBlank String approvalCode,
        @NotBlank String tenantId,
        @NotBlank String reviewer) {}
