package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomChange {
    /**
     * response to join, quit c2s and disconnection (abrupt/unplanned) if in a room,
     * to other connections in that room.
     * {
     *     "type":"roomchange",
     *     "identity": "string",
     *     "former": "string",
     *     "roomid": "string"
     * }
     */

    @JsonProperty("type")
    private String type;
    @JsonProperty("identity")
    private String identity;
    @JsonProperty("former")
    private String former;
    @JsonProperty("roomid")
    private String roomid;

    public RoomChange(String identity, String former, String roomid) {
        this.type = "roomchange";
        this.identity = identity;
        this.former = former;
        this.roomid = roomid;
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

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    @Override
    public String toString() {
        return "RoomChange{" +
                "type='" + type + '\'' +
                ", identity='" + identity + '\'' +
                ", former='" + former + '\'' +
                ", roomid='" + roomid + '\'' +
                '}';
    }
}
