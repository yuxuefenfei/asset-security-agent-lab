package lab.contract.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String error, String code, String traceId) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static ApiResponse<Void> failure(String error, String code, String traceId) {
        return new ApiResponse<>(false, null, error, code, traceId);
    }
}
