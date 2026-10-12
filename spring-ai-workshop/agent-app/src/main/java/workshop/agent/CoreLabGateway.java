package workshop.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class CoreLabGateway {
    private final RestClient client;
    private final String allowedIp;
    public CoreLabGateway(@Value("${lab.core-url}") String url,
                          @Value("${lab.api-token}") String token,
                          @Value("${lab.allowed-ip}") String allowedIp) {
        this.allowedIp=allowedIp;
        this.client=RestClient.builder().baseUrl(url).defaultHeader("X-Lab-Token",token).build();
    }
    public void verifyIp(String ip) {
        if(!allowedIp.equals(ip)) throw new SecurityException("本实验只允许访问 " + allowedIp);
    }
    public Object getAsset(String ip){verifyIp(ip);return client.get().uri(uri->uri.path("/api/assets").queryParam("ip",ip).build()).retrieve().body(Map.class);}
    public Object getVulnerabilities(String ip){verifyIp(ip);return client.get().uri(uri->uri.path("/api/vulnerabilities").queryParam("ip",ip).build()).retrieve().body(Map.class);}
    public Object searchKnowledge(String query) {
        if(query==null||query.isBlank()||query.length()>200)throw new IllegalArgumentException("查询词不合法");
        return client.get().uri(uri->uri.path("/api/knowledge").queryParam("q",query).build()).retrieve().body(Map.class);
    }
}
