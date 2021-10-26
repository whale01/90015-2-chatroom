package protocal.c2s;

public class Join {
    private String type;
    private String roomid;

    public Join(String roomid) {
        this.type = "join";
        this.roomid = roomid;
    }
}
