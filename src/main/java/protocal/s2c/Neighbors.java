package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * response to a listneighbors c2s.
 *
 * {
 *     "type": "neighbors",
 *     "neighbors": ["string"]
 * }
 */
public class Neighbors {
    @JsonProperty("type")
    private String type;
    @JsonProperty("neighbors")
    private List<String> neighbors;

    public Neighbors() {
    }

    public Neighbors(List<String> neighbors) {
        this.type = "neighbors";
        this.neighbors = neighbors;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<String> neighbors) {
        this.neighbors = neighbors;
    }
}
