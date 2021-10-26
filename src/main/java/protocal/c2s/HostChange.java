package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class HostChange implements Serializable {
    @JsonProperty("type")
    private String type;
    @JsonProperty("content")
    private String content;

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
