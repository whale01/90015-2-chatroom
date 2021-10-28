package protocal.c2s;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 1.每个protocal类都需要有一个空的constructor，用于jackson反序列化
 * 2.需要getter、setter
 * 3. toString方法是为了测试。
 */

/**
 * no s2c response expected
 * {
 *     "type":"hostchange",
 *     "host": "string"
 * }
 */
public class HostChange{
    @JsonProperty("type")
    private String type;
    @JsonProperty("host")
    private String host;

    public HostChange() {
    }

    public HostChange(String host) {
        this.type = "hostchange";
        this.host = host;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return host;
    }

    public void setContent(String host) {
        this.host = host;
    }


}
