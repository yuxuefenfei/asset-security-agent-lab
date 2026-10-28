package lab.contract.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 核心 REST 的统一响应信封；泛型数据放在 data 中，失败时提供错误码和追踪编号。 null 字段不输出到 JSON；实际 HTTP 状态仍由控制器或异常处理器决定。
 *
 * @param <T> 成功响应所承载的业务数据类型
 * @param success 业务调用是否成功
 * @param data 成功时的业务数据，失败时为 null
 * @param error 失败时面向使用者的说明
 * @param code 稳定的机器可读错误码，供前端区分恢复方式
 * @param traceId 失败请求的追踪编号，关联响应头与服务端日志
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String error, String code, String traceId) {
    /** 构造成功响应体；只包装业务数据，不负责设置 HTTP 状态。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    /** 构造失败响应体；调用方同时需要设置与错误码匹配的 HTTP 状态。 */
    public static ApiResponse<Void> failure(String error, String code, String traceId) {
        return new ApiResponse<>(false, null, error, code, traceId);
    }
}
