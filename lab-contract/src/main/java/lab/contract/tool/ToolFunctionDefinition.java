package lab.contract.tool;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolFunctionDefinition(ToolName name, String description, JsonNode parameters) {}
