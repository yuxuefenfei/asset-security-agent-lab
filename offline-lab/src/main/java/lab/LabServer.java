package lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 核心 Spring Boot 启动入口，同时扫描 lab 下的组件和类型化配置。 建议新手按 Controller → Service → Repository/Mapper → PO
 * 的顺序阅读一次 HTTP 请求； 再从 AnalysisService 进入 ToolSession 与 ModelClient，理解规则、工具、模型的分工。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LabServer {
    public static void main(String[] args) {
        SpringApplication.run(LabServer.class, args);
    }
}
