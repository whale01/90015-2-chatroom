import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;
import protocal.s2c.RoomChange;
import protocal.s2c.RoomContents;
import protocal.s2c.RoomList;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

/**
 * 监听server的response，并根据response生成相关protocol类，
 * 让peer调用相应方法处理。
 */
public class ClientConnThread extends Thread{
    private ObjectMapper mapper;

    private Peer peer;
    private Socket socket;
    private BufferedReader br;

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
                        case ("roomchange"):
                            RoomChange roomChange = mapper.readValue(line, RoomChange.class);
                            System.out.println(mapper.writeValueAsString(roomChange)); //TODO: 目前client接收到response都打印出来。
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
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
