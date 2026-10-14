package lab.config;

import jakarta.validation.constraints.*;
import lab.contract.api.ApiConstraints;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
