import java.net.InetAddress;
import java.util.regex.Pattern;

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

    public static boolean isValidIP(String IP) {
        if (IP.equals("localhost")) {
            return true;
        }
        return Pattern.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$", IP);
    }

    public static boolean isValidPort(int port) {
        return Pattern.matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"
                , String.valueOf(port));
    }

    public static boolean isValidPort(String port){
        int numPort;
        try{
            numPort = Integer.parseInt(port);
        }
        catch (NumberFormatException e){
            return false;
        }
        return isValidPort(numPort);
    }

    public boolean isValidAddress() {
        return isValidIP(this.IP) && isValidPort(this.port);
    }

}
