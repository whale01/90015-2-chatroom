package protocal.s2c;

public class MessageS2C {
    private String type;
    private String identity;
    private String content;

    public MessageS2C(String identity, String content) {
        this.type = "message";
        this.identity = identity;
        this.content = content;
    }
}
