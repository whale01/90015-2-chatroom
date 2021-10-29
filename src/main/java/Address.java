import java.net.InetAddress;

public class Address {

    private String IP;
    private int port;

    public Address(String identity) {
        String[] tokens = identity.split(":");
        this.IP = tokens[0];
        this.port = Integer.parseInt(tokens[1]);
    }

    public Address(String IP, int port) {
        this.IP = IP;
        this.port = port;
    }

    public Address(InetAddress IP, int port) {
        this.IP = IP.toString();
        this.port = port;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toString() {
        return this.IP + ":" + this.port;
    }
}
