package lab.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

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
