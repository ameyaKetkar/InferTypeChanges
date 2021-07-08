
package Utilities.comby;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Pattern p = Pattern.compile("(.*)_(.*)_equal");
        Matcher mt = p.matcher(this.variable);
        if(mt.matches()){
            this.variable= mt.group(2);
        }else {
            this.variable = variable;
        }
        return variable;
    }

    public void setVariable(String variable) {
        Pattern p = Pattern.compile("(.*)_(.*)_equal");
        Matcher mt = p.matcher(variable);
        if(mt.matches()){
            this.variable= mt.group(2);
        }else {
            this.variable = variable;
        }
    }

    public String getValue() {
        return value.replace("\\\"", "\"")
                .replace("\\n","");
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
