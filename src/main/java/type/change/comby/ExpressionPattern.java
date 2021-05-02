
package type.change.comby;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class ExpressionPattern {

    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("Template")
    @Expose
    private String template;
    @SerializedName("subPatterns")
    @Expose
    private List<SubPattern> subPatterns = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<SubPattern> getSubPatterns() {
        return Stream.ofNullable(subPatterns).flatMap(x -> x.stream()).collect(Collectors.toList());
    }

    public void setSubPatterns(List<SubPattern> subPatterns) {
        this.subPatterns = subPatterns;
    }

    public List<String> getSubPatternFor(String template_var){
        return subPatterns != null ? subPatterns.stream()
                .filter(x -> x.getVariable().equals(template_var))
                .findFirst().stream()
                .flatMap(x -> x.getValues().stream()).collect(Collectors.toList())
                : new ArrayList<>();

    }

    public static ExpressionPattern getInstanceFrom(ExpressionPattern basicMatch, String template){
        var exp = new ExpressionPattern();
        exp.setName(basicMatch.getName());
        exp.setSubPatterns(basicMatch.getSubPatterns());
        exp.setTemplate(template);
        return exp;
    }

}
