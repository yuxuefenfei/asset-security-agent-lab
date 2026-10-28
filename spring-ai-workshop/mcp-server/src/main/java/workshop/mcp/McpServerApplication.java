package workshop.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** MCP 教学服务入口；把只读核心查询注册为可被 MCP Client 发现和调用的工具。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
