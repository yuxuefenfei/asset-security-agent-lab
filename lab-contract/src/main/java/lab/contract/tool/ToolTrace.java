package lab.contract.tool;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolTrace(
        int step, ToolOrigin origin, ToolName tool, JsonNode arguments, int resultCount) {}
