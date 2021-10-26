import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class ClientConnThread extends Thread{
    private Socket socket;
    private BufferedReader br;

    public  ClientConnThread(Socket socket,BufferedReader br) {
        this.socket = socket;
        this.br = br;
    }

    @Override
    public void run() {
        while (socket.isConnected()){
            try {
                String line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
