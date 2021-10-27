package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Room{
    @JsonProperty("roomid")
    private String roomid;
    @JsonProperty("count")
    private int count;

    public Room(String roomid, int count) {
        this.roomid = roomid;
        this.count = count;
    }

    public Room() {
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