package protocal.c2s;

public class Who {
    private String type;
    private String roomid;

    public Who(String roomid) {
        this.type = "who";
        this.roomid = roomid;
    }
}
