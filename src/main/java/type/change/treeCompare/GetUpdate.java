package type.change.treeCompare;

import Utilities.ASTUtils;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.*;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.eclipse.jdt.core.dom.*;
import org.refactoringminer.RMinerUtils.TypeChange;
import type.change.visitors.LowerCaseIdentifiers;

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


//    private final Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesB4;
//    private final Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;
    private final String commit;
    private final String repoName;

    public GetUpdate(CodeMapping codeMapping, TypeChange typeChange, String commit, String repoName) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
//        this.matchesB4 = new HashMap<>();
//        this.matchesAfter = new HashMap<>();
        this.commit = commit;
        this.repoName = repoName;
    }

    public boolean areEqualInText(Tree t1, Tree t2) {
        return t1.getPos() == t2.getPos() && t1.getEndPos() == t2.getEndPos();
    }

    public boolean isInDomain(ASTNode node){
        return node instanceof Expression || node instanceof VariableDeclarationStatement || node instanceof ExpressionStatement;
    }


    public static boolean isSafe(ASTNode before, ASTNode after){
        LowerCaseIdentifiers v1= new LowerCaseIdentifiers(), v2 = new LowerCaseIdentifiers();
        before.accept(v1);
        after.accept(v2);
        ImmutableSet<String> varIdentifiers1 = Sets.difference(v1.identifiers, v1.methodNames).immutableCopy(),
                varIdentifiers2 = Sets.difference(v2.identifiers, v2.methodNames).immutableCopy();
        return Sets.difference(varIdentifiers2, varIdentifiers1).isEmpty() && Sets.difference(v2.stringLiterals, v1.stringLiterals).isEmpty()
                && Sets.difference(v2.numberLiterals, v1.numberLiterals).isEmpty();
    }

    public Update getUpdate(ASTNode before, ASTNode after, Tree root1, Tree root2) {

        if (root1 == null || root2 == null) return null;

        if(root1.getChildren().size() == 1 && root1.getChildren().size() == root2.getChildren().size()) {
            Tree child1 = root1.getChild(0);
            Tree child2 = root2.getChild(0);
            if (areEqualInText(root1, child1) && areEqualInText(root2, child2)) {
                if (root1.getType().name.equals("ExpressionStatement") && root2.getType().name.equals("ExpressionStatement")
                        && before.getNodeType() == ASTNode.BLOCK && after.getNodeType() == ASTNode.BLOCK
                        && ((Block) before).statements().size() == 1 && ((Block) after).statements().size() == 1)
                    return getUpdate(((ExpressionStatement)((Block) before).statements().get(0)).getExpression(),
                            ((ExpressionStatement)((Block) after).statements().get(0)).getExpression(),
                            child1, child2);
                else
                    return getUpdate(before, after, child1, child2);
            }
        }
        boolean isSafe = isSafe(before, after);

        Either<String, MatchReplace> reasonForNoMR_matchReplace;
        if (isInDomain(before) && isInDomain(after)) {

            Tuple2<Integer, Integer> locAftr = Tuple.of(root2.getPos(), root2.getEndPos()),
                    locB4 = Tuple.of(root1.getPos(), root1.getEndPos());

            reasonForNoMR_matchReplace = getMatchReplace(locB4, locAftr, before, after, typeChange.getBeforeName())
                    .filter(r -> !r.getGeneralizations().isEmpty())
                    .getOrElse(() -> getMatchReplaceCompleteDecomposition(before, after, typeChange.getBeforeName()))

            ;
        } else
            reasonForNoMR_matchReplace = Either.left("Not an expression");

        Update upd = new Update(root1, root2, before.toString(), after.toString(),
                reasonForNoMR_matchReplace.getOrNull(),
                codeMapping, typeChange, commit, repoName, isSafe);

        if (root1.hasSameType(root2)) {
            List<Update> subUpdate = tryToMatchCandidates(before, after
                    , getChildren(root1).flatMap(x -> getCoveringNode(before, x).stream()).collect(toList())
                    , getChildren(root2).flatMap(x -> getCoveringNode(after, x).stream()).collect(toList()));
            upd.addAllSubExplanation(subUpdate);
        }
        else {
            if (reasonForNoMR_matchReplace.isRight()) {
                Map<String, String> unMappedTVB4 = reasonForNoMR_matchReplace.get().getUnMatchedBefore().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(Entry::getKey, x -> x.getValue()));
                Map<String, String> unMappedTVAfter = reasonForNoMR_matchReplace.get().getUnMatchedAfter().entrySet().stream()
                        .filter(x -> !x.getKey().endsWith("c")).collect(toMap(Entry::getKey, Entry::getValue));
                List<Update> subUpdate = tryToMatchTheUnMatched(before, after, reasonForNoMR_matchReplace.get(), unMappedTVB4, unMappedTVAfter);
                upd.addAllSubExplanation(subUpdate);
            }else if(reasonForNoMR_matchReplace.getLeft().equals("Not an expression")){
                List<Update> subUpdate = tryToMatchCandidates(before, after
                        , getChildren(root1).flatMap(x -> getCoveringNode(before, x).stream()).collect(toList())
                        , getChildren(root2).flatMap(x -> getCoveringNode(after, x).stream()).collect(toList()));
                upd.addAllSubExplanation(subUpdate);
            }
        }
        if (reasonForNoMR_matchReplace.isEmpty()) {
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
            else if (simplestDescendants.size() == 1
                    && reasonForNoMR_matchReplace.get().getMatch().getTemplate().contains(simplestDescendants.get(0).getMatchReplace().get().getMatch().getTemplate())
                    && reasonForNoMR_matchReplace.get().getReplace().getTemplate().contains(simplestDescendants.get(0).getMatchReplace().get().getReplace().getTemplate()))
                upd.resetMatchReplace();
            else
                mergeParentChildrenMatchReplace(upd, simplestDescendants, typeChange.getBeforeName(), commit);
        else
            mergeParentChildrenMatchReplace(upd, simplestDescendants, typeChange.getBeforeName(), commit);
        return upd;
    }

    private void mergeParentChildrenMatchReplace(Update upd, List<Update> simplestDescendants, String beforeName, String commit) {
        for (var child : simplestDescendants) {
            if(upd.getMatchReplace().isEmpty() || child.getMatchReplace().isEmpty()) continue;
            mergeParentChildMatchReplace(upd.getMatchReplace().get(), child.getMatchReplace().get(), beforeName, commit)
                    .ifPresent(upd::setMatchReplace);
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
                                             List<ASTNode> childrenB4, List<ASTNode> childrenAfter) {

        if (before instanceof Statement) {
            return zip(childrenB4.stream(), childrenAfter.stream(), this::getUpdate).collect(toList());
        }

        return hungarianMatch(before, after, childrenB4, childrenAfter);
    }

    private List<Update> hungarianMatch(ASTNode before, ASTNode after, List<ASTNode> childrenB4, List<ASTNode> childrenAfter) {
        if(before.toString().contains("facade")){
            System.out.println();
        }
        Map<ASTNode, ASTNode> exactMatches = childrenB4.stream()
                .flatMap(x -> childrenAfter.stream().filter(y -> y.toString().equals(x.toString()))
                        .map(y -> Tuple.of(x, y)))
                .collect(toMap(Tuple2::_1, Tuple2::_2, (a, b)->a));

        childrenAfter.stream().filter(x-> x.toString().equals(before.toString()))
                .forEach(e -> exactMatches.put(before, e));

        childrenB4.stream().filter(x-> x.toString().equals(after.toString()))
                .forEach(e -> exactMatches.put(e, after));

        ToIntFunction<Update> overlaps = upd -> Stream.concat(Stream.of(upd), getAllDescendants(upd))
                .filter(i -> i.getMatchReplace().isPresent())
                .mapToInt(e -> e.getMatchReplace().get().getGeneralizations().size())
                .sum();

        List<Tuple5<ASTNode, ASTNode, Update, Integer, Integer>> holeForEachPair = new ArrayList<>();

        childrenAfter.sort(Comparator.comparingInt(c -> c.toString().length()).reversed());
        childrenB4.sort(Comparator.comparingInt(c -> c.toString().length()).reversed());

        int i = 0;
        if(!exactMatches.containsKey(before) && !exactMatches.containsValue(after)) {
            for (var n1 : childrenB4) {
                if (exactMatches.containsKey(n1) || n1.toString().equals(before.toString())) continue;
                int j = 0;
                for (var n2 : childrenAfter) {
                    if (exactMatches.containsValue(n2) || n2.toString().equals(after.toString())) continue;
                    Update upd = getUpdate(n1, n2);
                    if (upd == null) continue;
                    holeForEachPair.add(Tuple.of(n1, n2, upd, overlaps.applyAsInt(upd), i - j < 0 ? i - j : j - i));
                    j += 1;
                }
                i += 1;
            }
        }


        holeForEachPair.sort(reverseOrder(Comparator.<Tuple5<ASTNode, ASTNode, Update, Integer, Integer>>comparingInt(x -> x._4())
                .thenComparingInt(x -> x._5())));

        for (var e : exactMatches.entrySet()) {
            holeForEachPair.add(0, Tuple.of(e.getKey(), e.getValue(), getUpdate(e.getKey(), e.getValue()), 1,0));
        }

        List<Update> result = new ArrayList<>();
        Set<String> alreadyConsideredB4 = new HashSet<>();
        Set<String> alreadyConsideredAfter = new HashSet<>();

        for (var e : holeForEachPair) {
            if (alreadyConsideredB4.contains(e._1().toString()) || alreadyConsideredAfter.contains(e._2().toString()))
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
        for (var ub : unMappedTVB4.entrySet()) {
            for (var r : expl.getUnMatchedBeforeRange().get(ub.getKey())) {
                Optional<ASTNode> n1 = getCoveringNode(before, r);
                if (n1.isEmpty() || n1.get().toString().equals(before.toString())) continue;
                for (var ua : unMappedTVAfter.entrySet()) {
                    for (var r2 : expl.getUnMatchedAfterRange().get(ua.getKey())) {
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

        for (var e : holeForEachPair) {
            if (alreadyConsideredB4.contains(e._1()) || alreadyConsideredAfter.contains(e._2()))
                continue;
            result.add(e._3());
            alreadyConsideredB4.add(e._1());
            alreadyConsideredAfter.add(e._2());
        }

        return result;
    }

    public Update getUpdate(ASTNode before, ASTNode after) {
        Tree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(TreeContext::getRoot).orElse(null);
        Tree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(TreeContext::getRoot).orElse(null);
        return getUpdate(before, after, root1, root2);
    }

    public Either<String, MatchReplace> getMatchReplace(Tuple2<Integer, Integer> loc_b4,
                                                        Tuple2<Integer, Integer> loc_aftr, ASTNode beforeNode, ASTNode afterNode, String beforeName) {

        Optional<PerfectMatch> explanationBefore = PerfectMatch.getMatch(beforeNode);
        if (explanationBefore.isEmpty()) return Either.left("Could not explain Before");

        Optional<PerfectMatch> explanationAfter = PerfectMatch.getMatch(afterNode);
        if (explanationAfter.isEmpty())
            return Either.left("Could not match after");

        if ((explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Stream.of(":[[id]]", ":[a~\\\".*\\\"]",":[st~false]",":[st~true]").anyMatch(x -> explanationAfter.get().getName().equals(x))))
            return Either.left("Identifier updates");

        return Try.of(() -> new MatchReplace(explanationBefore.get(), explanationAfter.get(), beforeName))
                .onFailure(e -> {
                    System.out.println(this.commit);
                    System.out.println(e.toString());
                    System.out.println();
                })
                .toJavaOptional()
                .map(Either::<String, MatchReplace>right)
                .orElse(Either.left("Error when computing MatchReplace"));

    }

    public Either<String, MatchReplace> getMatchReplaceCompleteDecomposition(ASTNode beforeNode, ASTNode afterNode, String beforeName) {

        Optional<PerfectMatch> explanationBefore = PerfectMatch.getMatch(beforeNode)
                .map(PerfectMatch::completelyDecompose);

        if (explanationBefore.isEmpty()) return Either.left("Could not explain Before");

        Optional<PerfectMatch> explanationAfter = PerfectMatch.getMatch(afterNode).map(PerfectMatch::completelyDecompose);

//        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || (explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Set.of(":[[id]]", ":[a~\\\".*\\\"]").contains(explanationAfter.get().getName())))
            return Either.left("Could not explain After");

        return Try.of(() -> new MatchReplace(explanationBefore.get(), explanationAfter.get(), beforeName))
                .onFailure(e -> {
                    System.out.println(this.commit);
                    System.out.println(e.toString());
                })
                .toJavaOptional().map(Either::<String, MatchReplace>right)
                .orElse(Either.left("Error when computing MatchReplace"));
    }

}

