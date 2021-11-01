package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateUser {

    @JsonProperty("type")
    private String type;
    @JsonProperty("target")
    private String target;
    @JsonProperty("roomid")
    private String roomid;

    public MigrateUser() {
    }

    public MigrateUser(String target, String roomid) {
        this.type = "migrateuser";
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
