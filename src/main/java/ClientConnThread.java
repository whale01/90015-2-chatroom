import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;
import protocal.s2c.MessageS2C;
import protocal.s2c.RoomChange;
import protocal.s2c.RoomContents;
import protocal.s2c.RoomList;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * 监听server的response，并根据response生成相关protocol类，
 * 让peer调用相应方法处理。
 */
public class ClientConnThread extends Thread{
    private ObjectMapper mapper;

    private Peer peer;
    private Socket socket;
    private BufferedReader br;
    private Boolean quitFlag = false;

    public  ClientConnThread(Socket socket,BufferedReader br, Peer peer) {
        this.socket = socket;
        this.br = br;
        this.peer = peer;
        this.mapper = peer.getMapper();
    }

    @Override
    public void run() {
        System.out.println("A client conn thread started");
        String line = null;
        while (socket != null && socket.isConnected() && !quitFlag){
            try {
                line = br.readLine();
            } catch (SocketException e){
                System.err.println("Server disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(null != line){
                try {
                    JsonNode jsonNode = mapper.readTree(line);
                    String type = jsonNode.get("type").asText();
                    switch (type){
                        case ("roomchange"):
                            RoomChange roomChange = mapper.readValue(line, RoomChange.class);
                            handleRoomChange(roomChange);
                            break;
                        case ("roomcontents"):
                            RoomContents roomContents = mapper.readValue(line, RoomContents.class);
                            System.out.println(mapper.writeValueAsString(roomContents));;
                            break;
                        case ("hostchange"):
                            //TODO
                            HostChange hostChange = mapper.readValue(line, HostChange.class);
                            break;
                        case ("roomlist"):
                            RoomList roomList = mapper.readValue(line, RoomList.class);
                            System.out.println(mapper.writeValueAsString(roomList));
                            break;
                        case("message"):
                            MessageS2C messageS2C = mapper.readValue(line, MessageS2C.class);
                            System.out.println(mapper.writeValueAsString(messageS2C));
                            break;
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Deal with various cases of roomchange msg received.
     * Scenario 1: join a valid room
     * Scenario 2: #join to leave current room
     * Scenario 3: #quit to leave current room and disconnect
     */
    private void handleRoomChange(RoomChange roomChange) throws IOException, InterruptedException {
        String roomid = roomChange.getRoomid();
        assert roomid != null;
        Boolean quitRemoteSent = peer.getQuitFlag();
        if(roomid.equals("") && !quitRemoteSent){
            //Scenario 2
            System.out.println(mapper.writeValueAsString(roomChange)); //TODO: 目前client接收到response都打印出来。
            System.out.println("Scenario 2: #join to leave current room");
        }
        else if(roomid.equals("") && quitRemoteSent){
            //Scenario 3
            System.out.println(mapper.writeValueAsString(roomChange)); //TODO: 目前client接收到response都打印出来。
            System.out.println("Scenario 3: #quit to leave current room and disconnect");
            socket.close();
            socket = null;
            peer.setSocket(null);
            quitFlag = true;
//            peer.setQuitFlag(true);
            peer.setConnected(false);
        }
        else{
            //Scenario 1
            System.out.println(mapper.writeValueAsString(roomChange)); //TODO: 目前client接收到response都打印出来。
            System.out.println("Scenario 1: join a valid room");

        }
    }

}
