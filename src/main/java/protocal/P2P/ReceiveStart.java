package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveStart {

    @JsonProperty("type")
    private final String type = "receivestart";

    public ReceiveStart() {
    }

    public String getType() {
        return type;
    }
}
