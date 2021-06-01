package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.comby.Range__1;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.refactoringminer.RMinerUtils.TypeChange;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static com.google.common.collect.Streams.zip;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;
import static java.util.Map.*;
import static java.util.stream.Collectors.*;
import static type.change.treeCompare.MatchReplace.mergeParentChildMatchReplace;
import static type.change.treeCompare.Update.applyUpdatesAndMatch;
import static type.change.treeCompare.Update.getAllDescendants;

public class GetUpdate {


    private final Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesB4;
    private final Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;
    private final String commit;

    public GetUpdate(CodeMapping codeMapping, TypeChange typeChange, String commit) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();
        this.commit = commit;
    }

    public boolean areEqualInText(Tree t1, Tree t2){
        return t1.getPos() == t2.getPos() && t1.getEndPos() == t2.getEndPos();
    }

    public Update getUpdate(ASTNode before, ASTNode after, Tree root1, Tree root2) {

        if (root1 == null || root2 == null) return null;

        if (root1.getChildren().size() == root2.getChildren().size()
                && root1.getChildren().size() == 1 && areEqualInText(root1, root1.getChild(0))
                && areEqualInText(root2, root2.getChild(0))) {
            return getUpdate(before, after, root1.getChild(0), root2.getChild(0));
        }

        Either<String,MatchReplace> explanation = before instanceof Expression && after instanceof Expression
                ? getMatchReplace(Tuple.of(root1.getPos(), root1.getEndPos())
                            , Tuple.of(root2.getPos(), root2.getEndPos()), before, after, typeChange.getBeforeName())
                : Either.left("Not an expression");

        if(!explanation.isEmpty() && explanation.get().getGeneralizations().isEmpty()){
             explanation = getMatchReplaceCompleteDecomposition(Tuple.of(root1.getPos(), root1.getEndPos())
                     , Tuple.of(root2.getPos(), root2.getEndPos()), before, after, typeChange.getBeforeName())
                     .map(x->Either.<String, MatchReplace>right(x))
                     .orElse(explanation);
        }

        Update upd = new Update(root1, root2, before.toString(), after.toString(), explanation.getOrNull(), codeMapping, typeChange);

        if (root1.hasSameType(root2)) {
            List<Update> subUpdate = tryToMatchCandidates(before, after
                    , getChildren(root1).flatMap(x -> getCoveringNode(before, x).stream()).collect(toList())
                    , getChildren(root2).flatMap(x -> getCoveringNode(after, x).stream()).collect(toList()));
            upd.addAllSubExplanation(subUpdate);
        }
//
//            zip(getChildren(root1), getChildren(root2), Tuple::of).forEach(t -> {
//                if (!t._1().isIsomorphicTo(t._2()))
//                    getCoveringNode(before, t._1()).flatMap(x -> getCoveringNode(after, t._2()).map(y -> Tuple.of(x, y)))
//                            .ifPresent(x -> upd.addSubExplanation(getUpdate(x._1(), x._2(), t._1(), t._2())));
//            });
        else {
            if(!explanation.isEmpty()) {
                Map<String, String> unMappedTVB4 = explanation.get().getUnMatchedBefore().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(Entry::getKey, x-> x.getValue()));
                Map<String, String> unMappedTVAfter = explanation.get().getUnMatchedAfter().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(x -> x.getKey(), Entry::getValue));
                List<Update> subUpdate = tryToMatchTheUnMatched(before, after, explanation.get(), unMappedTVB4, unMappedTVAfter);
                upd.addAllSubExplanation(subUpdate);
            }
        }
        if(explanation.isEmpty()) {
            return upd;
        }

        List<Update> simplestDescendants = removeSubsumedEdits(getAllDescendants(upd)
                .filter(x -> x.getMatchReplace().isPresent())
                .collect(toList()));

        boolean doesNotAddAnythingNew = applyUpdatesAndMatch(simplestDescendants, upd.getBeforeStr(), upd.getAfterStr());
        if (doesNotAddAnythingNew)
            if (!upd.getAsInstance().isRelevant())
                upd.resetMatchReplace();
            else if (simplestDescendants.stream().noneMatch(d -> d.getAsInstance().isRelevant()))
                upd.resetMatchReplace();
            else if(simplestDescendants.size() == 1
                    && explanation.get().getMatch().getTemplate().contains(simplestDescendants.get(0).getMatchReplace().get().getMatch().getTemplate())
                    && explanation.get().getReplace().getTemplate().contains(simplestDescendants.get(0).getMatchReplace().get().getReplace().getTemplate()))
                upd.resetMatchReplace();
            else
                mergeParentChildrenMatchReplace(upd, simplestDescendants, typeChange.getBeforeName(),commit );
        else
            mergeParentChildrenMatchReplace(upd, simplestDescendants, typeChange.getBeforeName(), commit);
        return upd;
    }

    private void mergeParentChildrenMatchReplace(Update upd, List<Update> simplestDescendants, String beforeName, String commit) {
        for (var child : simplestDescendants) {
            Optional<MatchReplace> m = mergeParentChildMatchReplace(upd.getMatchReplace().get(), child.getMatchReplace().get(), beforeName, commit);
            if (m.isPresent()) upd.setMatchReplace(m.get());
            else child.setMatchReplace(null);
        }
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

    public List<Update> tryToMatchCandidates(ASTNode before, ASTNode after,
                                             Collection<ASTNode> childrenB4, Collection<ASTNode> childrenAfter) {


        Map<ASTNode, ASTNode> exactMatches = childrenB4.stream()
                .flatMap(x -> childrenAfter.stream().filter(y->y.toString().equals(x.toString()))
                        .map(y->Tuple.of(x, y)))
                .collect(toMap(x->x._1(), x->x._2()));

        ToIntFunction<Update> overlaps = upd -> Stream.concat(Stream.of(upd), getAllDescendants(upd))
                .filter(i -> i.getMatchReplace().isPresent())
                .mapToInt(e -> e.getMatchReplace().get().getGeneralizations().size())
                .sum();

        List<Tuple4<ASTNode, ASTNode, Update, Integer>> holeForEachPair = new ArrayList<>();
        for(var n1: childrenB4){
                if (exactMatches.containsKey(n1) || n1.toString().equals(before.toString())) continue;

                for (var n2 : childrenAfter) {
                    if (exactMatches.containsValue(n2)||n2.toString().equals(after.toString())) continue;
                    Update upd = getUpdate(n1, n2);
                    if (upd == null) continue;
                    holeForEachPair.add(Tuple.of(n1, n2, upd, overlaps.applyAsInt(upd)));
                }
        }



        holeForEachPair.sort(reverseOrder(comparingInt(x -> x._4())));

        for(var e: exactMatches.entrySet()){
            holeForEachPair.add(0, Tuple.of(e.getKey(), e.getValue(), getUpdate(e.getKey(), e.getValue()), 1));
        }

        List<Update> result = new ArrayList<>();
        Set<String> alreadyConsideredB4 = new HashSet<>();
        Set<String> alreadyConsideredAfter = new HashSet<>();

        for(var e: holeForEachPair){
            if(alreadyConsideredB4.contains(e._1().toString()) || alreadyConsideredAfter.contains(e._2().toString()))
                continue;
            result.add(e._3());
            alreadyConsideredB4.add(e._1().toString());
            alreadyConsideredAfter.add(e._2().toString());
        }

        return result;
    }

    public List<Update> tryToMatchTheUnMatched(ASTNode before, ASTNode after, MatchReplace expl,
                                                   Map<String, String> unMappedTVB4, Map<String, String> unMappedTVAfter) {

        List<Tuple3<String, String, Update>> holeForEachPair = new ArrayList<>();
        for(var ub: unMappedTVB4.entrySet()){
            for(var r: expl.getUnMatchedBeforeRange().get(ub.getKey())) {
                Optional<ASTNode> n1 = getCoveringNode(before, r);
                if (n1.isEmpty() || n1.get().toString().equals(before.toString())) continue;
                for (var ua : unMappedTVAfter.entrySet()) {
                    for(var r2: expl.getUnMatchedAfterRange().get(ua.getKey())) {
                        Optional<ASTNode> n2 = getCoveringNode(after, r2);
                        if (n2.isEmpty() || n2.get().toString().equals(after.toString())) continue;
                        Update upd = getUpdate(n1.get(), n2.get());
                        if (upd == null) continue;
                        holeForEachPair.add(Tuple.of(ub.getKey(), ua.getKey(), upd));
                    }
                }
            }
        }

        ToIntFunction<Tuple3<String, String, Update>> overlaps = upd -> Stream.concat(Stream.of(upd._3()), getAllDescendants(upd._3()))
                .filter(i -> i.getMatchReplace().isPresent())
                .mapToInt(e -> e.getMatchReplace().get().getGeneralizations().size())
                .sum();

        holeForEachPair.sort(reverseOrder(comparingInt(overlaps)));

        List<Update> result = new ArrayList<>();
        Set<String> alreadyConsideredB4 = new HashSet<>();
        Set<String> alreadyConsideredAfter = new HashSet<>();

        for(var e: holeForEachPair){
            if(alreadyConsideredB4.contains(e._1()) || alreadyConsideredAfter.contains(e._2()))
                continue;
            result.add(e._3());
            alreadyConsideredB4.add(e._1());
            alreadyConsideredAfter.add(e._2());
        }

        return result;
    }

    public Update  getUpdate(ASTNode before, ASTNode after) {
        Tree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(TreeContext::getRoot).orElse(null);
        Tree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(TreeContext::getRoot).orElse(null);
        return getUpdate(before, after, root1, root2);
    }

    public Either<String, MatchReplace> getMatchReplace(Tuple2<Integer, Integer> loc_b4,
                                                        Tuple2<Integer, Integer> loc_aftr, ASTNode beforeNode, ASTNode afterNode, String beforeName) {

        Optional<PerfectMatch> explanationBefore = matchesB4.containsKey(loc_b4) ? matchesB4.get(loc_b4)
                : PerfectMatch.getMatch(beforeNode);
        matchesB4.put(loc_b4, explanationBefore);
        if (explanationBefore.isEmpty()) return Either.left("Could not explain Before");

        Optional<PerfectMatch> explanationAfter = matchesAfter.containsKey(loc_aftr) ? matchesAfter.get(loc_aftr)
                : PerfectMatch.getMatch(afterNode);

        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty())
            return Either.left("Could not match after");

        if ((explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Stream.of(":[[id]]", ":[a~\\\".*\\\"]").anyMatch(x -> explanationAfter.get().getName().equals(x))))
            return Either.left("Identifier updates");

        return Try.of(() -> new MatchReplace(explanationBefore.get(), explanationAfter.get(), beforeName))
                .onFailure(e -> System.out.println(e.toString()))
                .toJavaOptional()
                .map(Either::<String, MatchReplace>right)
                .orElse(Either.left("Error when computing MatchReplace"));

    }

    public Optional<MatchReplace> getMatchReplaceCompleteDecomposition(Tuple2<Integer, Integer> loc_b4,
                                                  Tuple2<Integer, Integer> loc_aftr, ASTNode beforeNode, ASTNode afterNode, String beforeName) {

        Optional<PerfectMatch> explanationBefore = PerfectMatch.getMatch(beforeNode).map(PerfectMatch::completelyDecompose);

        matchesB4.put(loc_b4, explanationBefore);
        if (explanationBefore.isEmpty()) return Optional.empty();

        Optional<PerfectMatch> explanationAfter = PerfectMatch.getMatch(afterNode).map(PerfectMatch::completelyDecompose);

        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || (explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Set.of(":[[id]]", ":[a~\\\".*\\\"]").contains(explanationAfter.get().getName())))
//                && Stream.of(":[[id]]", ":[a~\\\".*\\\"]").anyMatch(x -> explanationAfter.get().getName().equals(x))))
            return Optional.empty();

        return Try.of(() -> new MatchReplace(explanationBefore.get(), explanationAfter.get(), beforeName))
                .onFailure(e -> System.out.println(e.toString()))
                .toJavaOptional();
    }

}

