import com.fasterxml.jackson.databind.ObjectMapper;
import protocal.c2s.HostChange;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerThread extends Thread{
    private ObjectMapper mapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private int pPort;
    private Peer peer;

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ServerThread(int pPort, Peer peer) {
        this.pPort = pPort;
        this.peer = peer;
    }

    @Override
    public void run() {
        System.out.printf("\nlistening on port %d\n", pPort);
        System.out.print(">");
        try {
            serverSocket = new ServerSocket(pPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                Socket socket = serverSocket.accept(); //a new connection request
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); // set encoding as UTF8
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                User user = new User(socket.getRemoteSocketAddress()+"", null);
                new ServerConnThread(socket,br, this, user).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleHostChange(HostChange hostChange, User user) {
        String host = hostChange.getContent();
    }


}
