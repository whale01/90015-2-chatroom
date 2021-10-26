import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRoom {
    private List<User> members;
    private String roomId;

    public ChatRoom(String roomId) {
        this.roomId = roomId;
        this.members = Collections.synchronizedList(new ArrayList<User>());
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
