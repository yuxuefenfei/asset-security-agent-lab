package lab.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PagePolicy {
    private final PolicyProperties policy;

    public void validate(long page, long size) {
        if (page < 1 || size < 1 || size > policy.maxPageSize() || page > Integer.MAX_VALUE)
            throw new IllegalArgumentException("分页参数不合法");
    }
}
