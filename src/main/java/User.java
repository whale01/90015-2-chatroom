import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private String userId; // address:source port, userd for unique identifier, and chatting prefix
    private String ipAndListeningPort; // address:listening port, used when #listneighbors & #searchnetwork

    private ChatRoom currentRoom;
    private final BufferedWriter bw;
    private ServerConnThread serverConnThread;


    public User(String userId, String ipAndListeningPort, ChatRoom currentRoom,  BufferedWriter bw) {
        this.userId = userId;
        this.ipAndListeningPort = ipAndListeningPort;
        this.currentRoom = currentRoom;
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

    public BufferedWriter getBw() {
        return bw;
    }

    public ServerConnThread getServerConnThread() {
        return serverConnThread;
    }

    public void setServerConnThread(ServerConnThread serverConnThread) {
        this.serverConnThread = serverConnThread;
    }

    /**
     * 任何需要和client发任何消息的场景都用到这个方法。
     */
    public synchronized void sendMsg(String msg) throws IOException {
        bw.write(msg + System.lineSeparator());
        bw.flush();
    }

}
