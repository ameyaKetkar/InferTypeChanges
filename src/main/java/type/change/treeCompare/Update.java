package type.change.treeCompare;

import Utilities.CombyUtils;
import Utilities.InferredMappings.Instance;
import com.github.gumtreediff.tree.Tree;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import Utilities.comby.CombyRewrite;
import org.refactoringminer.RMinerUtils.TypeChange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Update {

    private final Tree before;
    private final Tree after;
    private final String beforeStr;
    private final String afterStr;
    private MatchReplace matchReplace;
    private final List<Update> subUpdates;
    private Instance project_commit_cu_los;

    public Update(Tree before, Tree after, String beforeStr, String afterStr, MatchReplace matchReplace, CodeMapping codeMapping, TypeChange typeChange, String commit, String repoName) {
        this.before = before;
        this.after = after;
        this.beforeStr = beforeStr;
        this.afterStr = afterStr;
        this.subUpdates = new ArrayList<>();
        this.matchReplace = matchReplace;
        this.project_commit_cu_los = new Instance(codeMapping, this, typeChange, commit, repoName);
    }

    public Optional<String> applyUpdate(String source){
        if(matchReplace == null) return Optional.empty();
        Optional<CombyRewrite> rewrite = CombyUtils.rewrite(matchReplace.getMatchReplace()._1(), matchReplace.getMatchReplace()._2(), source);
        return rewrite.map(CombyRewrite::getRewrittenSource);
    }

    public Optional<String> applyCutPaste(String source){
        if(matchReplace == null) return Optional.empty();
        if(source.contains(getBeforeStr())) return Optional.of(source.replace(getBeforeStr(), getAfterStr()));
        return Optional.empty();
    }

    public static Stream<Update> getAllDescendants(Update u){
        if(u == null || u.getSubUpdates() == null)
            return Stream.empty();
        return u.getSubUpdates().stream().flatMap(x -> Stream.concat(Stream.of(x), getAllDescendants(x)));
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

    public Optional<MatchReplace> getMatchReplace() {
        return Optional.ofNullable(matchReplace);
    }

    public void addSubExplanation(Update u){
        if(u != null)
            this.subUpdates.add(u);
    }

    public void addAllSubExplanation(List<Update> se){
        this.subUpdates.addAll(se);
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

    public void resetMatchReplace() {
        this.matchReplace = null;
        this.project_commit_cu_los = null;
    }

    public void setMatchReplace(MatchReplace e) {
        this.matchReplace = e;
        if(e != null)
            this.project_commit_cu_los.updateExplanation(this);
        else this.project_commit_cu_los = null;
    }
}
