package protocal.P2P;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateSuccess {

    @JsonProperty
    private final String type = "migratesuccess";

    public MigrateSuccess() {
    }

    public String getType() {
        return type;
    }
}
