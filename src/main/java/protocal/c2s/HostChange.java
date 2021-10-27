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
    @JsonProperty("content")
    private String content;

    public HostChange() {
    }

    public HostChange(String content) {
        this.type = "hostchange";
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
