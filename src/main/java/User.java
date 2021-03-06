import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

/**
 * The User domain class.
 * Used to store information on both the client side and the server side.
 * When used on the client side, it reps the current User, which is the owner of all rooms in this peer server:
 *
 *      the bw and serverConnThread would obviously be null.
 *      the userId is used as a unique identifier;
 *      the ipAndListeningPort is used to store this info, for the use of #listneighbors & #searchnetwork;
 *      the currentRoom points to the current ChatRoom.
 *
 *  When used on the server side, it reps a user that's connected to the server.
 *  A User will be created with all fields excluding currentRoom (as haven't joined any room yet)
 *
 *      the userId and ipAndListeningPort should be consistent with the corresponding local User (especially userId)
 *      the currentRoom points to the current ChatRoom.
 *      the bw and serverConnThread would be 1:1 along with the TCP connection between client and server.
 *
 */
public class User {
    private String userId; // address:source port, userid for unique identifier, and chatting prefix
    private String address; // address:listening port, used when #listneighbors & #searchnetwork

    private ChatRoom currentRoom;

    private final BufferedWriter bw;
    private ServerConnThread serverConnThread;
    private Socket socket; // socket from server


    public User(String userId, String address, ChatRoom currentRoom,  BufferedWriter bw) {
        this.userId = userId;
        this.currentRoom = currentRoom;
        this.bw = bw;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address != null) {
            this.address = address;
        }
    }

    public ServerConnThread getServerConnThread() {
        return serverConnThread;
    }

    public void setServerConnThread(ServerConnThread serverConnThread) {
        this.serverConnThread = serverConnThread;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * ???????????????client????????????????????????????????????????????????
     */
    public synchronized void sendMsg(String msg) throws IOException {
        bw.write(msg + System.lineSeparator());
        bw.flush();
    }

}
