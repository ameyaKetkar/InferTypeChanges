
package type.change.comby;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class CapturePatterns {

    @SerializedName("ExpressionPatterns")
    @Expose
    private List<ExpressionPattern> expressionPatterns = null;

    public List<ExpressionPattern> getExpressionPatterns() {
        return expressionPatterns;
    }

    public void setExpressionPatterns(List<ExpressionPattern> expressionPatterns) {
        this.expressionPatterns = expressionPatterns;
    }

}
