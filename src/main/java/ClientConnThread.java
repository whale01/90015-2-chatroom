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
import java.util.Map;

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
                        case ("roomlist"):
                            RoomList roomList = mapper.readValue(line, RoomList.class);
                            System.out.println(mapper.writeValueAsString(roomList));
                            break;
                        case("message"):
                            MessageS2C messageS2C = mapper.readValue(line, MessageS2C.class);
                            String id = messageS2C.getIdentity();
                            String content = messageS2C.getContent();
                            System.out.println(id + " : " + content);
                            break;
                        //TODO: case neighbors
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
        String former = roomChange.getFormer();
        String id = roomChange.getIdentity();
        User self = peer.getSelf();
        Boolean quitRemoteSent = peer.getQuitFlag();
        Map<String, ChatRoom> chatRooms = peer.getChatRooms();

        if(quitRemoteSent && roomid.equals("")){ // got response for a quit command
            socket.close();
            socket = null;
            peer.setSocket(null);
            quitFlag = true;
            peer.setConnected(false);
            peer.setQuitFlag(false); // unconnected, but can apparently make new connections.
            return;
        }

        if (id.equals(self.getUserId())){ // this roomchange is regarding me
            if (roomid.equals(former)){ // failed join (both join to leave & join to join)
                System.out.println("The requested room is invalid or non existent.");
            }
            else{
                if ( former.equals("")){ // "" -> "roomid"
                    ChatRoom chatRoom = chatRooms.get(roomid);
                    assert chatRoom != null;
                    self.setCurrentRoom(chatRoom);
                    System.out.println(id + " moved to " + roomid);
                }
                else{
                    if(roomid.equals("")){ // "former" -> ""
                        self.setCurrentRoom(null);
                    }
                    else { // "former" -> "roomid"
                        ChatRoom chatRoom = chatRooms.get(roomid);
                        assert chatRoom != null;
                        self.setCurrentRoom(chatRoom);
                    }
                    System.out.println(id + " moved from " + former + " to " + roomid);
                }
            }
        }
        else { // this roomchange has nothing to do with me
            System.out.println(
                    id + " moved from " + (former.equals("") ? "noroom": former)
                            + " to " + (roomid.equals("") ? "noroom " : roomid)
            );
        }
    }


}
