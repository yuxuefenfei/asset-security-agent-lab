package lab.controller;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.tool.ToolDefinition;
import lab.tools.ToolSchemas;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 工具声明查询接口，方便学习者查看模型可申请的能力；此接口本身不执行工具。 */
@RestController
@RequiredArgsConstructor
public class ToolController {
    private final ToolSchemas schemas;

    @GetMapping("/api/tools")
    public ApiResponse<List<ToolDefinition>> schemas() {
        return ApiResponse.ok(schemas.definitions());
    }
}
