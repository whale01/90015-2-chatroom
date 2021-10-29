package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * message s2c response expected (both to the sender & current room)
 * on response output formatted message to stdout
 *
 * {
 *     "type":"message",
 *     "content": "string"
 * }
 */
public class MessageC2S {
    @JsonProperty("type")
    private String type;
    @JsonProperty("content")
    private String content;

    public MessageC2S() {
    }

    public MessageC2S(String content) {
        this.type = "message";
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
