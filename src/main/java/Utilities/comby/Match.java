
package Utilities.comby;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Match {

    @SerializedName("range")
    @Expose
    private Range range;
    @SerializedName("environment")
    @Expose
    private List<Environment> environment = null;
    @SerializedName("matched")
    @Expose
    private String matched;

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public List<Environment> getEnvironment() {
        return environment;
    }

    public void setEnvironment(List<Environment> environment) {
        this.environment = environment;
    }

    public String getMatched() {
        return matched;
    }

    public void setMatched(String matched) {
        this.matched = matched;
    }

}
