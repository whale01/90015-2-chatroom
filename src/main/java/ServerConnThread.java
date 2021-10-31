import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

/**
 * Only responsible for listening
 * For each connection made, ServerConnThread listens to the msg sent by the client
 * Deserialize the msg to objects and send relevant information back to ServerThread to handle.
 */
public class ServerConnThread extends Thread {
    private ObjectMapper mapper;

    private Socket socket;
    private BufferedReader br;
    private ServerThread serverThread;
    private User user;

    private Boolean quitFlag = false;

    public ServerConnThread(Socket socket, BufferedReader br, ServerThread serverThread, User user){
        this.socket = socket;
        this.br = br;
        this.serverThread = serverThread;
        this.user = user;
        this.mapper = serverThread.getMapper();
    }

    @Override
    public void run() {
//        System.out.println("A server conn thread started");
        String line = null;
        while (socket.isConnected() && !quitFlag){
            try {
                line = br.readLine();
                if(line != null){
                    JsonNode jsonNode = mapper.readTree(line);
                    String type = jsonNode.get("type").asText();
                    switch (type){
                        case ("join"):
                            Join join = mapper.readValue(line, Join.class);
                            serverThread.handleJoin(join,user);
                            break;
                        case ("who"):
                            Who who = mapper.readValue(line, Who.class);
                            serverThread.handleWho(who,user);
                            break;
                        case ("list"):
                            List list = mapper.readValue(line, List.class);
                            serverThread.handleList(user);
                            break;
                        case ("quit"):
                            Quit quit = mapper.readValue(line, Quit.class);
                            serverThread.handleQuit(user);
                            quitFlag = true;
                            System.out.println("A server conn thread ended.");
                            break;
                        case ("message"):
                            MessageC2S messageC2S = mapper.readValue(line, MessageC2S.class);
                            serverThread.handleMsg(messageC2S,user);
                            break;
                        case ("hostchange"):
                            HostChange hostChange = mapper.readValue(line, HostChange.class);
                            serverThread.handleHostChange(hostChange,user);
                            break;
                        case ("listneighbors"):
                            serverThread.handleListNeighbour(user);
                            System.out.println(line);
                            break;
                    }
                } else {
                    System.out.println("Closing connection.");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setQuitFlag(Boolean quitFlag) {
        this.quitFlag = quitFlag;
    }
}