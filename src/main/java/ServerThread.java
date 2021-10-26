import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;
import protocal.c2s.Join;
import protocal.s2c.RoomChange;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ServerThread extends Thread{
    private ObjectMapper mapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private int pPort;
    private Peer peer;
    private final Map<String, ChatRoom> chatRooms;

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ServerThread(int pPort, Peer peer, Map<String, ChatRoom> chatRooms) {
        this.pPort = pPort;
        this.peer = peer;
        this.chatRooms = chatRooms;
    }

    @Override
    public void run() {
        System.out.printf("\nlistening on port %d\n", pPort);
        System.out.print(">");
        try {
            serverSocket = new ServerSocket(pPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                Socket socket = serverSocket.accept(); //a new connection request
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); // set encoding as UTF8
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                User user = new User(socket.getRemoteSocketAddress()+"", null,bw);
                new ServerConnThread(socket,br, this, user).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleHostChange(HostChange hostChange, User user) {
        String host = hostChange.getContent();
        //TODO
    }


    public void handleJoin(Join join, User user) throws IOException {

            String roomidToJoin = join.getRoomid();
            if (roomidToJoin.equals("")) {
                ChatRoom roomToLeave = user.getCurrentRoom();
                synchronized (chatRooms) {
                    if (null != roomToLeave && chatRooms.containsKey(roomToLeave.getRoomId())) {
                        roomToLeave.getMembers().remove(user);
                        user.setCurrentRoom(null);
                        System.out.println("JOIN REMOTE: leaved room " + roomToLeave.getRoomId());//todo
                        String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), user.getCurrentRoom().getRoomId(), ""));
                        user.sendMsg(msg);
                    }
                }
            } else {

            }

    }
}
