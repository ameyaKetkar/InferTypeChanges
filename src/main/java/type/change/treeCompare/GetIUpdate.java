package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CaptureMappingsLike;
import Utilities.CombyUtils;
import Utilities.RWUtils;
import com.github.gumtreediff.tree.ITree;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import type.change.comby.CombyMatch;
import type.change.comby.ExpressionPattern;
import type.change.comby.Match;
import type.change.comby.SubPattern;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utilities.ASTUtils.getChildren;
import static Utilities.ASTUtils.getCoveringNode;
import static Utilities.CombyUtils.getPerfectMatch;
import static Utilities.CombyUtils.isPerfectMatch;
import static com.google.common.collect.Streams.zip;
import static java.util.Arrays.*;
import static type.change.comby.ExpressionPattern.getInstanceFrom;

public class GetIUpdate {


    private Map<Tuple2<Integer, Integer>, Optional<Tuple3<String, String, Match>>> matchesB4;
    private Map<Tuple2<Integer, Integer>, Optional<Tuple3<String, String, Match>>> matchesAfter;

    public GetIUpdate() {
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();
    }

    public IUpdate getUpdate(ASTNode before, ASTNode after, ITree root1, ITree root2) {

        if (root1 == null || root2 == null) return new NoUpdate();

        if (!root1.hasSameType(root2))
            System.out.println();

        Update upd = new Update(root1, root2, before.toString(), after.toString());

        if (before instanceof Expression && after instanceof Expression)
            upd.setExplanation(getInstance(before.toString(), after.toString()
                    , Tuple.of(root1.getPos(), root1.getEndPos())
                    , Tuple.of(root2.getPos(), root2.getEndPos())));

        if (root1.hasSameType(root2))
            zip(getChildren(root1), getChildren(root2), Tuple::of).forEach(t -> {
                if (t._1().isIsomorphicTo(t._2()))
                    upd.addSubExplanation(new NoUpdate());
                else
                    getCoveringNode(before, t._1()).flatMap(x -> getCoveringNode(after, t._2()).map(y -> Tuple.of(x, y)))
                            .ifPresent(x -> upd.addSubExplanation(getUpdate(x._1(), x._2(), t._1(), t._2())));
            });
        else{
            if(upd.getExplanation() instanceof Explanation){
                ((Explanation) upd.getExplanation()).enhanceExplanation();
            }

            // Decompose the unmatched templte variables into smaller changes
            System.out.println();
        }
        return upd;
    }

    public IUpdate getUpdate(ASTNode before, ASTNode after) {
        ITree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(c -> c.getRoot()).orElse(null);
        ITree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(c -> c.getRoot()).orElse(null);
        return getUpdate(before, after, root1, root2);
    }

    public AbstractExplanation getInstance(String before, String after, Tuple2<Integer, Integer> loc_b4,
                                           Tuple2<Integer, Integer> loc_aftr) {

        Optional<Tuple3<String, String, Match>> explanationBefore;

        if (matchesB4.containsKey(loc_b4)) {
            explanationBefore = matchesB4.get(loc_b4);
        } else {
            explanationBefore = getMatch(before);
        }

        matchesB4.put(loc_b4, explanationBefore);
        if (explanationBefore.isEmpty()) return new NoExplanation();

        Optional<Tuple3<String, String, Match>> explanationAfter;
        if (matchesAfter.containsKey(loc_aftr)) {
            explanationAfter = matchesAfter.get(loc_aftr);
        } else {
            explanationAfter = getMatch(after, Collections.singletonList(explanationBefore.get()._1()));
        }
        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || explanationBefore.get()._1().equals(explanationAfter.get()._1())
                && explanationAfter.get()._1().equals("Identifier")) return new NoExplanation();

        return new Explanation(explanationBefore.get(), explanationAfter.get());
    }

    public static Optional<Tuple3<String, String, Match>> matchWithHeuristics(String src) {
        return matchWithHeuristics(src, new ArrayList<>());
    }

    public static Optional<Tuple3<String, String, Match>> matchWithHeuristics(String src, List<String> heuristicTemplate) {

        Function<List<String>, List<String>> concat = ls -> Stream.concat(heuristicTemplate.stream(), ls.stream())
                .distinct().collect(Collectors.toList());

        if (Character.isDigit(src.charAt(0))) return getMatch(src, concat.apply((asList("NumberLiteral", "Hexdecimal"))));
        else if (!src.contains(" ")) return getMatch(src, concat.apply((asList("NumberLiteral", "Hexdecimal", "Identifier"))));
        else if (src.startsWith("new ")) return
                getMatch(src, concat.apply((asList("ClassInstanceCreation", "Member-Reference"))));
        else if (src.startsWith("(")) return getMatch(src, concat.apply((asList("Cast"))));
        return getMatch(src, new ArrayList<>());
    }


    static Optional<Tuple3<String, String, Match>> getMatch(String source, List<String> templateOrder) {
        return Stream.concat(RWUtils.CAPTURE_PATTERNS.getExpressionPatterns().stream()
                        .filter(x -> templateOrder.contains(x.getName()))
                        .sorted(Comparator.comparingInt(x -> templateOrder.indexOf(x.getName()))),
                RWUtils.CAPTURE_PATTERNS.getExpressionPatterns().stream()
                        .filter(x -> !templateOrder.contains(x.getName())))
                .flatMap(x->getMatch(source, x).stream())
                .findFirst();
    }

    static Optional<Tuple3<String, String, Match>> getMatch(Tuple2<String, String> name_template, String source) {
        Optional<CombyMatch> perfectMatch = getPerfectMatch(name_template._2(), source);
        Optional<Tuple3<String, String, Match>> result = Optional.empty();
        if(perfectMatch.isPresent())
            return Optional.of(name_template.concat(Tuple.of(perfectMatch.get().getMatches().get(0))));

        List<String> allTvR = CombyUtils.getAllTemplateVariableNames(name_template._2())
                .stream().filter(x -> x.endsWith("r"))
                .collect(Collectors.toList());

        for(var tv: allTvR) {
            for (var var_values : CaptureMappingsLike.PATTERNS.entrySet()) {
                String tryTemplate = CombyUtils.substitute(name_template._2(), tv, var_values.getValue());
                Optional<CombyMatch> match = CombyUtils.getMatch(tryTemplate, source, null);
                if (match.isPresent()) {
                    result = getMatch(name_template.map(x -> String.format("%s--%s", var_values.getKey(), x), x -> tryTemplate), source);
                    if (result.isPresent()) {
                        break;
                    }
                }
            }
            if(result.isPresent())
                break;
        }
        return result;
    }
    /*
    pattern: 1= pattern name, 2= pattern with holes
     */
    static Optional<Tuple3<String, String, Match>> getMatch(String source) {

        List<Tuple2<Map.Entry<String, String>, CombyMatch>> basicMatches = CaptureMappingsLike.PATTERNS.entrySet().stream()
                .flatMap(x -> CombyUtils.getMatch(x.getValue(), source, null).map(z -> Tuple.of(x, z)).stream())
                .filter(x -> !x._1().getKey().equals("Anything"))
                .collect(Collectors.toList());

        Optional<Tuple3<String, String, Match>> perfectMatch = basicMatches.stream()
                .filter(x -> isPerfectMatch(source, x._2()))
                .map(x -> Tuple.fromEntry(x._1()).concat(Tuple.of(x._2().getMatches().get(0))))
                .filter(x -> !CombyUtils.isDecomposable(x))
                .findFirst();

        if (perfectMatch.isPresent())
            return perfectMatch;

        Optional<Tuple3<String, String, Match>> currentMatch;
        for (var basicMatch : basicMatches) {
            currentMatch = getMatch(Tuple.fromEntry(basicMatch._1()), source);
            if (currentMatch.isPresent()) {
                return currentMatch;
            }
        }
        return Optional.empty();
    }


//
//        Tuple3<String, String, Optional<CombyMatch>> basicMatch = pattern.concat(Tuple.of(CombyUtils.getMatch(pattern._2(), source, null)));

//        if (basicMatch._3().isEmpty()) return Optional.empty();
//
//        if (!isPerfectMatch(source, basicMatch._3().get())) {
//            List<String> allTvR = CombyUtils.getAllTemplateVariableNames(basicMatch._2())
//                    .stream().filter(x -> x.endsWith("r"))
//                    .collect(Collectors.toList());
//
//            for(var tv: allTvR){
//            for (var var_values : CaptureMappingsLike.PATTERNS.entrySet()) {
//                String tryTemplate = CombyUtils.substitute(basicMatch._2(), tv, var_values.getValue());
//                Optional<CombyMatch> potentialMatch = CombyUtils.getPerfectMatch(tryTemplate, source);
//
//                for (var val : var_values.getValues()) {
//                    String tryTemplate = CombyUtils.substitute(basicMatch._1().getTemplate(),
//                            var_values.getVariable(), val);
//                    if (potentialMatch.isPresent()) {
//                        basicMatch = Tuple.of(getInstanceFrom(basicMatch._1(), tryTemplate), potentialMatch.get());
//                        break;
//                    }
//                }
//                if (potentialMatch.isPresent()) break;
//            }
//            }
//        }


    /**
     * Original implementation
     * @param source
     * @param x
     * @return
     */
    static Optional<Tuple3<String, String, Match>> getMatch(String source, ExpressionPattern x) {

        Tuple2<ExpressionPattern, CombyMatch> basicMatch = CombyUtils.getMatch(x.getTemplate(), source, null)
                .map(y -> Tuple.of(x, y)).orElse(null);

        if (basicMatch == null) return Optional.empty();

        if (!isPerfectMatch(source, basicMatch._2())) {
            List<SubPattern> subPatterns = basicMatch._1().getSubPatterns();
            for (var var_values : subPatterns) {
                Optional<CombyMatch> potentialMatch = Optional.empty();
                for (var val : var_values.getValues()) {
                    String tryTemplate = CombyUtils.substitute(basicMatch._1().getTemplate(),
                            var_values.getVariable(), val);
                    potentialMatch = CombyUtils.getPerfectMatch(tryTemplate, source);
                    if (potentialMatch.isPresent()) {
                        basicMatch = Tuple.of(getInstanceFrom(basicMatch._1(), tryTemplate), potentialMatch.get());
                        break;
                    }
                }
                if (potentialMatch.isPresent()) break;
            }
        }

        if (!isPerfectMatch(source, basicMatch._2)) {
            return Optional.empty();
        }


        Tuple2<ExpressionPattern, Match> refinedTemplate = basicMatch.map2(y -> y.getMatches().get(0));

        for (var template_var : refinedTemplate._2().getEnvironment()) {
            var sp = refinedTemplate._1().getSubPatternFor(template_var.getVariable());
            for (var sub_template : sp) {
                String tryTemplate = CombyUtils.substitute(refinedTemplate._1().getTemplate(), template_var.getVariable(), sub_template);
                Optional<CombyMatch> tryMatch = CombyUtils.getPerfectMatch(tryTemplate, source);
                if (tryMatch.isPresent() && tryMatch.get().getMatches().size() == 1) {
                    refinedTemplate = Tuple.of(getInstanceFrom(basicMatch._1(), tryTemplate), tryMatch.get().getMatches().get(0));
                    break;
                }
            }
        }
        return Optional.of(Tuple.of(refinedTemplate._1().getName(), refinedTemplate._1().getTemplate(), refinedTemplate._2()))
                .filter(y -> leftMostDoesntMatchAnyExpression(y._2()));

    }

    private static boolean leftMostDoesntMatchAnyExpression(String template) {
        return !template.contains(":[9l]");
//
//        Optional<CombyMatch> cm = CombyUtils.getMatch(":[:[var]]", template);
//        return cm.stream().flatMap(x->x.getMatches().stream())
//                .noneMatch(x->x.getMatched().endsWith("l"));
    }


}

