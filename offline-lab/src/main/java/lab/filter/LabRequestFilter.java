package lab.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import lab.config.PolicyProperties;
import lab.config.SecurityProperties;
import lab.contract.api.ApiResponse;
import lab.contract.api.LabProtocol;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class LabRequestFilter extends OncePerRequestFilter {

    private final SecurityProperties security;
    private final PolicyProperties policy;
    private final ObjectMapper json;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        String trace = request.getHeader(LabProtocol.TRACE_HEADER);
        if (trace == null || !trace.matches("[a-zA-Z0-9_-]{1,64}"))
            trace = UUID.randomUUID().toString();
        MDC.put("traceId", trace);
        response.setHeader(LabProtocol.TRACE_HEADER, trace);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            String uri = request.getRequestURI();
            // 只放行编译产物的明确路径，未知资源仍需口令，不能把 /assets 整段开放。
            boolean publicResource =
                    request.getMethod().equals("GET")
                            && (Set.of("/", "/index.html", "/favicon.svg").contains(uri)
                                    || uri.matches(
                                            "/assets/[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*\\.(?:js|css|woff2)"));
            if (!publicResource) {
                String token = request.getHeader(LabProtocol.TOKEN_HEADER);
                if (token == null
                        || !MessageDigest.isEqual(
                                security.apiToken().getBytes(StandardCharsets.UTF_8),
                                token.getBytes(StandardCharsets.UTF_8))) {
                    fail(response, 403, "需要有效的 X-Lab-Token（本地实验口令）", "FORBIDDEN", trace);
                    return;
                }
            }
            if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
                // 多读一个字节即可识别超限，不能先无限读完再判断请求大小。
                byte[] body = request.getInputStream().readNBytes(policy.maxBodyBytes() + 1);
                if (body.length > policy.maxBodyBytes()) {
                    fail(response, 400, "请求体过大", "INVALID_REQUEST", trace);
                    return;
                }
                request = new BufferedRequest(request, body);
            }
            chain.doFilter(request, response);
        } finally {
            // Servlet 线程会复用，必须清理请求上下文，避免下个请求沿用旧编号。
            MDC.remove("traceId");
        }
    }

    private void fail(
            HttpServletResponse response, int status, String message, String code, String trace)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), ApiResponse.failure(message, code, trace));
    }
}
