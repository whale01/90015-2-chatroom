import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;
import protocal.c2s.Join;
import protocal.c2s.List;
import protocal.c2s.Who;

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

    public ServerConnThread(Socket socket, BufferedReader br, ServerThread serverThread, User user){
        this.socket = socket;
        this.br = br;
        this.serverThread = serverThread;
        this.user = user;
        this.mapper = serverThread.getMapper();
    }

    @Override
    public void run() {
        System.out.println("A server conn thread started");
        String line = null;
        while (socket.isConnected()){
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(null != line){
                try {
                    JsonNode jsonNode = mapper.readTree(line);
                    String type = jsonNode.get("type").asText();
                    switch (type){
                        case ("join"):
                            Join join = mapper.readValue(line, Join.class);
                            System.out.println(join);
                            serverThread.handleJoin(join,user);
                            break;
                        case ("who"):
                            Who who = mapper.readValue(line, Who.class);
                            System.out.println(who);
                            serverThread.handleWho(who,user);
                            break;
                        case ("list"):
                            List list = mapper.readValue(line, List.class);
                            System.out.println(list);
                            serverThread.handleList(user);
                            break;
                        case ("quit"):
                            System.out.println("quit");
                            break;
                        case ("message"):
                            System.out.println("message");
                            break;
                        case ("hostchange"):
                            HostChange hostChange = mapper.readValue(line, HostChange.class);
                            serverThread.handleHostChange(hostChange,user);
                            break;
                        case ("listneighbors"):

                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}