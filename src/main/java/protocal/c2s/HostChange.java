package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HostChange{
    @JsonProperty("type")
    private String type;
    @JsonProperty("content")
    private String content;

    public HostChange() {
    }

    public HostChange(String content) {
        this.type = "hostchange";
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
