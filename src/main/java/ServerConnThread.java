import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.P2P.MigrateStart;
import protocal.P2P.MigrateUser;
import protocal.c2s.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

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
                    System.out.println(line);
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
                            System.out.println("listneighbors:");
                            System.out.println(user.getAddress());
                            serverThread.handleListNeighbour(user);
                            break;
                        case ("migratestart"):
                            System.out.println("migratestart");
                            MigrateStart migrateStart = mapper.readValue(line, MigrateStart.class);
                            serverThread.handleMigrateRoom(migrateStart, user);
                            break;
                    }
                }
            }
            catch (SocketException e) {
                System.out.printf("%s has disconnected.\n", user.getUserId());
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setQuitFlag(Boolean quitFlag) {
        this.quitFlag = quitFlag;
    }
}