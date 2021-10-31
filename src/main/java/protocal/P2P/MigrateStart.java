package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateStart {

    @JsonProperty("type")
    private String type;
    @JsonProperty("roomid")
    private String roomid;
    @JsonProperty("count")
    private int count;

    public MigrateStart() {
    }

    public MigrateStart(String roomid, int count) {
        this.type = "migratestart";
        this.roomid = roomid;
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
