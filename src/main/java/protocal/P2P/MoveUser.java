package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveUser {

    @JsonProperty("type")
    private final String type = "moveuser";
    @JsonProperty("target")
    private String target;
    @JsonProperty("roomid")
    private String roomid;

    public MoveUser() {
    }

    public MoveUser(String target, String roomid) {
        this.target = target;
        this.roomid = roomid;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }
}
