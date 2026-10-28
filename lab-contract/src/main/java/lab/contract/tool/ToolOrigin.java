package lab.contract.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 工具调用的发起来源；用于区分固定预查询与模型请求，不能用来代表可信程度。 */
public enum ToolOrigin {
    /** 由应用编排代码主动执行的预查询。 */
    JAVA_ORCHESTRATOR("java-orchestrator"),
    /** 模型提出申请后，经工具会话检查并执行的调用。 */
    LLM_REQUEST("llm-request");

    /** 对外协议使用的固定代码，与 Java 枚举名分离。 */
    private final String code;

    ToolOrigin(String code) {
        this.code = code;
    }

    /** 序列化为固定协议代码，而不是默认输出 Java 枚举名。 */
    @JsonValue
    public String code() {
        return code;
    }

    /** 只接受声明过的协议代码，未知值立即报错，避免静默回退到其他含义。 */
    @JsonCreator
    public static ToolOrigin fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 ToolOrigin：" + code);
    }
}
