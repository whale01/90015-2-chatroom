package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * roomchange s2c response expected (both to the sender & current room if != “” & entering room if != “”).
 * {
 *     "type":"join",
 *     "roomid": "string"
 * }
 */
public class Join {
    @JsonProperty("type")
    private String type;
    @JsonProperty("roomid")
    private String roomid;

    public Join() {
    }

    public Join(String roomid) {
        this.type = "join";
        this.roomid = roomid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

}
