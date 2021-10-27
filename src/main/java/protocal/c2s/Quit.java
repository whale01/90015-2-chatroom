package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * roomchange s2c response expected (both to the sender & current room).
 * on response close connection socket.
 *
 * {
 *     "type":"quit"
 * }
 */
public class Quit {
    @JsonProperty("type")
    private String type;

    public Quit() {
        this.type = "quit";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
