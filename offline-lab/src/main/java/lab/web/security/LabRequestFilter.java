package lab.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import lab.config.*;
import lab.contract.api.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
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
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String trace = request.getHeader(LabProtocol.TRACE_HEADER);
        if (trace == null || !trace.matches("[a-zA-Z0-9_-]{1,64}"))
            trace = UUID.randomUUID().toString();
        MDC.put("traceId", trace);
        response.setHeader(LabProtocol.TRACE_HEADER, trace);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            boolean publicResource =
                    request.getMethod().equals("GET")
                            && Set.of(
                                            "/",
                                            "/assets/app.css",
                                            "/assets/app.js",
                                            "/assets/api.js",
                                            "/assets/report.js",
                                            "/assets/ticket.js",
                                            "/assets/history.js")
                                    .contains(request.getRequestURI());
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
                byte[] body = request.getInputStream().readNBytes(policy.maxBodyBytes() + 1);
                if (body.length > policy.maxBodyBytes()) {
                    fail(response, 400, "请求体过大", "INVALID_REQUEST", trace);
                    return;
                }
                request = new BufferedRequest(request, body);
            }
            chain.doFilter(request, response);
        } finally {
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
