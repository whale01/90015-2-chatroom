import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import protocal.Commands;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Peer {

    @Option(name="-p", usage = "The listening (server) port - waiting for incoming connections ")
    private int pPort = 4444;

    @Option(name="-i", usage = "The speaking (client) port - ready to make outgoing connections")
    private int iPort = Integer.MIN_VALUE;

    private Socket socket;
    private List<ChatRoom> chatRooms = Collections.synchronizedList(new ArrayList<ChatRoom>());

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
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("Use it like this: java -jar chatpeer.jar [-p port] [-i port]");
            parser.printUsage(System.err);
            System.err.println();
        }
    }

    private void act() throws IOException, InterruptedException {
        // start the server first (unconnected as client)
        ServerThread server = new ServerThread(pPort);
        server.start();
        // listen for user input
        Scanner sc = new Scanner(System.in);
        while(sc.hasNext()){
            String line = sc.nextLine();
            if(line.startsWith("#")){
                String[] splitLine = line.strip().split(" ");
                String command = splitLine[0].substring(1); // get the actual command
                switch (command){
                    case Commands.CONNECT:
                        connect(splitLine);
                }
            }
            System.out.print(">");
        }

    }

    private void connect(String[] splitLine) throws IOException {
        if(splitLine.length == 2 || splitLine.length == 3){
            String[] splitArg = splitLine[1].split(":");
            String remoteAddress = splitArg[0];
            String remotePort = splitArg[1];
            //TODO: 验证address:port格式正确性
            switch (splitLine.length){
                case (2):
                    if(iPort == Integer.MIN_VALUE){
                        socket = new Socket(remoteAddress, Integer.parseInt(remotePort));
                    }
                    else{
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
            new ClientConnThread(socket,br).start();
        }
        else{
            System.err.println("Wrong number of args CONNECT");
        }
    }
}