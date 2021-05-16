package type.change.treeCompare;

import Utilities.CaptureMappingsLike;
import Utilities.CombyUtils;
import Utilities.InferredMappings;
import Utilities.InferredMappings.Instance;
import Utilities.RMinerUtils;
import Utilities.RMinerUtils.TypeChange;
import com.github.gumtreediff.tree.ITree;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import type.change.comby.CombyRewrite;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Update implements IUpdate {

    private final ITree before;
    private final ITree after;
    private final String beforeStr;
    private final String afterStr;
    private AbstractExplanation explanation;
    private List<Update> subUpdates;
    private final Instance project_commit_cu_los;
//    private boolean isRelevant;

    public Update(ITree before, ITree after, String beforeStr, String afterStr,AbstractExplanation explanation, CodeMapping codeMapping, TypeChange typeChange) {
        this.before = before;
        this.after = after;
        this.beforeStr = beforeStr;
        this.afterStr = afterStr;
        this.subUpdates = new ArrayList<>();
        this.explanation = explanation;
        this.project_commit_cu_los = new Instance(codeMapping, this, typeChange, explanation);
    }

    public static boolean isSamePosWise(ITree t1, ITree t2){
        return t1.getPos() == t2.getPos() && t1.getEndPos() == t2.getEndPos();
    }


    public Optional<String> applyUpdate(String source){
        if(explanation instanceof NoExplanation) return Optional.empty();
        Explanation expl = (Explanation) this.explanation;
        Optional<CombyRewrite> rewrite = CombyUtils.rewrite(expl.getMatchReplace()._1(), expl.getMatchReplace()._2(), source);
        return rewrite.map(CombyRewrite::getRewrittenSource);
    }

    public Optional<String> applyCutPaste(String source){
        if(explanation instanceof NoExplanation) return Optional.empty();
        if(source.contains(getBeforeStr())) return Optional.of(source.replace(getBeforeStr(), getAfterStr()));
        return Optional.empty();
    }

    public static Stream<Update> getAllDescendants(Update u){
        return u.getSubUpdates().stream().flatMap(x -> Stream.concat(Stream.of(x), getAllDescendants(x)));
    }

    public boolean isIsomorphic(){
        return before.isIsomorphicTo(after);
    }

    public boolean hasSameType() {
        return before.hasSameType(after);
    }

    public ITree getBefore() {
        return before;
    }

    public ITree getAfter() {
        return after;
    }

    public AbstractExplanation getExplanation() {
        return explanation;
    }

    public void addSubExplanation(IUpdate se){
        if(! (se instanceof NoUpdate))
            this.subUpdates.add((Update)se);
    }

    public void setSubUpdates(List<IUpdate> subUpdates) {
        this.subUpdates = subUpdates.stream().filter(x -> ! (x instanceof NoUpdate))
                .map(x -> (Update)x).collect(toList());
    }

    public List<Update> getSubUpdates() {
        return subUpdates;
    }

    public String getBeforeStr() {
        return beforeStr;
    }

    public String getAfterStr() {
        return afterStr;
    }

    public Instance getAsInstance() {
        return project_commit_cu_los;
    }

    public static boolean applyUpdatesAndMatch(Collection<Update> updates, String source, String target){
        Optional<String> curr = Optional.ofNullable(source);
        for(var u : updates){
            curr = curr.flatMap(u::applyUpdate);
            if(curr.isPresent() && curr.get().replace("\n","").equals(target)){
                return true;
            }
        }
        curr = Optional.ofNullable(source);
        for(var u : updates){
            curr = curr.flatMap(u::applyCutPaste);
            if(curr.isPresent() && curr.get().replace("\n","").equals(target)){
                return true;
            }
        }
        return false;
    }

//    public boolean isRelevant() {
//        return this.isRelevant;
//    }

    public void setExplanation(AbstractExplanation e) {
        this.explanation = e;
//        this.isRelevant = explanation instanceof Explanation && isRelevant((Explanation) explanation, getAsInstance());
    }
}
