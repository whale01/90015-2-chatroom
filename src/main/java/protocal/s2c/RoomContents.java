package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RoomContents {
    @JsonProperty("type")
    private String type;
    @JsonProperty("roomid")
    private String roomid;
    @JsonProperty("identities")
    private List<String> identities;

    public RoomContents() {
    }

    public RoomContents(String roomid, List<String> identities) {
        this.type = "roomcontents";
        this.roomid = roomid;
        this.identities = identities;
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

    public List<String> getIdentities() {
        return identities;
    }

    public void setIdentities(List<String> identities) {
        this.identities = identities;
    }

    @Override
    public String toString() {
        return "RoomContents{" +
                "type='" + type + '\'' +
                ", roomid='" + roomid + '\'' +
                ", identities=" + identities +
                '}';
    }
}
