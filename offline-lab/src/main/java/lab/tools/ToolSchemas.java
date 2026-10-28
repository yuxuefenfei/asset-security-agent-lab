package lab.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lab.contract.tool.ToolDefinition;
import lab.contract.tool.ToolFunctionDefinition;
import lab.contract.tool.ToolName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 向模型声明三个只读工具的名称、用途和参数；声明不执行工具，实际授权在 ToolSession 完成。 */
@Component
@RequiredArgsConstructor
public class ToolSchemas {
    private final ObjectMapper json;

    /** 列出模型可申请的只读工具；不提供工单修改、审批或真实扫描能力。 */
    public List<ToolDefinition> definitions() {
        return List.of(
                definition(ToolName.GET_ASSET, "查询目标资产基本信息、归属和网络暴露情况", "ip"),
                definition(ToolName.GET_VULNERABILITIES, "查询目标资产的漏洞扫描记录", "ip"),
                definition(ToolName.SEARCH_KNOWLEDGE, "检索共享的虚构教学知识库，返回来源标识", "query"));
    }

    private ToolDefinition definition(ToolName name, String description, String parameter) {
        // TODO：parameters 应保留根对象；当前链式结果是 required 数组，缺陷说明见 docs/09-核验记录.md。
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
