package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * response to list c2s.
 *
 * {
 *     "type":"roomlist",
 *     "rooms": [{"roomid": "string", "count": "int"}]
 * }
 */
public class RoomList {
    @JsonProperty("type")
    private String type;
    @JsonProperty("rooms")
    private List<Room> rooms;

    public RoomList() {
    }

    public RoomList(List<Room> rooms) {
        this.type = "roomlist";
        this.rooms = rooms;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

}
