package lab.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** 上游对话协议中的消息角色；工具结果以 TOOL 角色回传，模型最终说明属于 ASSISTANT。 */
public enum MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL;

    @JsonValue
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
