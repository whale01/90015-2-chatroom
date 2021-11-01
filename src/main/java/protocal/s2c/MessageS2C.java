package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * response to message c2s.
 *
 * {
 *     "type":"message",
 *     "identity": "string",
 *     "content": "string"
 * }
 */
public class MessageS2C {
    @JsonProperty("type")
    private String type;
    @JsonProperty("identity")
    private String identity;
    @JsonProperty("content")
    private String content;

    public MessageS2C() {
    }

    public MessageS2C(String identity, String content) {
        this.type = "message";
        this.identity = identity;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
