import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private String userId;
    private ChatRoom currentRoom;
    private List<ChatRoom> ownRooms;

    public User(String userId, ChatRoom currentRoom) {
        this.userId = userId;
        this.currentRoom = currentRoom;
        this.ownRooms = Collections.synchronizedList(new ArrayList<ChatRoom>());
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ChatRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(ChatRoom currentRoom) {
        this.currentRoom = currentRoom;
    }

    public List<ChatRoom> getOwnRooms() {
        return ownRooms;
    }

    public void setOwnRooms(List<ChatRoom> ownRooms) {
        this.ownRooms = ownRooms;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", currentRoom=" + currentRoom +
                ", ownRooms=" + ownRooms +
                '}';
    }
}
