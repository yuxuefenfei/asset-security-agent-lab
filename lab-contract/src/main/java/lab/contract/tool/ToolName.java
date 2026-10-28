package lab.contract.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 核心只读工具的白名单名称；JSON 使用显式代码，避免 Java 重命名直接改变对外协议。 */
public enum ToolName {
    /** 读取本次固定目标的资产信息。 */
    GET_ASSET("get_asset"),
    /** 读取本次固定目标的扫描记录。 */
    GET_VULNERABILITIES("get_vulnerabilities"),
    /** 检索共享教学知识，不提供写入能力。 */
    SEARCH_KNOWLEDGE("search_knowledge");

    /** 对外协议使用的固定代码，与 Java 枚举名分离。 */
    private final String code;

    ToolName(String code) {
        this.code = code;
    }

    /** 序列化为固定协议代码，而不是默认输出 Java 枚举名。 */
    @JsonValue
    public String code() {
        return code;
    }

    /** 只接受声明过的协议代码，未知值立即报错，避免静默回退到其他含义。 */
    @JsonCreator
    public static ToolName fromCode(String code) {
        for (var value : values()) if (value.code.equals(code)) return value;
        throw new IllegalArgumentException("无效的 ToolName：" + code);
    }
}
