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
import java.util.*;

public class ServerThread extends Thread {
    private ObjectMapper mapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private int pPort;
    private List<User> users;
    private final Map<String, ChatRoom> chatRooms;
    private Peer peer;

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ServerThread(Peer peer, int pPort, Map<String, ChatRoom> chatRooms, List<User> users) {
        this.peer = peer;
        this.pPort = pPort;
        this.chatRooms = chatRooms;
        this.users = users;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(pPort);
            while (true) {
                Socket socket = serverSocket.accept(); //a new connection request
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); // set encoding as UTF8
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                int userOutgoingPort = socket.getPort();
                String userIp = socket.getInetAddress().toString().split("/")[1];
                User user = new User(userIp + ":" + userOutgoingPort, null, null, bw);
//                System.out.println(System.lineSeparator() + "Connected user id: " + user.getUserId());
//                System.out.println("socket.getRemoteSocketAddress():" + socket.getRemoteSocketAddress());
//                System.out.println("socket.getLocalSocketAddress()" + socket.getLocalSocketAddress());
                users.add(user);
                ServerConnThread serverConnThread = new ServerConnThread(socket, br, this, user);
                serverConnThread.start();
                user.setServerConnThread(serverConnThread);
                user.setSocket(socket);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleHostChange(HostChange hostChange, User user) {
        String host = hostChange.getHost();
        user.setAddress(host);
        System.out.println(user.getUserId());
        System.out.println(user.getAddress());
    }

    public void handleMsg(MessageC2S messageC2S, User user) throws IOException {
        String msgContent = messageC2S.getContent();
        ChatRoom currentRoom = user.getCurrentRoom();
        if (currentRoom == null) {
            // Ignore any msg sent when user is in no room.
            return;
        }
        assert msgContent != null;
        assert user.getUserId() != null;
        MessageS2C messageS2C = new MessageS2C(user.getUserId(), msgContent);

        sendMsgToEveryoneInARoom(messageS2C, user);

    }

    private void sendMsgToEveryoneInARoom(MessageS2C messageS2C, User sender) throws IOException {
        ChatRoom currentRoom = sender.getCurrentRoom();
        List<User> members = currentRoom.getMembers();
        for (User member : members) {
            if (member.getBw() == null) {
                /* if a member has bw == null, it must be the owner.
                 *  The owner is using this server thread right here.
                 *  So, just print the msg to the console.
                 * */
                String content = messageS2C.getContent();
                System.out.println(sender.getUserId() + " : " + content);
                continue;
            }
            String msg = mapper.writeValueAsString(messageS2C);
            member.sendMsg(msg);
        }
    }

    public void handleJoin(Join join, User user) throws IOException {
        String roomidToJoin = join.getRoomid();
        if (roomidToJoin.equals("")) { //直接#join没有参数的情况，是要退出房间但保持连接。
            joinToLeave(user);
        } else { //#join roomid，根据得到的roomid进行判断并调动房间。
            joinToJoin(user, roomidToJoin);
        }
    }

    private void joinToLeave(User user) throws IOException {
        ChatRoom roomToLeave = user.getCurrentRoom();
        RoomChange roomChange;
        if (null != roomToLeave) { // "current room" -> ""
            roomToLeave.getMembers().remove(user);
            user.setCurrentRoom(null);
            roomChange = new RoomChange(user.getUserId(), roomToLeave.getRoomId(), "");
            sendRoomchangeToEveryoneInARoom(roomToLeave,roomChange,user);
        } else { // "" -> ""
            roomChange = new RoomChange(user.getUserId(), "", "");
        }
        String msg = mapper.writeValueAsString(roomChange);
        user.sendMsg(msg);
    }

    private void joinToJoin(User user, String roomidToJoin) throws IOException {
        ChatRoom currentRoom = user.getCurrentRoom();
        RoomChange roomChange;
        if (chatRooms.containsKey(roomidToJoin)) { // The requested room exists.
            if (null != currentRoom) { // current room is not null
                if (currentRoom.getRoomId().equals(roomidToJoin)) { // "current room" -> "current room"
                    roomChange = new RoomChange(user.getUserId(), user.getCurrentRoom().getRoomId(), user.getCurrentRoom().getRoomId());
                    String msg = mapper.writeValueAsString(roomChange);
                    user.sendMsg(msg);
                } else { //"current room" -> "room to join"
                    currentRoom.getMembers().remove(user);
                    ChatRoom roomToJ = chatRooms.get(roomidToJoin);
                    roomToJ.getMembers().add(user);
                    user.setCurrentRoom(roomToJ);
                    roomChange = new RoomChange(user.getUserId(), currentRoom.getRoomId(), roomidToJoin);
                    sendRoomchangeToEveryoneInARoom(currentRoom,roomChange,user);
                    sendRoomchangeToEveryoneInARoom(roomToJ,roomChange,user);
                }
            } else { // "" -> "room to join"
                ChatRoom roomToJ = chatRooms.get(roomidToJoin);
                roomToJ.getMembers().add(user);
                user.setCurrentRoom(roomToJ);
                roomChange = new RoomChange(user.getUserId(), "", roomidToJoin);
                sendRoomchangeToEveryoneInARoom(roomToJ,roomChange,user);
            }
        } else { //The requested room does not exist.
            if (null != currentRoom) { // "current room" -> "current room"
                roomChange = new RoomChange(user.getUserId(), currentRoom.getRoomId(), currentRoom.getRoomId());
                String msg = mapper.writeValueAsString(roomChange);
                user.sendMsg(msg);
            } else { // "" -> ""
                roomChange = new RoomChange(user.getUserId(), "", "");
                String msg = mapper.writeValueAsString(roomChange);
                user.sendMsg(msg);
            }
        }

    }

    private void sendRoomchangeToEveryoneInARoom(ChatRoom destination, RoomChange roomchange, User user) throws IOException {
        List<User> members = destination.getMembers();
        for (User member : members) {
            if (member.getBw() == null) {
                String id = roomchange.getIdentity();
                String former = roomchange.getFormer();
                String roomid = roomchange.getRoomid();
                System.out.println(
                        id + " moved from " + (former.equals("") ? "\"\"": former)
                                + " to " + (roomid.equals("") ? "\"\"" : roomid)
                );
                continue;
            }
            String msg = mapper.writeValueAsString(roomchange);
            member.sendMsg(msg);
        }
    }

    public void handleWho(Who who, User user) throws IOException {
        String roomToCount = who.getRoomid();
        if (chatRooms.containsKey(roomToCount)) {
            List<String> members = new ArrayList<>();
            ChatRoom roomToC = chatRooms.get(roomToCount);
            for (User member : roomToC.getMembers()) {
                members.add(member.getUserId());
            }
            String msg = mapper.writeValueAsString(new RoomContents(roomToCount, members));
            user.sendMsg(msg);
        } else {
            System.err.println("HANDLE WHO: no room matches the who request");
        }
    }

    public void handleList(User user) throws IOException {
        java.util.List rooms = new ArrayList<>();
        for (ChatRoom chatRoom : chatRooms.values()) {
            String roomId = chatRoom.getRoomId();
            int count = chatRoom.getMembers().size();
            Room room = new Room(roomId, count);
            rooms.add(room);
        }
        String msg = mapper.writeValueAsString(new RoomList(rooms));
        user.sendMsg(msg);
    }

    public void handleQuit(User user) throws IOException {
        ChatRoom currentRoom = user.getCurrentRoom();
        if (null != currentRoom) {
            currentRoom.getMembers().remove(user);
            user.setCurrentRoom(null);
            users.remove(user);
            RoomChange roomChange = new RoomChange(user.getUserId(), currentRoom.getRoomId(), "");
            String msg = mapper.writeValueAsString(roomChange);
            user.sendMsg(msg);
            sendRoomchangeToEveryoneInARoom(currentRoom,roomChange,user);
            user.getServerConnThread().setQuitFlag(true);
        } else {
            users.remove(user);
            String msg = mapper.writeValueAsString(new RoomChange(user.getUserId(), "", ""));
            user.sendMsg(msg);
            user.getServerConnThread().setQuitFlag(true);
        }
    }

    public void handleListNeighbour(User currUser) throws IOException {
        Set<String> identities = new HashSet<>();
        // add the users
        for (User user: users) {
            System.out.println("user:");
            System.out.println(user.getUserId());
            System.out.println(user.getAddress());
            System.out.println("curr:");
            System.out.println(currUser.getUserId());
            System.out.println(currUser.getAddress());
            if (!user.getAddress().equals(currUser.getAddress())) {
                identities.add(user.getAddress());
            }
        }

        Address connectingAdd = getConnectingAddress();
        System.out.println("connectingAddress: ");
        System.out.println(connectingAdd);
        if (connectingAdd != null) {
            identities.add(connectingAdd.toString());
        }
        String msg = mapper.writeValueAsString(new Neighbors(new ArrayList<>(identities)));
        currUser.sendMsg(msg);
    }

    /********************** helper functions **********************/
    public List<User> getUsers() {
        return users;
    }

    /*
     * Get the address of the sever if connecting as client
     * return null if not connected
     */
    public Address getConnectingAddress() {
         if (peer.getConnected()) {
             return peer.getConnectingAddress();
         } else {
             return null;
         }

    }


}