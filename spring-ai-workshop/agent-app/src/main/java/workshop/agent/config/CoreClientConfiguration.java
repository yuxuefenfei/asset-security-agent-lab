package workshop.agent.config;

import lab.contract.api.LabProtocol;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 配置 Agent 到核心 REST 的 HTTP 客户端，统一地址、实验口令、超时及追踪头传播。 */
@Configuration
public class CoreClientConfiguration {
    /** 统一构造访问核心 REST 的客户端；携带实验口令，并转发当前线程的请求编号以关联日志。 */
    @Bean
    RestClient coreRestClient(LabProperties config, RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.connectTimeout());
        factory.setReadTimeout(config.requestTimeout());
        return builder.baseUrl(config.coreUrl().toString())
                .requestFactory(factory)
                .defaultHeader(LabProtocol.TOKEN_HEADER, config.apiToken())
                .requestInterceptor(
                        (request, body, execution) -> {
                            String trace = MDC.get("traceId");
                            if (trace != null)
                                request.getHeaders().set(LabProtocol.TRACE_HEADER, trace);
                            return execution.execute(request, body);
                        })
                .build();
    }
}
