import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import protocal.Commands;
import protocal.c2s.HostChange;
import protocal.c2s.Join;
import protocal.s2c.RoomContents;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Peer {
    @Option(name = "-p", usage = "The listening (server) port - waiting for incoming connections ")
    private int pPort = 4444;

    @Option(name = "-i", usage = "The speaking (client) port - ready to make outgoing connections")
    private int iPort = Integer.MIN_VALUE;

    private Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;
    private final Map<String, ChatRoom> chatRooms = Collections.synchronizedMap(new HashMap<>());
    private User self;

    private Boolean connected = false;

    private ObjectMapper mapper = new ObjectMapper();

    public ObjectMapper getMapper() {
        return mapper;
    }

    public static void main(String[] args) {
        Peer peer = new Peer();
        peer.parseArgs(args);
        try {
            peer.act();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void parseArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Use it like this: java -jar chatpeer.jar [-p port] [-i port]");
            parser.printUsage(System.err);
            System.err.println();
        }
    }

    private void act() throws IOException, InterruptedException {
        self = new User("localhost:"+pPort,null,bw); //不会给自己发消息，bw设为Null
        ServerThread server = new ServerThread(pPort, this,chatRooms);
        server.start();
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String line = sc.nextLine();
            if (line.startsWith("#")) {
                String[] splitLine = line.strip().split(" ");
                String command = splitLine[0].substring(1); // get the actual command
                switch (command) {
                    case Commands.CONNECT:
                        connect(splitLine);
                        break;
                    case Commands.CREATEROOM:
                        createRoom(splitLine);
                        break;
                    case Commands.KICK:
                        kick(splitLine);
                        break;
                    case Commands.HELP:
                        help(splitLine);
                        break;
                    case Commands.JOIN:
                        if(connected){
                            joinRemote(splitLine);
                        }else{
                            joinLocal(splitLine);
                        }
                        break;
                    case Commands.WHO:
                        if(connected){
                            whoRemote(splitLine);
                        }else{
                            whoLocal(splitLine);
                        }
                        break;
                    case Commands.QUIT:
                        if(connected){
                            quitRemote();
                        }else{
                            quitLocal(splitLine);
                        }
                }
            }
            System.out.print(">");
        }

    }

    /**
     * 接到#connect命令后，连接到指定address:port
     * 并新建一个ClientConnThread用于监听server的消息回复
     */
    private void connect(String[] splitLine) throws IOException {
        if (splitLine.length == 2 || splitLine.length == 3) {
            String[] splitArg = splitLine[1].split(":");
            String remoteAddress = splitArg[0];
            String remotePort = splitArg[1];
            //TODO: 验证address:port格式正确性
            switch (splitLine.length) {
                case (2):
                    if (iPort == Integer.MIN_VALUE) {
                        socket = new Socket(remoteAddress, Integer.parseInt(remotePort));
                    } else {
                        socket = new Socket(remoteAddress, Integer.parseInt(remotePort), InetAddress.getLocalHost(), iPort);
                    }
                    break;
                case (3):
                    //TODO: 验证localPort格式正确性
                    iPort = Integer.parseInt(splitLine[2]);
                    socket = new Socket(remoteAddress, Integer.parseInt(remotePort), InetAddress.getLocalHost(), iPort);
                    break;
            }
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            bw.write(mapper.writeValueAsString(new HostChange(socket.getLocalAddress() + ":" + pPort)));
            bw.flush();
            new ClientConnThread(socket, br, this).start();
            connected = true;
        } else {
            System.err.println("CONNECT: Wrong number of args");
        }
    }

    /**
     * 本地命令。
     * 在本地创建房间。
     * 1. 检查是否有重复房间
     * 2. 创建。
     */
    private void createRoom(String[] splitLine) {
        if (splitLine.length == 2) {
            String roomToCreate = splitLine[1];
            //TODO: 验证roomToCreate格式
            if (!chatRooms.containsKey(roomToCreate)) {
                ChatRoom chatRoom = new ChatRoom(roomToCreate);
                chatRooms.put(roomToCreate, chatRoom);
                System.out.println("CREATE ROOM: Room " + roomToCreate + " created.");
            } else {
                System.err.println("CREATEROOM: Name occupied.");
            }
        } else {
            System.err.println("CREATEROOM: Wrong number of args");
        }
    }

    private void kick(String[] splitLine) {

    }

    private void help(String[] splitLine) {

    }

    /**
     * 本地命令。
     * 没连接的状态下，
     * 1.加入本地的某房间（自己创建的）
     * 2.或者离开当前房间。
     */
    private void joinLocal(String[] splitLine) {
        if(splitLine.length == 1){
            //#join "": leaving whatever room and stay connected(in this case just leaving whatever room).
            ChatRoom roomToLeave = self.getCurrentRoom();
            if(null != roomToLeave && chatRooms.containsKey(roomToLeave.getRoomId())){
                roomToLeave.getMembers().remove(self);
                self.setCurrentRoom(null);
                System.out.println("JOIN LOCAL: leaved room " + roomToLeave.getRoomId());
            }
            else{
                System.err.println("JOIN LOCAL: Failed to leave current room.");
            }
        }
        else if(splitLine.length == 2){
            String roomToJoin = splitLine[1];
            ChatRoom currentRoom = self.getCurrentRoom();
            if(chatRooms.containsKey(roomToJoin)){
                if(null != currentRoom){
                    if(currentRoom.getRoomId().equals(roomToJoin)){
                        System.err.println("JOIN LOCAL: Already in this room");
                        return;
                    }
                    currentRoom.getMembers().remove(self);
                }
                ChatRoom roomToJ = chatRooms.get(roomToJoin);
                roomToJ.getMembers().add(self);
                self.setCurrentRoom(roomToJ);
                System.out.println("JOIN LOCAL: Room " + roomToJoin +" joined");
            }else{
                System.err.println("JOIN LOCAL: Wrong room to join");
            }
        }else{
            System.err.println("JOIN LOCAL: Wrong number of args");
        }
    }

    /**
     * 已连接的状态下，向连到的server发送消息以加入server端指定房间。
     */
    private void joinRemote(String[] splitLine) throws IOException {
        if(splitLine.length == 1){
            Join join = new Join("");
            bw.write(mapper.writeValueAsString(join));
        }
        else if(splitLine.length == 2){
            String roomToJoin = splitLine[1];
            Join join = new Join(roomToJoin);
            bw.write(mapper.writeValueAsString(join));
        }
        else {
            System.err.println("JOIN REMOTE: Wrong number of args");
        }
        bw.flush();
    }

    /**
     * 本地命令。
     * 返回本地某房间内的人员。
     */
    private void whoLocal(String[] splitLine) {
        if(splitLine.length == 2){
            String roomToCount = splitLine[1];
            if(chatRooms.containsKey(roomToCount)){
                List<String> members = new ArrayList<>();
                synchronized (chatRooms) {
                    ChatRoom roomToC = chatRooms.get(roomToCount);
                    for (User user : roomToC.getMembers()) {
                        members.add(user.getUserId());
                    }
                }
                System.out.println(new RoomContents(roomToCount, members));
            }else{
                System.err.println("WHO LOCAL: No existing room as per requested");
            }
        }
        else{
            System.err.println("WHO LOCAL: Wrong number of args");
        }
    }


    private void whoRemote(String[] splitLine) {

    }

    /**
     * 本地命令。
     *
     */
    private void quitLocal(String[] splitLine) {
        if(splitLine.length == 1){
            joinLocal(splitLine);
        }
        else{
            System.err.println("QUIT LOCAL: Wrong number of args");
        }
    }

    private void quitRemote() {

    }


//    public class ServerThread extends Thread{
//        private ObjectMapper mapper = new ObjectMapper();
//
//        private ServerSocket serverSocket;
//        private int pPort;
//        private Peer peer;
//
//        public ObjectMapper getMapper() {
//            return mapper;
//        }
//
//        public ServerThread(int pPort, Peer peer) {
//            this.pPort = pPort;
//            this.peer = peer;
//        }
//
//        @Override
//        public void run() {
//            System.out.printf("\nlistening on port %d\n", pPort);
//            System.out.print(">");
//            try {
//                serverSocket = new ServerSocket(pPort);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            while (true){
//                try {
//                    Socket socket = serverSocket.accept(); //a new connection request
//                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); // set encoding as UTF8
//                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
//                    User user = new User(socket.getRemoteSocketAddress()+"", null);
//                    new ServerConnThread(socket,br, this, user).start();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        public void handleHostChange(HostChange hostChange, User user) {
//            String host = hostChange.getContent();
//            //TODO
//        }
//
//
//        public void handleJoin(Join join, User user) {
//
//        }
//    }
}