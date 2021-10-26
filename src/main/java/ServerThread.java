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


    public ServerThread(int pPort) {
        this.pPort = pPort;
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
                User user = new User(socket.getInetAddress().getHostAddress(), null);
                System.out.println("socket.getInetAddress().getHostAddress()" + socket.getInetAddress().getHostAddress());//TODO: testing
                System.out.println("socket.getLocalPort():" + socket.getLocalPort());//TODO: testing
                System.out.println("socket.getPort():" + socket.getPort());//TODO: testing
                System.out.println("socket.getInetAddress():" + socket.getInetAddress());//TODO: testing
                System.out.println("socket.getLocalSocketAddress():" + socket.getLocalSocketAddress());//TODO: testing
                System.out.println("socket.getLocalAddress():" + socket.getLocalAddress());//TODO: testing
                System.out.println("socket.getRemoteSocketAddress():" + socket.getRemoteSocketAddress());//TODO: testing
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
