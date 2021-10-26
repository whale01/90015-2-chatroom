import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private String userId;
    private ChatRoom currentRoom;
    private List<ChatRoom> ownRooms;
    private final BufferedWriter bw;

    public User(String userId, ChatRoom currentRoom, BufferedWriter bw) {
        this.userId = userId;
        this.currentRoom = currentRoom;
        this.ownRooms = Collections.synchronizedList(new ArrayList<ChatRoom>());
        this.bw = bw;
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

    /**
     * 任何需要和client发任何消息的场景都用到这个方法。
     */
    public synchronized void sendMsg(String msg) throws IOException {
        bw.write(msg);
        bw.flush();
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
