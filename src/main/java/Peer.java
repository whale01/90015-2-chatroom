import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import protocal.Commands;
import protocal.c2s.*;
import protocal.c2s.List;
import protocal.s2c.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class Peer {

    /**
     * pPort & iPort for storing command line args.
     * pPort is the user-specified listening port of the peer server (by default 4444)
     * iPort is the user-specified outgoing port of the peer client (by default 0, indicating random when passed to socket)
     */
    @Option(name = "-p")
    private int pPort = 4444;

    @Option(name = "-i")
    private int iPort = 0;

    private static Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;
    private final Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<String, ChatRoom>();
    private final java.util.List<User> users = new CopyOnWriteArrayList<User>();
    private User self;

    private static Boolean connected = false;
    private Boolean quitFlag = false;

    private ServerThread server;

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectMapper getMapper() {
        return mapper;
    }

    public static void main(String[] args) {
        Peer peer = new Peer();
        peer.parseArgs(args);
        try {
            peer.act();
        }  catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        finally {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                System.err.println("main: Failed to close socket");
//            }
//        }
    }

    private void parseArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            // check format of ports
            if(isValidPort(pPort)){
                if(iPort != 0){ //iPort specified
                    if(isValidPort(iPort)){
                        // do nothing
                    } else {
                        throw new CmdLineException(parser,"Port format invalid");
                    }
                } else { //iPort not specified
                    // do nothing
                }
            }else{ //pPort invalid
                throw new CmdLineException(parser,"Port format invalid");
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Use it like this: java -jar chatpeer.jar [-p port] [-i port]");
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        }
    }

    private void act() throws IOException, InterruptedException, SocketException {
        self = new User("localhost:"+pPort,null,null); // when unconnected, the user act as the owner of the peer
        server = new ServerThread(pPort, chatRooms,users);
        server.start();
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String line = sc.nextLine();
            if (line.startsWith("#")) { // command
                String[] splitLine = line.strip().split(" ");
                String command = splitLine[0].substring(1); // get the actual command
                switch (command) {
                    case Commands.CONNECT:
                        connect(splitLine);
                        break;
                    case Commands.CREATEROOM:
                        if(connected){
                            System.err.println("CREATEROOM: Invalid when connected");
                            break;
                        }
                        createRoom(splitLine);
                        break;
                    case Commands.KICK:
                        if(connected){
                            System.err.println("KICK: Invalid when connected");
                            break;
                        }
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
                    case Commands.DELETE:
                        if(connected){
                            System.err.println("DELETE: Invalid when connected");
                            break;
                        }
                        delete(splitLine);
                        break;
                    case Commands.LIST:
                        if(connected){
                            listRemote(splitLine);
                        }else {
                            listLocal(splitLine);
                        }
                        break;
                    case Commands.QUIT:
                        if(connected){
                            quitRemote(splitLine);
                        }else{
                            quitLocal(splitLine);
                        }
                        break;
                    case Commands.LISTNEIGHBORS:
                        if (connected) {
                            listNeighbour();
                        }
                        break;
                    case Commands.SEARCHNETWORK:
                        break;
                    default:
                        System.out.println("INVALID COMMAND!");
                }
            }
            else{ //it's a msg, if not a command
                if(connected){
                    msgRemote(line);
                }
                else{
                    msgLocal(line);
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
        if(socket != null){
            System.err.println("CONNECT: Already have a TCP connection");
            return;
        }

        try {
            if (splitLine.length == 2 || splitLine.length == 3) {
                String[] splitArg = splitLine[1].split(":");
                if (splitArg.length != 2) { // ensure the first arg is have both address and port
                    System.err.println("CONNECT: Format wrong - address:listenport sourceport");
                    return;
                }
                String remoteAddress = splitArg[0];
                String remotePort = splitArg[1];
                switch (splitLine.length) {
                    case (2): // iPort not specified
                        if (!(isValidIP(remoteAddress) && isValidPort(remotePort))) {
                            System.err.println("CONNECT: Format wrong - address:listenport sourceport");
                            return;
                        }
                        //iPort is 0 by default, meaning random outgoing port
                        socket = new Socket(remoteAddress, Integer.parseInt(remotePort), InetAddress.getLocalHost(), iPort);
                        break;
                    case (3): // iPort specified
                        String outgoingPort = splitLine[2];
                        if (!(isValidPort(outgoingPort) && isValidIP(remoteAddress) && isValidPort(remotePort))) {
                            System.err.println("CONNECT: Format wrong - address:listenport sourceport");
                            return;
                        }
                        iPort = Integer.parseInt(outgoingPort);
                        socket = new Socket(remoteAddress, Integer.parseInt(remotePort), InetAddress.getLocalHost(), iPort);
                        break;
                }
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String localIP = InetAddress.getLocalHost().toString().split("/")[1];
                HostChange hostChange = new HostChange(localIP + ":" + pPort);
                bw.write(mapper.writeValueAsString(hostChange) + System.lineSeparator());
                bw.flush();
                ClientConnThread clientConnThread = new ClientConnThread(socket, br, this);
                clientConnThread.start();
                connected = true;
                self.setUserId(localIP + ":" + socket.getLocalPort());
            } else {
                System.err.println("CONNECT: Wrong number of args");
            }
        }
        catch (SocketException e) {
            System.out.println("Cannot reach server.");
            connected = false;
        }
    }

    private void msgLocal(String line) throws IOException {
        ChatRoom currentRoom = self.getCurrentRoom();
        if(null != currentRoom){
            java.util.List<User> members = currentRoom.getMembers();
            String msg = mapper.writeValueAsString(new MessageS2C(self.getUserId(), line));
            for (User member : members) {
                if(member.getBw() == null){
                    continue;
                }
                member.sendMsg(msg);
            }

            System.out.println(self.getUserId() + " : " + line); //self msging
        }
    }

    /**
     * Send msg as a connected client.
     */
    private void msgRemote(String line) throws IOException {
        try {
            String msg = mapper.writeValueAsString(new MessageC2S(line));
            bw.write(msg + System.lineSeparator());
            bw.flush();
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }
    }

    /**
     * local local command
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

    /**
     * local local command
     */
    private void delete(String[] splitLine) {
        if(splitLine.length == 2){
            String roomToDelete = splitLine[1];
            if(chatRooms.containsKey(roomToDelete)){
                ChatRoom roomToD = chatRooms.get(roomToDelete);
                java.util.List<User> members = roomToD.getMembers();
                for (User member : members) {
                    member.setCurrentRoom(null);
                }
                chatRooms.remove(roomToDelete);
                System.out.println("DELETE: Room " + roomToDelete + " deleted.");
            } else {
                System.err.println("DELETE: No such room to delete");
            }
        } else {
            System.err.println("DELETE: Wrong number of args");
        }
    }

    /**
     * local local command
     */
    private void kick(String[] splitLine) throws IOException {
        if(splitLine.length == 2){
            String userToKick = splitLine[1];
            java.util.List<User> users = server.getUsers();
            for (User user : users) {
                if (user.getUserId().equals(userToKick)) {
                    if (null != user.getCurrentRoom()) {
                        java.util.List<User> members = user.getCurrentRoom().getMembers();
                        members.remove(user);
                        user.setCurrentRoom(null);
                    }
                    user.getServerConnThread().setQuitFlag(true); // close the server conn for this user
                    user.getSocket().close();
                    users.remove(user);
                }
            }
        } else {
            System.err.println("KICK: Wrong number of args.");
        }
    }

    /**
     * local local command
     * sprint help guide, with some different colors of course.
     * ( cannot show on windows terminal )
     */
    private void help(String[] splitLine) {
        String help =
                    "GENERAL COMMANDS: " + System.lineSeparator() +
                            Commands.ANSI_BLUE +
                    "#help - list this info" + System.lineSeparator() +
                    "#connect IP[:remote listening port] [local source port] - connect to another peer" + System.lineSeparator() +
                    "#quit - disconnect if connected to a peer" + System.lineSeparator() +
                    "#join [roomid] - join a room, or leave the current room but stay connected if no roomid provided" + System.lineSeparator() +
                    "#who roomid - see who's in a room" + System.lineSeparator() +
                    "#list - see the current server's list of rooms and head count in each room" + System.lineSeparator() +
                    "#listneighbors - see a list of neighbor peers" + System.lineSeparator() +
                    "#searchnetwork - see a list of rooms and info for each room on the network" + System.lineSeparator() +
                            Commands.ANSI_RESET +
                    "LOCAL ONLY COMMANDS: " + System.lineSeparator() +
                            Commands.ANSI_BLUE +
                    "#createroom roomid - create a room with a name locally" + System.lineSeparator() +
                    "#delete roomid - delete a room and send members back to the void" + System.lineSeparator() +
                    "#kick id - kick a user and block the user's ip from reconnecting" +
                            Commands.ANSI_RESET;
        System.out.println(help);
    }

    private void sendRoomchangeToEveryoneInARoom(ChatRoom destination, RoomChange roomchange) throws IOException {
        java.util.List<User> members = destination.getMembers();
        for (User member : members) {
            if (member.getBw() == null) {
                String id = roomchange.getIdentity();
                String former = roomchange.getFormer();
                String roomid = roomchange.getRoomid();
                System.out.println(
                        id + " moved from " + (former == null ? "\"\"": former)
                                + " to " + (roomid == null ? "\"\"" : roomid)
                );
                continue;
            }
            String msg = mapper.writeValueAsString(roomchange);
            member.sendMsg(msg);
        }
    }

    /**
     * 本地命令。
     * 没连接的状态下，
     * 1.加入本地的某房间（自己创建的）
     * 2.或者离开当前房间（设置为null）。
     */
    private void joinLocal(String[] splitLine) throws IOException {
        if(splitLine.length == 1){
            //#join "": leaving whatever room and stay connected(in this case just leaving whatever room).
            ChatRoom roomToLeave = self.getCurrentRoom();
            if(null != roomToLeave && chatRooms.containsKey(roomToLeave.getRoomId())){
                roomToLeave.getMembers().remove(self);
                self.setCurrentRoom(null);
                RoomChange roomChange = new RoomChange(self.getUserId(), roomToLeave.getRoomId(), "");
                sendRoomchangeToEveryoneInARoom(roomToLeave,roomChange);
            }
            else{
                System.err.println("Failed to leave current room.");
            }
        }
        else if(splitLine.length == 2){
            String roomToJoin = splitLine[1];
            ChatRoom currentRoom = self.getCurrentRoom();
            if(chatRooms.containsKey(roomToJoin)){
                ChatRoom roomToJ = chatRooms.get(roomToJoin);
                if(null != currentRoom){
                    if(currentRoom.getRoomId().equals(roomToJoin)){
                        System.err.println("Already in this room.");
                        return;
                    }
                    RoomChange roomChange = new RoomChange(self.getUserId(), currentRoom.getRoomId(), roomToJoin);
                    sendRoomchangeToEveryoneInARoom(currentRoom,roomChange);
                    sendRoomchangeToEveryoneInARoom(roomToJ,roomChange);
                    currentRoom.getMembers().remove(self);
                    self.setCurrentRoom(roomToJ);
                    roomToJ.getMembers().add(self);
                }
                else{ // current room is null
                    RoomChange roomChange = new RoomChange(self.getUserId(), "", roomToJoin);
                    sendRoomchangeToEveryoneInARoom(roomToJ,roomChange);
                    roomToJ.getMembers().add(self);
                    self.setCurrentRoom(roomToJ);
                    System.out.println(self.getUserId() + " moved to " + roomToJoin);
                }
            }else{
                System.err.println("The requested room is invalid or non existent.");
            }
        }else{
            System.err.println("JOIN: Wrong number of args, use #help to learn");
        }
    }

    /**
     * 已连接的状态下，向连到的server发送消息以加入server端指定房间。
     */
    private void joinRemote(String[] splitLine) throws IOException {
        try {
            if (splitLine.length == 1) {
                Join join = new Join("");
                bw.write(mapper.writeValueAsString(join) + System.lineSeparator()); //使用bw需要在消息后面加上newline才能被br.readLine()读到
                bw.flush();
            } else if (splitLine.length == 2) {
                String roomToJoin = splitLine[1];
                Join join = new Join(roomToJoin);
                bw.write(mapper.writeValueAsString(join) + System.lineSeparator());
                bw.flush();
            } else {
                System.err.println("JOIN REMOTE: Wrong number of args");
            }
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }

    }

    /**
     * 本地命令。
     * 返回本地某房间内的人员。
     */
    private void whoLocal(String[] splitLine) throws JsonProcessingException {
        if(splitLine.length == 2){
            String roomToCount = splitLine[1];
            if(chatRooms.containsKey(roomToCount)){
                java.util.List members = new ArrayList<>();
                synchronized (chatRooms) {
                    ChatRoom roomToC = chatRooms.get(roomToCount);
                    for (User user : roomToC.getMembers()) {
                        members.add(user.getUserId());
                    }
                }
                String s = mapper.writeValueAsString(new RoomContents(roomToCount, members));
                System.out.println(s);
            }else{
                System.err.println("WHO LOCAL: No existing room as per requested");
            }
        }
        else{
            System.err.println("WHO LOCAL: Wrong number of args");
        }
    }


    private void whoRemote(String[] splitLine) throws IOException {
        try {
            if (splitLine.length == 2) {
                String roomToAsk = splitLine[1];
                Who who = new Who(roomToAsk);
                bw.write(mapper.writeValueAsString(who) + System.lineSeparator());
                bw.flush();
            } else {
                System.err.println("WHO REMOTE: Wrong number of args");
            }
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }
    }

    /**
     * 本地命令。
     * 本地的quit命令只是把自己从当前房间退出
     * 所以直接沿用joinLocal方法。
     */
    private void quitLocal(String[] splitLine) throws IOException {
        if(splitLine.length == 1){
            joinLocal(splitLine);
        }
        else{
            System.err.println("QUIT LOCAL: Wrong number of args");
        }
    }

    /**
     * 远程quit
     * 发送quit消息
     * @param splitLine
     */
    private void quitRemote(String[] splitLine) throws IOException {
        try {
            if (splitLine.length == 1) {
                String msg = mapper.writeValueAsString(new Quit());
                bw.write(msg + System.lineSeparator());
                bw.flush();
                quitFlag = true;
            } else {
                System.err.println("QUIT REMOTE: No args needed");
            }
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }
    }

    /**
     * 本地命令。
     */
    private void listLocal(String[] splitLine) throws JsonProcessingException {
        if(splitLine.length == 1){
            java.util.List rooms = new ArrayList<>();
            synchronized (chatRooms) {
                for (ChatRoom chatRoom: chatRooms.values()) {
                    String roomId = chatRoom.getRoomId();
                    int count = chatRoom.getMembers().size();
                    Room room = new Room(roomId,count);
                    rooms.add(room);
                }
            }
            String msg = mapper.writeValueAsString(new RoomList(rooms));
            System.out.println(msg);
        }else{
            System.err.println("LIST LOCAL: No args needed for #list");
        }
    }

    private void listRemote(String[] splitLine) throws IOException {
        try {

            if (splitLine.length == 1) {
                String msg = mapper.writeValueAsString(new List());
                bw.write(msg + System.lineSeparator());
                bw.flush();
            } else {
                System.err.println("LIST REMOTE: No args needed for #list");
            }
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }
    }

    /**
     * Send out list_neighbour command
     */
    private void listNeighbour() {
        try {
            ListNeighbours listNeighbours = new ListNeighbours();
            System.out.println(mapper.writeValueAsString(listNeighbours));
            bw.write(mapper.writeValueAsString(listNeighbours) + System.lineSeparator());
            bw.flush();
        }
        catch (SocketException e){
            System.err.println("The connection has been closed.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * fetch the current room and
     */
    private void printCmdlineHeader(){

    }

    public Map<String, ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public User getSelf() {
        return self;
    }

    public Boolean getQuitFlag() {
        return quitFlag;
    }

    public void setQuitFlag(Boolean quitFlag) {
        this.quitFlag = quitFlag;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private static boolean isValidIP(String str) {
        if (str.equals("localhost")) {
            return true;
        }
        return Pattern.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$", str);
    }

    private static boolean isValidPort(int port) {
        return Pattern.matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"
                , String.valueOf(port));
    }

    private static boolean isValidPort(String port){
        int numPort;
        try{
            numPort = Integer.parseInt(port);
        }
        catch (NumberFormatException e){
            return false;
        }
        return isValidPort(numPort);
    }
}