import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import protocal.Commands;
import protocal.c2s.HostChange;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Peer {

    @Option(name = "-p", usage = "The listening (server) port - waiting for incoming connections ")
    private int pPort = 4444;

    @Option(name = "-i", usage = "The speaking (client) port - ready to make outgoing connections")
    private int iPort = Integer.MIN_VALUE;

    private Socket socket;
    private final Map<String, ChatRoom> chatRooms = new HashMap<>(); // 只有当前线程可以修改，因此不需要synchronized map
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
        self = new User("localhost:"+pPort,null);
        ServerThread server = new ServerThread(pPort, this);
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
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
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
                System.out.println("CREATE ROOM: Room " + roomToCreate + "created.");
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
     * 没连接的状态下，加入本地的某房间（自己创建的）。
     */
    private void joinLocal(String[] splitLine) {
        if(splitLine.length == 2){
            String roomToJoin = splitLine[1];
            if(chatRooms.containsKey(roomToJoin)){
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
    private void joinRemote(String[] splitLine) {

    }
}