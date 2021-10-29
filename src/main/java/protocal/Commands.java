package protocal;

public class Commands {
    /**
     *  LOCAL COMMANDS
     *  do not generate c2s packets.
     *  all except #help are only recognized when not connected to a remote peer
      */
    public static final String CREATEROOM = "createroom";
    public static final String KICK = "kick";
    public static final String HELP = "help";
    /**
     *  REMOTE COMMANDS
     *  generate c2s packets 1:1
     */
    public static final String JOIN = "join";
    public static final String WHO = "who";
    public static final String LIST = "list";
    public static final String QUIT = "quit";
    public static final String LISTNEIGHBORS = "listneighbors";
    public static final String DELETE = "delete";
    public static final String CONNECT = "connect";
    /**
     * doesn’t generate c2s packets on the current connection - does in background.
     * can be issued regardless of whether connected to peer or not.
     * It will always start out locally (not on remote connection), then branch out
     */
    public static final String SEARCHNETWORK = "searchnetwork";


    /**
     * Escapes (转义符) to set the sout color
     */
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_RESET = "\u001B[0m";

}
