package lab.contract.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 独立 Spring Agent 的工具接入方式：本地 @Tool 方法或远端 MCP 回调；JSON 使用小写代码。 */
public enum ToolBackend {
    /** 通过本次请求创建的本地工具对象调用核心接口。 */
    LOCAL("local"),
    /** 通过已连接的远端协议工具回调调用核心接口。 */
    MCP("mcp");

    /** 对外协议使用的固定代码，与 Java 枚举名分离。 */
    private final String code;

    ToolBackend(String code) {
        this.code = code;
    }

    /** 序列化为固定协议代码，而不是默认输出 Java 枚举名。 */
    @JsonValue
    public String code() {
        return code;
    }

    /** 只接受声明过的协议代码，未知值立即报错，避免静默回退到其他含义。 */
    @JsonCreator
    public static ToolBackend fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 ToolBackend：" + code);
    }
}
