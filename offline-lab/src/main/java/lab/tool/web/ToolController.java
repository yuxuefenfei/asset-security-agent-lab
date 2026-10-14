package lab.tool.web;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.tool.ToolDefinition;
import lab.tool.application.*;
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
