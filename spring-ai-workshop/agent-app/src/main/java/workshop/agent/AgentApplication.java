package workshop.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 独立 Spring AI 教学应用入口；通过核心 REST 读取训练数据，本应用不负责核心报告持久化。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
