package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveUserSuccess {

    @JsonProperty("type")
    private final String type = "moveusersuccess";

    public MoveUserSuccess() {
    }

    public String getType() {
        return type;
    }
}
