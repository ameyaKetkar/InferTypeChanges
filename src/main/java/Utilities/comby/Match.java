
package Utilities.comby;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import static java.util.stream.Collectors.*;

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

    public Map<String, String> getTemplateVarSubstitutions() {
        return getEnvironment().stream().collect(toMap(x -> x.getVariable(), x -> x.getValue(), (a,b)->b));
    }

    public Map<String, List<Range__1>> getTemplateVarSubstitutionsRange() {
        return getEnvironment().stream().collect(groupingBy(x -> x.getVariable(),
                collectingAndThen(toList(), lss -> lss.stream().map(x->x.getRange()).collect(toList()))));
    }

    public void setMatched(String matched) {
        this.matched = matched;
    }

    public static Match renamedInstance(Match m1, Map<String, String> renames){
        Match res = new Match();
        res.setMatched(m1.getMatched());
        res.setRange(m1.getRange());
        List<Environment> newEnv = new ArrayList<>();
        for(var e: m1.getEnvironment()){
            Environment e_new = new Environment();
            e_new.setVariable(renames.containsKey(e.getVariable()) ? renames.get(e.getVariable()) : e.getVariable());
            e_new.setValue(e.getValue());
            newEnv.add(e_new);
        }
        res.setEnvironment(newEnv);
        return res;
    }

}
