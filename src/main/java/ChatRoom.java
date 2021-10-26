import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRoom {
    private List<User> members;
    private User owner;
    private String roomId;

    public ChatRoom(User owner, String roomId) {
        this.owner = owner;
        this.roomId = roomId;
        this.members = Collections.synchronizedList(new ArrayList<User>());
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
