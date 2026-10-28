package lab.service;

import lab.config.PolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 分页与目录搜索的公共校验规则，限制查询容量并把搜索通配符作为普通文本处理。 */
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

    /** 页码从 1 开始且不超过整数范围，每页大小不能超过配置上限。 */
    public void validate(long page, long size) {
        if (page < 1 || size < 1 || size > policy.maxPageSize() || page > Integer.MAX_VALUE)
            throw new IllegalArgumentException("分页参数不合法");
    }
}
