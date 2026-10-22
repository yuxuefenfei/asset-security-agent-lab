package lab.service;

import lab.config.PolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PagePolicy {
    private final PolicyProperties policy;

    /** 为目录 LIKE 搜索转义通配符；值仍交给框架参数绑定，转义不是 SQL 拼接许可。 */
    public String keyword(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() > policy.maxQueryLength())
            throw new IllegalArgumentException("搜索内容不能超过" + policy.maxQueryLength() + "字");
        return value.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public void validate(long page, long size) {
        if (page < 1 || size < 1 || size > policy.maxPageSize() || page > Integer.MAX_VALUE)
            throw new IllegalArgumentException("分页参数不合法");
    }
}
