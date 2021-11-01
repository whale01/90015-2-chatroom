package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateFail {

    @JsonProperty
    private final String type = "migratefail";

    public MigrateFail() {
    }

    public String getType() {
        return type;
    }
}
