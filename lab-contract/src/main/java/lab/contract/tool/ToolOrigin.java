package lab.contract.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ToolOrigin {
    JAVA_ORCHESTRATOR("java-orchestrator"),
    LLM_REQUEST("llm-request");
    private final String code;

    ToolOrigin(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static ToolOrigin fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 ToolOrigin：" + code);
    }
}
