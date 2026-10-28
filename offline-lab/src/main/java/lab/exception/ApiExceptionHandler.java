package lab.exception;

import lab.contract.api.ApiResponse;
import lab.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 核心控制器的统一异常出口；把业务错误映射为 HTTP 状态和响应信封，内部异常详情只写服务端日志。 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LabException.class)
    ResponseEntity<ApiResponse<Void>> business(LabException ex) {
        if (ex.getCode() == ErrorCode.UPSTREAM_FAILURE)
            log.warn("模型调用失败 traceId={}", MDC.get("traceId"));
        return failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(SecurityException ex) {
        return failure(ErrorCode.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMediaTypeNotSupportedException.class
    })
    ResponseEntity<ApiResponse<Void>> invalid(Exception ex) {
        return failure(
                ErrorCode.INVALID_REQUEST,
                ex instanceof IllegalArgumentException ? ex.getMessage() : "参数格式、字段或取值不合法");
    }

    @ExceptionHandler({
        NoResourceFoundException.class,
        HttpRequestMethodNotSupportedException.class
    })
    ResponseEntity<ApiResponse<Void>> missing(Exception ex) {
        return failure(ErrorCode.NOT_FOUND, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        // 详细堆栈只留服务端日志，客户端通过 traceId 定位，不返回 SQL 或上游原文。
        log.error("执行失败 traceId={}", MDC.get("traceId"), ex);
        return failure(ErrorCode.INTERNAL_ERROR, "执行失败，请使用 traceId 查询服务日志");
    }

    private ResponseEntity<ApiResponse<Void>> failure(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.failure(message, code.name(), MDC.get("traceId")));
    }
}
