package workshop.agent.web;

import lab.contract.api.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

/** 独立 Agent 的异常出口；保留其 ErrorResponse 契约，上游故障详情留在服务端。 */
@Slf4j
@RestControllerAdvice
public class AgentExceptionHandler {
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<ErrorResponse> forbidden(SecurityException ex) {
        return error(403, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ErrorResponse> invalid(Exception ex) {
        return error(400, "INVALID_REQUEST", "参数或工具调用不合法");
    }

    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    ResponseEntity<ErrorResponse> upstream(Exception ex) {
        log.warn("模型或工具调用失败 traceId={}", MDC.get("traceId"));
        return error(502, "UPSTREAM_FAILURE", "模型或工具服务调用失败，请检查服务状态");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("执行失败 traceId={}", MDC.get("traceId"), ex);
        return error(500, "INTERNAL_ERROR", "执行失败，请查询服务日志");
    }

    private ResponseEntity<ErrorResponse> error(int status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(message, code, MDC.get("traceId")));
    }
}
