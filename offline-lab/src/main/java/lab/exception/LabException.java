package lab.exception;

import lab.enums.ErrorCode;
import lombok.Getter;

@Getter
public class LabException extends RuntimeException {

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
