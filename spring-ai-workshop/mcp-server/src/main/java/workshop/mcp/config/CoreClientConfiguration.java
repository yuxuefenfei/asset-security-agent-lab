package workshop.mcp.config;

import lab.contract.api.LabProtocol;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CoreClientConfiguration {
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
