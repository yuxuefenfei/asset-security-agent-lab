package lab.contract.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ToolName {
    GET_ASSET("get_asset"),
    GET_VULNERABILITIES("get_vulnerabilities"),
    SEARCH_KNOWLEDGE("search_knowledge");
    private final String code;

    ToolName(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static ToolName fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 ToolName：" + code);
    }
}
