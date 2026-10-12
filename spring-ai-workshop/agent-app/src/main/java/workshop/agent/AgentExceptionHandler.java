package workshop.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestControllerAdvice
public class AgentExceptionHandler {
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<?> forbidden(SecurityException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalidRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "参数或工具调用不合法"));
    }

    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    ResponseEntity<?> upstreamFailure(Exception ex) {
        return ResponseEntity.status(502).body(Map.of("error", "模型或工具服务调用失败，请检查服务状态"));
    }
}