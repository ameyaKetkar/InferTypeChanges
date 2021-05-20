package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.comby.Environment;
import Utilities.comby.Match;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.refactoringminer.RMinerUtils.TypeChange;

import java.util.*;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static Utilities.CombyUtils.*;
import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static type.change.treeCompare.MatchReplace.mergeParentChildMatchReplace;
import static type.change.treeCompare.Update.applyUpdatesAndMatch;
import static type.change.treeCompare.Update.getAllDescendants;

public class GetUpdate {


    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesB4;
    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;

    public GetUpdate(CodeMapping codeMapping, TypeChange typeChange) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();

    }

    public boolean areEqualInText(Tree t1, Tree t2){
        return t1.getPos() == t2.getPos() && t1.getEndPos() == t2.getEndPos();
    }

    public Optional<Update> getUpdate(ASTNode before, ASTNode after, Tree root1, Tree root2) {

        if (root1 == null || root2 == null) return Optional.empty();

        if(root1.getChildren().size() == root2.getChildren().size() && root1.getChildren().size() == 1){
            if(areEqualInText(root1, root1.getChild(0)) && areEqualInText(root2, root2.getChild(0))){
                return getUpdate(before, after, root1.getChild(0), root2.getChild(0));
            }
        }

        Optional<MatchReplace> explanation = before instanceof Expression && after instanceof Expression ?
                getInstance(before.toString(), after.toString(), Tuple.of(root1.getPos(), root1.getEndPos())
                            , Tuple.of(root2.getPos(), root2.getEndPos()))
                : Optional.empty();

        Update upd = new Update(root1, root2, before.toString(), after.toString(),explanation, codeMapping, typeChange);

        if (root1.hasSameType(root2))
            zip(getChildren(root1), getChildren(root2), Tuple::of).forEach(t -> {
                if (!t._1().isIsomorphicTo(t._2()))
                    getCoveringNode(before, t._1()).flatMap(x -> getCoveringNode(after, t._2()).map(y -> Tuple.of(x, y)))
                            .ifPresent(x -> upd.addSubExplanation(getUpdate(x._1(), x._2(), t._1(), t._2())));
            });
        else{
            if(upd.getExplanation().isPresent()){
                Map<String, String> unMappedTVB4 = explanation.get().getUnMatchedBefore().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(Map.Entry::getKey, x-> x.getValue()));
                Map<String, String> unMappedTVAfter = explanation.get().getUnMatchedAfter().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(x -> x.getKey(), x-> x.getValue()));
                Optional<Update> subUpdate = tryToMatchTheUnMatched(before, after, explanation.get(), unMappedTVB4, unMappedTVAfter);
                upd.addSubExplanation(subUpdate);
            }
        }

        if(explanation.isEmpty())
            return Optional.of(upd);

        List<Update> simplestDescendants = removeSubsumedEdits(getAllDescendants(upd)
                .filter(x -> x.getExplanation().isPresent())
                .collect(toList()));

        boolean thisUpdateAddsSomethingNew = applyUpdatesAndMatch(simplestDescendants, upd.getBeforeStr(), upd.getAfterStr());
        if (thisUpdateAddsSomethingNew && !upd.getAsInstance().isRelevant())
            upd.setExplanation(Optional.empty());
        if(!thisUpdateAddsSomethingNew){
            for(var child: simplestDescendants){
                Optional<MatchReplace> m = mergeParentChildMatchReplace(upd.getExplanation().get(), child.getExplanation().get());
                if(m.isPresent()){
                    upd.setExplanation(m);
                }else{
                    child.setExplanation(Optional.empty());
                }
            }

        }
        return Optional.of(upd);
    }



    public static List<Update> removeSubsumedEdits(Collection<Update> allUpdates) {
        List<Update> subsumedEdits = new ArrayList<>();
        for (var i : allUpdates) {
            for (var j : allUpdates) {
                if (isSubsumedBy(i.getBefore(), j.getBefore()) || isSubsumedBy(i.getAfter(), j.getAfter())) {
                    subsumedEdits.add(j);
                }
            }
        }
        return allUpdates.stream().filter(x -> !subsumedEdits.contains(x)).collect(toList());
    }

    public Optional<Update> tryToMatchTheUnMatched(ASTNode before, ASTNode after, MatchReplace expl, Map<String, String> unMappedTVB4, Map<String, String> unMappedTVAfter) {
        if(unMappedTVB4.size() == unMappedTVAfter.size() && unMappedTVB4.size() == 1){
            Optional<ASTNode> n1 = getCoveringNode(before, expl.getUnMatchedBeforeRange()
                    .get(unMappedTVB4.entrySet().iterator().next().getKey()));
            Optional<ASTNode> n2 = getCoveringNode(after, expl.getUnMatchedAfterRange()
                    .get(unMappedTVAfter.entrySet().iterator().next().getKey()));
            if(n1.isPresent() && n2.isPresent()){
                if(!n1.get().toString().equals(before.toString()) && !n2.get().toString().equals(after.toString()))
                    return getUpdate(n1.get(), n2.get());
            }
            else System.out.println(String.join("\n", "Could not find expr", before.toString(), after.toString()));
        }else if (unMappedTVB4.size() != unMappedTVAfter.size() && unMappedTVB4.size() != 0) {
            System.out.println(String.join("\n", "Ow! Too many unmatched vars", before.toString(), after.toString()));
        }
        return Optional.empty();
    }

    public Optional<Update> getUpdate(ASTNode before, ASTNode after) {
        Tree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(TreeContext::getRoot).orElse(null);
        Tree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(TreeContext::getRoot).orElse(null);
        return getUpdate(before, after, root1, root2);
    }

    public Optional<MatchReplace> getInstance(String before, String after, Tuple2<Integer, Integer> loc_b4,
                                           Tuple2<Integer, Integer> loc_aftr) {

        Optional<PerfectMatch> explanationBefore = matchesB4.containsKey(loc_b4) ? matchesB4.get(loc_b4)
                : PerfectMatch.getMatch(before);
        matchesB4.put(loc_b4, explanationBefore);
        if (explanationBefore.isEmpty()) return Optional.empty();

        Optional<PerfectMatch> explanationAfter = matchesAfter.containsKey(loc_aftr) ? matchesAfter.get(loc_aftr)
                : PerfectMatch.getMatch(after);
        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || (explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Stream.of("Identifier", "ClassName","StringLiteral").anyMatch(x -> explanationAfter.get().getName().equals(x))))
            return Optional.empty();


        return Try.of(() -> new MatchReplace(explanationBefore.get(), explanationAfter.get()))
                .onFailure(e -> e.printStackTrace())
                .toJavaOptional();

    }



    /*
    pattern: 1= pattern name, 2= pattern with holes
     */


    public static List<String> getAllTemplateVariableName(String template){

        return CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched().replace("\\\\", "\\"), z))))
                .map(t -> {
                    Environment x = t._2();
                    if (x.getValue().startsWith("[")) return x.getValue().substring(1, x.getValue().length() - 1);
                    else if (x.getValue().contains("~")) return x.getValue().substring(0, x.getValue().indexOf("~"));
                    else return x.getValue();
                })
                .collect(toList());

    }

}

