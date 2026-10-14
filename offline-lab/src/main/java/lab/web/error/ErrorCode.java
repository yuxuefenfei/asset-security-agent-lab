package lab.web.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
    private final int status;
}
