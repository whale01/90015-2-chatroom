package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateStart {

    @JsonProperty("type")
    private final static String type = "migratestart";
    @JsonProperty("roomid")
    private String roomid;
    @JsonProperty("count")
    private int count;

    public MigrateStart(String roomid, int count) {
        this.roomid = roomid;
        this.count = count;
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
