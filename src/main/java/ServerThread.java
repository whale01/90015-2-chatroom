import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;
import protocal.c2s.Join;
import protocal.c2s.MessageC2S;
import protocal.c2s.Who;
import protocal.s2c.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerThread extends Thread {
    private ObjectMapper mapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private int pPort;
    private ArrayList<User> users;
    private final Map<String, ChatRoom> chatRooms;

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ServerThread(int pPort, Map<String, ChatRoom> chatRooms) {
        this.pPort = pPort;
        this.chatRooms = chatRooms;
        this.users = new ArrayList<>();
    }

    @Override
    public void run() {
        System.out.printf("\nlistening on port %d\n", pPort);
        System.out.print(">");
        try {
            serverSocket = new ServerSocket(pPort);
            while (true) {
                Socket socket = serverSocket.accept(); //a new connection request
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); // set encoding as UTF8
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                User user = new User(socket.getRemoteSocketAddress() + "", null, bw);
                users.add(user);
                ServerConnThread serverConnThread = new ServerConnThread(socket, br, this, user);
                serverConnThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleHostChange(HostChange hostChange, User user) {
        String host = hostChange.getHost();
        user.setUserId(host);
        System.out.printf("%s has connected.\n", host);
    }

    public void handleMsg(MessageC2S messageC2S, User user) throws IOException {
        String msgContent = messageC2S.getContent();
        ChatRoom currentRoom = user.getCurrentRoom();
        if(currentRoom == null){
            return;
        }
        assert msgContent != null;
        assert user.getUserId() != null;
        String msg = mapper.writeValueAsString(new MessageS2C(user.getUserId(), msgContent));
        List<User> members = currentRoom.getMembers();
        for (User member : members) {
            if(member.getBw() == null){
                System.out.println(msg);
                continue;
            }
            member.sendMsg(msg);
        }

    }

    public void handleJoin(Join join, User user) throws IOException {
        String roomidToJoin = join.getRoomid();
        if (roomidToJoin.equals("")) { //直接#join没有参数的情况，是要退出房间但保持连接。
            ChatRoom roomToLeave = user.getCurrentRoom();
            synchronized (chatRooms) {
                if (null != roomToLeave && chatRooms.containsKey(roomToLeave.getRoomId())) {
                    roomToLeave.getMembers().remove(user);
                    user.setCurrentRoom(null);
                    System.out.println("HANDLE JOIN: leaved room " + roomToLeave.getRoomId());//todo
                    String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), null != user.getCurrentRoom() ? user.getCurrentRoom().getRoomId() : "" ,""));
                    user.sendMsg(msg);
                } else {
                    System.err.println("HANDLE JOIN: failed to leave current room");
                }
            }
        } else { //#join roomid，根据得到的roomid进行判断并调动房间。
            ChatRoom currentRoom = user.getCurrentRoom();
            if (chatRooms.containsKey(roomidToJoin)) {
                if (null != currentRoom) {
                    if (currentRoom.getRoomId().equals(roomidToJoin)) {
                        String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), user.getCurrentRoom().getRoomId(), user.getCurrentRoom().getRoomId()));
                        user.sendMsg(msg);
                        return;
                    }
                    currentRoom.getMembers().remove(user);
                }
                ChatRoom roomToJ = chatRooms.get(roomidToJoin);
                roomToJ.getMembers().add(user);
                user.setCurrentRoom(roomToJ);
                String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), null != currentRoom ? currentRoom.getRoomId() : null, roomidToJoin));
                user.sendMsg(msg);
            } else { //房间列表没找到要加入的房间，房间不存在。
                if(null != user.getCurrentRoom()){
                    String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), user.getCurrentRoom().getRoomId(), user.getCurrentRoom().getRoomId()));
                    user.sendMsg(msg);
                }
            }
        }

    }

    public void handleWho(Who who, User user) throws IOException {
        String roomToCount = who.getRoomid();
        if (chatRooms.containsKey(roomToCount)) {
            List<String> members = new ArrayList<>();
            synchronized (chatRooms) {
                ChatRoom roomToC = chatRooms.get(roomToCount);
                for (User member : roomToC.getMembers()) {
                    members.add(member.getUserId());
                }
            }
            String msg = mapper.writeValueAsString(new RoomContents(roomToCount, members));
            user.sendMsg(msg);
        } else {
            System.err.println("HANDLE WHO: no room matches the who request");
        }
    }

    public void handleList(User user) throws IOException {
        java.util.List rooms = new ArrayList<>();
        synchronized (chatRooms) {
            for (ChatRoom chatRoom : chatRooms.values()) {
                String roomId = chatRoom.getRoomId();
                int count = chatRoom.getMembers().size();
                Room room = new Room(roomId, count);
                rooms.add(room);
            }
        }
        String msg = mapper.writeValueAsString(new RoomList(rooms));
        user.sendMsg(msg);
    }

    public void handleQuit(User user) throws IOException {
        ChatRoom currentRoom = user.getCurrentRoom();
        if (null != currentRoom){
            synchronized (chatRooms) {
                currentRoom.getMembers().remove(user);
            }
            user.setCurrentRoom(null);
            users.remove(user);
            String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), currentRoom.getRoomId(), ""));
            user.sendMsg(msg);
        }
        else{
            users.remove(user);
            String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), "", ""));
            user.sendMsg(msg);
        }
    }

    public void handleListNeighbour(User currUser) throws IOException {
        ArrayList<String> identities = new ArrayList<>();
        for (User user: users) {
            if (!user.getUserId().equals(currUser.getUserId())) {
                identities.add(user.getUserId());
            }
        }
        String msg = mapper.writeValueAsString(new Neighbors(identities));
        currUser.sendMsg(msg);
    }

    public ArrayList<User> getUsers() {
        return users;
    }


}