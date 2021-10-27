package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

public class List {
    @JsonProperty("type")
    private String type;

    public List() {
        this.type = "list";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
