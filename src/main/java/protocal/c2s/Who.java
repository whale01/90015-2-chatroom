package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * roomcontents s2c response expected.
 * on response output formatted room contents to stdout
 * {
 *     "type":"who",
 *     "roomid": "string"
 * }
 */
public class Who {
    @JsonProperty("type")
    private String type;
    @JsonProperty("roomid")
    private String roomid;

    public Who() {
    }

    public Who(String roomid) {
        this.type = "who";
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
