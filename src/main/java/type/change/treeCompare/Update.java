package type.change.treeCompare;

import Utilities.CombyUtils;
import Utilities.InferredMappings.Instance;
import com.github.gumtreediff.tree.Tree;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import Utilities.comby.CombyRewrite;
import org.refactoringminer.RMinerUtils;
import org.refactoringminer.RMinerUtils.TypeChange;

import java.util.*;
import java.util.stream.Stream;

public class Update {

    private final Tree before;
    private final Tree after;
    private final String beforeStr;
    private final String afterStr;
    private Optional<MatchReplace> explanation;
    private List<Update> subUpdates;
    private final Instance project_commit_cu_los;
//    private boolean isRelevant;

    public Update(Tree before, Tree after, String beforeStr, String afterStr, Optional<MatchReplace> explanation, CodeMapping codeMapping, TypeChange typeChange) {
        this.before = before;
        this.after = after;
        this.beforeStr = beforeStr;
        this.afterStr = afterStr;
        this.subUpdates = new ArrayList<>();
        this.explanation = explanation;
        this.project_commit_cu_los = new Instance(codeMapping, this, typeChange, explanation);
    }

    public static boolean isSamePosWise(Tree t1, Tree t2){
        return t1.getPos() == t2.getPos() && t1.getEndPos() == t2.getEndPos();
    }


    public Optional<String> applyUpdate(String source){
        if(explanation.isEmpty()) return Optional.empty();
        Optional<CombyRewrite> rewrite = CombyUtils.rewrite(explanation.get().getMatchReplace()._1(), explanation.get().getMatchReplace()._2(), source);
        return rewrite.map(CombyRewrite::getRewrittenSource);
    }

    public Optional<String> applyCutPaste(String source){
        if(explanation.isEmpty()) return Optional.empty();
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

    public Tree getBefore() {
        return before;
    }

    public Tree getAfter() {
        return after;
    }

    public Optional<MatchReplace> getExplanation() {
        return explanation;
    }

    public void addSubExplanation(Optional<Update> se){
        se.ifPresent(update -> this.subUpdates.add(update));
    }

    public void setSubUpdates(List<Update> subUpdates) {
        this.subUpdates = new ArrayList<>(subUpdates);
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

    public void setExplanation(Optional<MatchReplace> e) {
        this.explanation = e;
//        this.isRelevant = explanation instanceof Explanation && isRelevant((Explanation) explanation, getAsInstance());
    }
}
