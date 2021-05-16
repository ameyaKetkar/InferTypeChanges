
package Utilities.comby;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Range__1 {

    @SerializedName("start")
    @Expose
    private Start__1 start;
    @SerializedName("end")
    @Expose
    private End__1 end;

    public Start__1 getStart() {
        return start;
    }

    public void setStart(Start__1 start) {
        this.start = start;
    }

    public End__1 getEnd() {
        return end;
    }

    public void setEnd(End__1 end) {
        this.end = end;
    }

}
