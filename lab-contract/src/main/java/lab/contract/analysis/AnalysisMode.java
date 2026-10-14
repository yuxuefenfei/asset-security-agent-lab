package lab.contract.analysis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnalysisMode {
    DEMO("demo"),
    LLM("llm");
    private final String code;

    AnalysisMode(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static AnalysisMode fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 AnalysisMode：" + code);
    }
}
