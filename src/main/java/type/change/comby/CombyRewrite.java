package type.change.comby;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class CombyRewrite {

    @SerializedName("uri")
    @Expose
    private Object uri;
    @SerializedName("rewritten_source")
    @Expose
    private String rewrittenSource;
    @SerializedName("in_place_substitutions")
    @Expose
    private List<InPlaceSubstitution> inPlaceSubstitutions = null;
    @SerializedName("diff")
    @Expose
    private String diff;

    public Object getUri() {
        return uri;
    }

    public void setUri(Object uri) {
        this.uri = uri;
    }

    public String getRewrittenSource() {
        return rewrittenSource;
    }

    public void setRewrittenSource(String rewrittenSource) {
        this.rewrittenSource = rewrittenSource;
    }

    public List<InPlaceSubstitution> getInPlaceSubstitutions() {
        return inPlaceSubstitutions;
    }

    public void setInPlaceSubstitutions(List<InPlaceSubstitution> inPlaceSubstitutions) {
        this.inPlaceSubstitutions = inPlaceSubstitutions;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
       this.diff = diff;
    }

}