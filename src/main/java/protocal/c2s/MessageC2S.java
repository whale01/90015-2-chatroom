package protocal.c2s;

public class MessageC2S {
    private String type;
    private String content;

    public MessageC2S(String content) {
        this.type = "message";
        this.content = content;
    }
}
