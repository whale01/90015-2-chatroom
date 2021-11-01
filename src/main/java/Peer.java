import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import protocal.Commands;
import protocal.P2P.MigrateStart;
import protocal.P2P.MigrateUser;
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
    private Address connectingAddress;
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
            // check format of ports
            if (Address.isValidPort(pPort)) {
                if (iPort != Integer.MIN_VALUE) { //iPort specified
                    if (Address.isValidPort(iPort)) {
                        // do nothing
                    } else {
                        throw new CmdLineException(parser, "Port format invalid");
                    }
                } else { //iPort not specified
                    // do nothing
                }
            } else { //pPort invalid
                throw new CmdLineException(parser, "Port format invalid");
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
        String localIP = InetAddress.getLocalHost().toString().split("/")[1];
        Address selfAddress = new Address(localIP, pPort);
        self = new User(selfAddress.toString(), selfAddress.toString(), null, null); // when unconnected, the user act as the owner of the peer
        self.setAddress(localIP + ":" + pPort);
        server = new ServerThread(this, pPort, chatRooms, users);
        server.start();
        System.out.println(" Print the identity prefix by hitting ENTER. ");
        printCmdlineHeader();
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                String line = sc.nextLine();
                if (line.isEmpty()) {
                    printCmdlineHeader();
                    continue;
                }
                if (line.startsWith("#")) { // command
                    String[] splitLine = line.strip().split(" ");
                    String command = splitLine[0].substring(1); // get the actual command
                    switch (command) {
                        case Commands.CONNECT:
                            connect(splitLine);
                            break;
                        case Commands.CREATEROOM:
                            if (connected) {
                                System.err.println("CREATEROOM: Invalid when connected");
                                break;
                            }
                            createRoom(splitLine);
                            break;
                        case Commands.KICK:
                            if (connected) {
                                System.err.println("KICK: Invalid when connected");
                                break;
                            }
                            kick(splitLine);
                            break;
                        case Commands.HELP:
                            help(splitLine);
                            break;
                        case Commands.JOIN:
                            if (connected) {
                                joinRemote(splitLine);
                            } else {
                                joinLocal(splitLine);
                            }
                            break;
                        case Commands.WHO:
                            if (connected) {
                                whoRemote(splitLine);
                            } else {
                                whoLocal(splitLine);
                            }
                            break;
                        case Commands.DELETE:
                            if (connected) {
                                System.err.println("DELETE: Invalid when connected");
                                break;
                            }
                            delete(splitLine);
                            break;
                        case Commands.LIST:
                            if (connected) {
                                listRemote(splitLine);
                            } else {
                                listLocal(splitLine);
                            }
                            break;
                        case Commands.QUIT:
                            if (connected) {
                                quitRemote(splitLine);
                            } else {
                                quitLocal(splitLine);
                            }
                            break;
                        case Commands.LISTNEIGHBORS:
                            if (connected) {
                                listNeighbour();
                            }
                            break;
                        case Commands.SEARCHNETWORK:
                            searchNetwork();
                            break;
                        case Commands.MIGRATEROOM:
                            migrateRoom(splitLine);
                        default:
                            System.out.println("INVALID COMMAND!");
                    }
                } else { //it's a msg
                    if (connected) {
                        msgRemote(line);
                    } else {
                        msgLocal(line);
                    }
                }
//                printCmdlineHeader();
            }
        } catch (Exception e) {
            sc.close();
            System.exit(0);
        }
    }

    /**
     * 接到#connect命令后，连接到指定address:port
     * 并新建一个ClientConnThread用于监听server的消息回复
     */
    private void connect(String[] splitLine) {
        if (socket != null) {
            System.err.println(System.lineSeparator() + "CONNECT: Already have a TCP connection");
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
                // replace localhost
                if (remoteAddress.contains("localhost")) {
                    remoteAddress = InetAddress.getLocalHost().toString().split("/")[1];
                }
                int remotePort = Integer.parseInt(splitArg[1]);
                switch (splitLine.length) {
                    case (2): // iPort not specified
                        if (!(Address.isValidIP(remoteAddress) && Address.isValidPort(remotePort))) {
                            System.err.println("CONNECT: Format wrong - address:listenport sourceport");
                            return;
                        }
                        //iPort is 0 by default, meaning random outgoing port
                        socket = new Socket(remoteAddress, remotePort, InetAddress.getLocalHost(), iPort);
                        break;
                    case (3): // iPort specified
                        String outgoingPort = splitLine[2];
                        if (!(Address.isValidPort(outgoingPort) && Address.isValidIP(remoteAddress) && Address.isValidPort(remotePort))) {
                            System.err.println("CONNECT: Format wrong - address:listenport sourceport");
                            return;
                        }
                        iPort = Integer.parseInt(outgoingPort);
                        socket = new Socket(remoteAddress, remotePort, InetAddress.getLocalHost(), iPort);
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
                connectingAddress = new Address(remoteAddress, remotePort);
                self.setUserId(localIP + ":" + socket.getLocalPort());
            } else {
                System.err.println("CONNECT: Wrong number of args");
            }
        } catch (SocketException e) {
            System.err.println("Cannot reach server.");
            connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void msgLocal(String line) throws IOException {
        ChatRoom currentRoom = self.getCurrentRoom();
        if (null != currentRoom) {
            java.util.List<User> members = currentRoom.getMembers();
            String msg = mapper.writeValueAsString(new MessageS2C(self.getUserId(), line));
            for (User member : members) {
                if (member.getBw() == null) {
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
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
            connected = false;
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
            if (!StringUtils.isAlphanumeric(roomToCreate) || roomToCreate.length() < 3 || roomToCreate.length() > 32) {
                System.err.println("CREATEROOM: Invalid room name.");
                return;
            }
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
        if (splitLine.length == 2) {
            String roomToDelete = splitLine[1];
            if (chatRooms.containsKey(roomToDelete)) {
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
        if (splitLine.length == 2) {
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
        String help = System.lineSeparator() +
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
                System.out.println(System.lineSeparator() +
                        id + " moved from " + (former == null ? "\"\"" : former)
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
        if (splitLine.length == 1) {
            //#join "": leaving whatever room and stay connected(in this case just leaving whatever room).
            ChatRoom roomToLeave = self.getCurrentRoom();
            if (null != roomToLeave && chatRooms.containsKey(roomToLeave.getRoomId())) {
                roomToLeave.getMembers().remove(self);
                self.setCurrentRoom(null);
                RoomChange roomChange = new RoomChange(self.getUserId(), roomToLeave.getRoomId(), "");
                sendRoomchangeToEveryoneInARoom(roomToLeave, roomChange);
            } else {
                System.err.println("Failed to leave current room.");
            }
        } else if (splitLine.length == 2) {
            String roomToJoin = splitLine[1];
            ChatRoom currentRoom = self.getCurrentRoom();
            if (chatRooms.containsKey(roomToJoin)) {
                ChatRoom roomToJ = chatRooms.get(roomToJoin);
                if (null != currentRoom) {
                    if (currentRoom.getRoomId().equals(roomToJoin)) {
                        System.err.println(System.lineSeparator() + "Already in this room.");
                        return;
                    }
                    RoomChange roomChange = new RoomChange(self.getUserId(), currentRoom.getRoomId(), roomToJoin);
                    sendRoomchangeToEveryoneInARoom(currentRoom, roomChange);
                    sendRoomchangeToEveryoneInARoom(roomToJ, roomChange);
                    currentRoom.getMembers().remove(self);
                    self.setCurrentRoom(roomToJ);
                    roomToJ.getMembers().add(self);
                } else { // current room is null
                    RoomChange roomChange = new RoomChange(self.getUserId(), "", roomToJoin);
                    sendRoomchangeToEveryoneInARoom(roomToJ, roomChange);
                    roomToJ.getMembers().add(self);
                    self.setCurrentRoom(roomToJ);
                    System.out.println(self.getUserId() + " moved to " + roomToJoin);
                }
            } else {
                System.err.println("The requested room is invalid or non existent.");
            }
        } else {
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
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
            connected = false;
        }
    }

    /**
     * 本地命令。
     * 返回本地某房间内的人员。
     */
    private void whoLocal(String[] splitLine) throws JsonProcessingException {
        if (splitLine.length == 2) {
            String roomToCount = splitLine[1];
            if (chatRooms.containsKey(roomToCount)) {
                java.util.List<String> members = new CopyOnWriteArrayList<String>();
                ChatRoom roomToC = chatRooms.get(roomToCount);
                for (User user : roomToC.getMembers()) {
                    members.add(user.getUserId());
                }
                System.out.print(System.lineSeparator() + roomToCount + " contains ");
                for (String member : members) {
                    System.out.print(member + " ");
                }
                System.out.println();
            } else {
                System.err.println("WHO LOCAL: No existing room as per requested");
            }
        } else {
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
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
            connected = false;
        }
    }

    /**
     * 本地命令。
     * 本地的quit命令只是把自己从当前房间退出
     * 所以直接沿用joinLocal方法。
     */
    private void quitLocal(String[] splitLine) throws IOException {
        if (splitLine.length == 1) {
            joinLocal(splitLine);
        } else {
            System.err.println("QUIT LOCAL: Wrong number of args");
        }
    }

    /**
     * 远程quit
     * 发送quit消息
     *
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
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
            connected = false;
        }
    }

    /**
     * 本地命令。
     */
    private void listLocal(String[] splitLine) throws JsonProcessingException {
        if (splitLine.length == 1) {
            java.util.List<Room> rooms = new CopyOnWriteArrayList<Room>();

            for (ChatRoom chatRoom : chatRooms.values()) {
                String roomId = chatRoom.getRoomId();
                int count = chatRoom.getMembers().size();
                Room room = new Room(roomId, count);
                rooms.add(room);
            }
            for (Room room : rooms) {
                String roomid = room.getRoomid();
                int count = room.getCount();
                if (count == 1 || count == 0) {
                    System.out.println(roomid + ": " + count + " guest");
                } else {
                    System.out.println(roomid + ": " + count + " guests");
                }
            }
        } else {
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
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
            connected = false;
        }
    }

    /**
     * Send out list_neighbour command
     */
    private void listNeighbour() {
        try {
            ListNeighbours listNeighbours = new ListNeighbours();
            bw.write(mapper.writeValueAsString(listNeighbours) + System.lineSeparator());
            bw.flush();
        } catch (SocketException e) {
            System.err.println("The connection has been closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * fetch the current room and
     */
    private void printCmdlineHeader() {
        ChatRoom currentRoom = self.getCurrentRoom();
        String currentRoomStr;
        String idStr;
        if (null != currentRoom) {
            currentRoomStr = currentRoom.getRoomId();
        } else {
            currentRoomStr = "";
        }
        if (connected) {
            idStr = self.getUserId();
        } else {
            idStr = self.getAddress();
        }
        System.out.printf("[%s]  %s> ", currentRoomStr, idStr);
    }

    public void searchNetwork() throws IOException {
        HashMap<String, java.util.List<Room>> result = search();
        for (String address : result.keySet()) {
            System.out.println(address);
            java.util.List<Room> rooms = result.get(address);
            for (Room room : rooms) {
                System.out.println(room.getRoomid() + ": " + room.getCount());
            }
        }
    }

    /**
     * Search the network with listneighbors and list
     */
    public HashMap<String, java.util.List<Room>> search() {
        HashMap<String, java.util.List<Room>> result = new HashMap<>();
        Stack<String> toSearch = new Stack<>();
        Set<String> visited = new HashSet<>();
        try {
            // if connecting as client, start with the server
            if (connected) {
                toSearch.add(connectingAddress.toString());
            } else {
                // start with the connections I have
                for (User user : server.getUsers()) {
                    Address clientAdd = new Address(user.getAddress());
                    if (!toSearch.contains(clientAdd)) {
                        toSearch.add(clientAdd.toString());
                    }
                }
            }
            if (toSearch.size() <= 0) {
                System.out.println("No adjacent peers to search.");
            }

            visited.add(self.getAddress());
            while (toSearch.size() > 0) {
                // connect to the address using a different socket
                Address currentAddress = new Address(toSearch.pop());
                Socket lsocket = new Socket(currentAddress.getIP(), currentAddress.getPort());
                BufferedWriter lbw = new BufferedWriter(new OutputStreamWriter(lsocket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader lbr = new BufferedReader(new InputStreamReader(lsocket.getInputStream(), StandardCharsets.UTF_8));
                // send a hostchange with the IP and listening port of this peer
                HostChange hostChange = new HostChange(self.getAddress());
                lbw.write(mapper.writeValueAsString(hostChange) + System.lineSeparator());
                lbw.flush();

                // use list_neighbors to find out the neighbors
                ListNeighbours listNeighbours = new ListNeighbours();
                lbw.write(mapper.writeValueAsString(listNeighbours) + System.lineSeparator());
                lbw.flush();
                String neighborList = lbr.readLine();
                Neighbors neighbors = mapper.readValue(neighborList, Neighbors.class);
                java.util.List<String> ids = neighbors.getNeighbors();
                for (String id : ids) {
                    if (!visited.contains(id)) {
                        toSearch.add(id);
                    }
                }
                visited.add(currentAddress.toString());

                // use list to find out its rooms
                List list = new List();
                lbw.write(mapper.writeValueAsString(list) + System.lineSeparator());
                lbw.flush();
                // read the response
                String roomList = lbr.readLine();
                RoomList rooms = mapper.readValue(roomList, RoomList.class);
                result.put(currentAddress.toString(), rooms.getRooms());

                // user sends a quit packet
                Quit quit = new Quit();
                lbw.write(mapper.writeValueAsString(quit) + System.lineSeparator());
                lbw.flush();
                // consumer the roomchange message and close the connection
                lbr.readLine();
                lsocket.close();
            }
            // remove address of self
            result.remove(self.getAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void migrateRoom(String[] splitLine) {
        if (splitLine.length != 2) {
            System.out.println("INVALID COMMAND: wrong number of arguments!");
            return;
        }
        String roomID = splitLine[1];
        migrate(roomID);
    }

    /*
     * Migrate a room to another peer
     */
    public void migrate(String roomID) {
        System.out.println("migrate");

        // 2 modes:
        // - provide ID, migrate a single room
        // - no ID, migrate all rooms
        Stack<ChatRoom> toMigrate = new Stack<ChatRoom>();
        if (roomID != null) {
            toMigrate.add(chatRooms.get(roomID));
        } else {
            toMigrate.addAll(chatRooms.values());
        }
        if (toMigrate.size() <= 0) {
            System.out.println("No room to migrate!");
            return;
        }

        // find out where to migrate to
        Stack<String> availableTargets = new Stack<>();
        availableTargets.addAll(search().keySet());
        System.out.println(availableTargets);
        if (availableTargets.size() == 0) {
            System.out.println("No target to migrate to.");
        }

        Address target;
        boolean success = false;
        try {
            while (!success && availableTargets.size() >= 1) {
                target = new Address(availableTargets.pop());
                // if target is not valid, go to next one
                if (!target.isValidAddress()) {
                    continue;
                }
                System.out.println("target: " + target);
                Socket lsocket = new Socket(target.getIP(), target.getPort());
                BufferedReader lbr = new BufferedReader(new InputStreamReader(lsocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter lbw = new BufferedWriter(new OutputStreamWriter(lsocket.getOutputStream(), StandardCharsets.UTF_8));

                Boolean skip = false;
                // loop through all chatrooms to migrate
                for (ChatRoom room : toMigrate) {
                    System.out.println("Migrating " + room.getRoomId());
                    // migrate start
                    MigrateStart start = new MigrateStart(room.getRoomId(), room.getMembers().size());
                    lbw.write(mapper.writeValueAsString(start) + System.lineSeparator());
                    lbw.flush();
                    // check if receiving has started
                    String line = lbr.readLine();
                    System.out.println(line);
                    JsonNode jsonNode = mapper.readTree(line);
                    String type = jsonNode.get("type").asText();
                    // something went wrong, go to next target
                    if (!type.equals("receivestart")) {
                        if (type.equals("migratefail")) {
                            System.out.println("Target already have room of same ID, go to next.");
                        }
                        continue;
                    }

                    for (User user : room.getMembers()) {
                        // do not move self
                        if (user.getUserId().equals(self.getUserId())) {
                            break;
                        }
                        System.out.println("move user: " + user.getAddress());
                        // tell the user to migrate
                        MigrateUser migrateUser = new MigrateUser(target.toString(), roomID);
                        user.sendMsg(mapper.writeValueAsString(migrateUser));
                        MessageS2C msg = new MessageS2C("", "hello");
                        user.sendMsg(mapper.writeValueAsString(msg));
                        // get the result of user migration
//                        line = br.readLine();
//                        System.out.println(line);
//                        jsonNode = mapper.readTree(line);
//                        type = jsonNode.get("type").asText();
//                        if (!type.equals("moveusersuccess")) {
//                            skip = true;
//                            break;
//                        }
                    }
                }

//                // get the overall result
//                String line = lbr.readLine();
//                System.out.println(line);

//                // user sends a quit packet
//                Quit quit = new Quit();
//                lbw.write(mapper.writeValueAsString(quit) + System.lineSeparator());
//                lbw.flush();
//                // consumer the roomchange message and close the connection
//                lbr.readLine();
//                lsocket.close();
            }

            if (success) {
                System.out.println("Migrate success.");
            } else {
                System.out.println("Migrate failed.");
            }
        } catch (Exception e) {
            System.out.println("Migrate failed.");
            e.printStackTrace();
        }
    }

    public void moveUser(String target, String roomID) {
        try {
//            BufferedWriter tempBW = new BufferedWriter(bw);
            // first, close the current connection
            String[] splitLine = {""};
            quitRemote(splitLine);
            // then connect to the target
            splitLine = new String[]{"#connect", target};
            connect(splitLine);
            // then join the room
            splitLine = new String[]{"#join", roomID};
            if (self.getAddress().equals(target)){
                System.out.println("join local");
                joinLocal(splitLine);
            } else {
                joinRemote(splitLine);
            }
//            // send moveuser success
////            MoveUserSuccess moveUserSuccess = new MoveUserSuccess();
////            tempBW.write(mapper.writeValueAsString(moveUserSuccess) + System.lineSeparator());
////            tempBW.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /***************** Helper functions ************************/

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

    public Map<String, ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public Boolean getConnected() {
        return connected;
    }

    public Address getConnectingAddress() {
        return connectingAddress;
    }


}