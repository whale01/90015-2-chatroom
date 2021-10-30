package protocal.s2c;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MigrateRoom {

    @JsonProperty("type")
    private static final String type = "migrateroom";
    @JsonProperty("target")
    private String target;

    public MigrateRoom(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
