package lab.controller;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.tool.ToolDefinition;
import lab.tools.ToolSchemas;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ToolController {
    private final ToolSchemas schemas;

    @GetMapping("/api/tools")
    public ApiResponse<List<ToolDefinition>> schemas() {
        return ApiResponse.ok(schemas.definitions());
    }
}
