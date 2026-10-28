package workshop.agent.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import lab.contract.api.LabProtocol;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为 Agent 请求建立线程内追踪上下文，供日志与调用核心 REST 时复用；请求结束必须清理。 */
@Component
public class TraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String trace = request.getHeader(LabProtocol.TRACE_HEADER);
        if (trace == null || !trace.matches("[a-zA-Z0-9_-]{1,64}"))
            trace = UUID.randomUUID().toString();
        MDC.put("traceId", trace);
        response.setHeader(LabProtocol.TRACE_HEADER, trace);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
