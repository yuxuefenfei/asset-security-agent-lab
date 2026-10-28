package lab.config;

import jakarta.validation.constraints.*;
import lab.contract.api.ApiConstraints;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 核心业务容量策略，绑定 lab.policy；与固定接口上限分开，允许环境配置进一步收紧。
 *
 * @param maxQuestionLength 核心分析问题的最大字符数
 * @param maxQueryLength 搜索词的最大字符数
 * @param maxBodyBytes 写入请求体的最大字节数，由过滤器先行限制
 * @param knowledgeLimit 每次分析工具检索最多返回的知识条数
 * @param apiKnowledgeLimit 公开 REST 知识搜索每次最多返回的条数
 * @param legacyListLimit 兼容旧列表接口的固定返回上限，新页面使用分页接口
 * @param maxPageSize 分页接口允许的最大每页容量
 */
@Validated
@ConfigurationProperties("lab.policy")
public record PolicyProperties(
        @Positive @Max(ApiConstraints.MAX_QUESTION_LENGTH) int maxQuestionLength,
        @Positive @Max(ApiConstraints.MAX_QUERY_LENGTH) int maxQueryLength,
        @Positive int maxBodyBytes,
        @Positive int knowledgeLimit,
        @Positive int apiKnowledgeLimit,
        @Positive int legacyListLimit,
        @Positive int maxPageSize) {}
