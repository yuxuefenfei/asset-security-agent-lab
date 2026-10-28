package lab.exception;

import lab.enums.ErrorCode;
import lombok.Getter;

/** 携带稳定错误码的业务异常；异常处理器据此选择 HTTP 状态，cause 仅保留供服务端诊断。 */
@Getter
public class LabException extends RuntimeException {

    /** 可对外公开的业务错误分类；避免由错误文本猜测 HTTP 状态。 */
    private final ErrorCode code;

    public LabException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public LabException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
