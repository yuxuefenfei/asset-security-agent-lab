package lab.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lab.contract.tool.ToolDefinition;
import lab.contract.tool.ToolFunctionDefinition;
import lab.contract.tool.ToolName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolSchemas {
    private final ObjectMapper json;

    public List<ToolDefinition> definitions() {
        return List.of(
                definition(ToolName.GET_ASSET, "查询目标资产基本信息、归属和网络暴露情况", "ip"),
                definition(ToolName.GET_VULNERABILITIES, "查询目标资产的漏洞扫描记录", "ip"),
                definition(ToolName.SEARCH_KNOWLEDGE, "检索共享的虚构教学知识库，返回来源标识", "query"));
    }

    private ToolDefinition definition(ToolName name, String description, String parameter) {
        var schema =
                json.createObjectNode()
                        .put("type", "object")
                        .put("additionalProperties", false)
                        .putObject("properties")
                        .putObject(parameter)
                        .put("type", "string")
                        .putArray("required")
                        .add(parameter);
        return new ToolDefinition(
                "function", new ToolFunctionDefinition(name, description, schema));
    }
}
