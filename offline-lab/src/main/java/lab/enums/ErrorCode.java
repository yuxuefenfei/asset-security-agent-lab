package lab.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 核心 API 的稳定错误码及 HTTP 状态映射；前端据此区分输入错误、并发冲突和服务故障。 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(400),
    INVALID_STATE(400),
    FORBIDDEN(403),
    NOT_FOUND(404),
    VERSION_CONFLICT(409),
    TOO_BUSY(429),
    INTERNAL_ERROR(500),
    UPSTREAM_FAILURE(502);

    /** 该错误对应的 HTTP 状态码；错误码名称通过响应体 code 提供更具体的分类。 */
    private final int status;
}
