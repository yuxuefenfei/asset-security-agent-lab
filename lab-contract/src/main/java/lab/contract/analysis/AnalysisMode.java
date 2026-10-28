package lab.contract.analysis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 核心分析执行模式：DEMO 只走规则，LLM 在规则结果外增加模型解释。 JSON 使用 demo/llm；Spring 查询参数绑定使用枚举名 DEMO/LLM，二者不可混淆。 */
public enum AnalysisMode {
    DEMO("demo"),
    LLM("llm");

    /** 对外协议使用的固定代码，与 Java 枚举名分离。 */
    private final String code;

    AnalysisMode(String code) {
        this.code = code;
    }

    /** 序列化为固定协议代码，而不是默认输出 Java 枚举名。 */
    @JsonValue
    public String code() {
        return code;
    }

    /** 只接受声明过的协议代码，未知值立即报错，避免静默回退到其他含义。 */
    @JsonCreator
    public static AnalysisMode fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 AnalysisMode：" + code);
    }
}
