package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ListNeighbours {
    @JsonProperty("type")
    private String type;

    public ListNeighbours() {
        this.type = "listneighbors";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
