
package Utilities.comby;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Environment {

    @SerializedName("variable")
    @Expose
    private String variable;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("range")
    @Expose
    private Range__1 range;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Range__1 getRange() {
        return range;
    }

    public void setRange(Range__1 range) {
        this.range = range;
    }

}
