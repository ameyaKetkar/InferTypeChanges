package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.comby.Match;
import Utilities.comby.Range__1;
import com.google.common.collect.Iterables;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Try;
import type.change.T2RLearnerException;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Map.Entry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static type.change.treeCompare.PerfectMatch.*;
import static type.change.treeCompare.PerfectMatch.renamedInstance;

public class MatchReplace {

    private final PerfectMatch Match;
    private final PerfectMatch Replace;
    private final Map<String, String> Generalizations;
    private final Map<String, String> UnMatchedBefore;
    private final Map<String, String> UnMatchedAfter;
    private final Map<String, Range__1> UnMatchedAfterRange;
    private final Map<String, Range__1> UnMatchedBeforeRange;


    public MatchReplace(PerfectMatch beforep, PerfectMatch afterp) throws T2RLearnerException {
        Tuple3<PerfectMatch, PerfectMatch, Map<String, String>> generalization = getGeneralization(beforep, afterp);
        var generalizedMatchAfter = generalization._2();
        var generalizedMatchBefore = generalization._1();

        this.Generalizations = generalizedMatchBefore.getTemplateVariableMapping().entrySet()
                .stream().filter(x -> !x.getKey().endsWith("c") && generalization._3().containsKey(x.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        this.UnMatchedBefore = notInGeneralization(generalizedMatchBefore.getTemplateVariableMapping());
        this.UnMatchedBeforeRange = UnMatchedBefore.keySet().stream()
                .collect(toMap(x -> x, x -> generalizedMatchBefore.getTemplateVariableMappingRange().get(x)));
        this.Match = specialize(generalizedMatchBefore, UnMatchedBefore);
        this.UnMatchedAfter = notInGeneralization(generalizedMatchAfter.getTemplateVariableMapping());
        this.UnMatchedAfterRange = UnMatchedAfter.keySet().stream()
                .collect(toMap(x -> x, x -> generalizedMatchAfter.getTemplateVariableMappingRange().get(x)));
        this.Replace = specialize(generalizedMatchAfter, UnMatchedAfter);
    }

    private Tuple3<PerfectMatch, PerfectMatch, Map<String, String>> getGeneralization(PerfectMatch beforep, PerfectMatch afterp) throws T2RLearnerException {
        Tuple3<PerfectMatch, PerfectMatch, Map<String, String>> generalization = generalize(beforep, afterp);

        Optional<Either<String, String>> wrapUnWrap = isWrapUnWrap(generalization._1(), generalization._2());

        if(wrapUnWrap.isPresent()){
            if (wrapUnWrap.get().isLeft())
                generalization = generalization.update2(generalization._2().updateTemplate(":["+wrapUnWrap.get().getLeft()+"]"));
            else
                generalization = generalization.update1(generalization._1().updateTemplate(":["+wrapUnWrap.get().get()+"]"));
            generalization = generalize(generalization._1(), generalization._2());
        }
        else {
            Optional<Either<String, String>> nxtDcmp = nextDecomposition(generalization._1(), generalization._2(), null);
            int i = 6;
            while (nxtDcmp.isPresent() && i >= 0) {
                if (nxtDcmp.get().isLeft())
                    generalization = generalization.update1(generalization._1().decompose(nxtDcmp.get().getLeft()).orElse(generalization._1()));
                else
                    generalization = generalization.update2(generalization._2().decompose(nxtDcmp.get().get()).orElse(generalization._2()));
                generalization = generalize(generalization._1(), generalization._2());
                nxtDcmp = nextDecomposition(generalization._1(), generalization._2(), nxtDcmp.get());
                i -= 1;
            }
        }

        List<String> intersectingTemplateVars = new ArrayList<>(generalization._3().values());

        /**
         * Get "the variable" and name it so!
         */


        Map<String, String> normalizationRenames = IntStream.range(0, intersectingTemplateVars.size()).boxed()
                .collect(toMap(intersectingTemplateVars::get, x -> "v" + x));

        generalization._2().rename(normalizationRenames);
        generalization._1().rename(normalizationRenames);
        generalization = generalization.update3(getIntersection(generalization._1(), generalization._2()));
        return generalization;
    }

    private PerfectMatch specialize(PerfectMatch match, Map<String, String> unMatched) throws T2RLearnerException {
        return match.updateTemplate(CombyUtils.substitute(match.getTemplate(), unMatched));
    }


    public Optional<Either<String, String>> nextDecomposition(PerfectMatch before, PerfectMatch after, Either<String, String> prev) {
        Map<String, String> intersectingTemplateVars = getIntersection(before, after);
        for (var eb : before.getTemplateVariableMapping().entrySet()) {
            if (eb.getKey().endsWith("c") || intersectingTemplateVars.containsKey(eb.getKey()) || eb.getValue().isEmpty())
                continue;
            for (var ea : after.getTemplateVariableMapping().entrySet()) {
                if (ea.getKey().endsWith("c") || intersectingTemplateVars.containsKey(ea.getKey()) || ea.getValue().isEmpty())
                    continue;

                if (isContainedTokenize(eb.getValue(), ea.getValue())) {
                    Either<String, String> l = Either.left(eb.getKey());
                    if (!l.equals(prev))
                        return Optional.of(l);
                }
                if (isContainedTokenize(ea.getValue(), eb.getValue())) {
                    Either<String, String> r = Either.right(ea.getKey());
                    if (!r.equals(prev))
                        return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Either<String, String>> isWrapUnWrap(PerfectMatch before, PerfectMatch after){
        for (var eb : before.getTemplateVariableMapping().entrySet()) {
            if(eb.getValue().equals(after.getCodeSnippet())){
                return Optional.of(Either.left(eb.getKey()));
            }
        }
        for (var ea : after.getTemplateVariableMapping().entrySet()) {
            if(ea.getValue().equals(before.getCodeSnippet())){
                return Optional.of(Either.right(ea.getKey()));
            }
        }
        return Optional.empty();
    }

    private boolean isContainedTokenize(String largerSnippet, String shorterSnippet) {
        if (largerSnippet.equals(shorterSnippet)) return false;
        List<String> codeSnippetTokens = ASTUtils.getAllTokens(largerSnippet)
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        List<String> tvTokens = ASTUtils.getAllTokens(shorterSnippet)
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        if (tvTokens.size() == 0 || codeSnippetTokens.size() == 0) {
            return largerSnippet.contains(shorterSnippet);
        }
        return Collections.indexOfSubList(codeSnippetTokens, tvTokens) != -1;
    }


    // Match template variables with the same value
    // Prefers matches with same key

    public PerfectMatch getMatch() {
        return Match;
    }

    public PerfectMatch getReplace() {
        return Replace;
    }

    public Map<String, String> getGeneralizations() {
        return Generalizations;
    }


    public Map<String, String> notInGeneralization(Map<String, String> tmap) {
        return tmap.entrySet().stream().filter(x -> !Generalizations.containsKey(x.getKey())
                && !Generalizations.containsValue(x.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));

    }

    public Tuple2<String, String> getMatchReplace() {
        return Tuple.of(getMatch().getTemplate(), getReplace().getTemplate());
    }

//    public Map<String, String> getUnMatchedTemplateVarsBefore(){
//        return notInTemplateVariableDeclarations(Match.getTemplateVariableMapping());
//    }
//    public Map<String, String> getUnMatchedTemplateVarsAfter(){
//        return notInTemplateVariableDeclarations(Replace.getTemplateVariableMapping());
//    }

    public String getCodeSnippetB4() {
        return getMatch().getCodeSnippet();
    }

    public String getCodeSnippetAfter() {
        return getReplace().getCodeSnippet();
    }


    public Map<String, String> getUnMatchedBefore() {
        return UnMatchedBefore;
    }

    public Map<String, String> getUnMatchedAfter() {
        return UnMatchedAfter;
    }


    public Map<String, Range__1> getUnMatchedAfterRange() {
        return UnMatchedAfterRange;
    }


    public Map<String, Range__1> getUnMatchedBeforeRange() {
        return UnMatchedBeforeRange;
    }

    /**
     * @param parent
     * @param child
     * @return Returns an explanation where the parent template is merged with the child template
     * The merge can happen iff one of the template variables in the parent template perfectly match the before
     * and after of the child explanation
     */
    public static Optional<MatchReplace> mergeParentChildMatchReplace(MatchReplace parent, MatchReplace child) {
        Optional<Tuple2<String, String>> b4 = Optional.empty();
        Optional<Tuple2<String, String>> aftr = Optional.empty();

        for (var b : parent.getUnMatchedBefore().entrySet()) {
            if (!b.getValue().contains(child.getCodeSnippetB4()))
                continue;
            for (var a : parent.getUnMatchedAfter().entrySet()) {
                if (a.getValue().contains(child.getCodeSnippetAfter())) {
                    b4 = Optional.of(Tuple.fromEntry(b));
                    aftr = Optional.of(Tuple.fromEntry(a));
                }
            }
        }

        if(b4.isPresent()){
            Tuple2<String, String> finalB = b4.get();
            Tuple2<String, String> finalAftr = aftr.get();

            Map<String, String> renamesB4 = child.Match.getTemplateVariableMapping()
                    .keySet().stream().collect(toMap(x -> x, x -> finalB._1() + "x" + x));
            String newTemplateB4 = renamedInstance(renamesB4, child.Match).getTemplate();
            newTemplateB4 = parent.getUnMatchedBefore().get(finalB._1()).replace("\\\"", "\"")
                    .replace(child.getCodeSnippetB4(), newTemplateB4);

            Map<String, String> renamesAfter = child.Replace.getTemplateVariableMapping()
                    .keySet().stream().collect(toMap(x -> x, x -> finalAftr._1() + "x" + x));
            String newTemplateAfter = renamedInstance(renamesAfter, child.Replace).getTemplate();
            newTemplateAfter = parent.getUnMatchedAfter().get(finalAftr._1()).replace("\\\"", "\"")
                    .replace(child.getCodeSnippetAfter(), newTemplateAfter);

            String mergedB4 = parent.getMatchReplace()._1().replace("\\\"", "\"").replace(b4.get()._2(), newTemplateB4);
            String mergedAfter = parent.getMatchReplace()._2().replace("\\\"", "\"").replace(aftr.get()._2(), newTemplateAfter);

            if (mergedB4.equals(parent.getMatchReplace()._1().replace("\\\"", "\""))
                    && mergedAfter.equals(parent.getMatchReplace()._2().replace("\\\"", "\"")))
                return Optional.of(parent);

            Optional<Utilities.comby.Match> newExplainationBefore = CombyUtils.getMatch(mergedB4, parent.getCodeSnippetB4(), null)
                    .filter(x -> CombyUtils.isPerfectMatch(parent.getCodeSnippetB4(), x))
                    .map(x -> x.getMatches().get(0));

            Optional<Match> newExplainationAfter = CombyUtils.getMatch(mergedAfter, parent.getCodeSnippetAfter(), null)
                    .filter(x -> CombyUtils.isPerfectMatch(parent.getCodeSnippetAfter(), x))
                    .map(x -> x.getMatches().get(0));
            if (newExplainationAfter.isPresent() && newExplainationBefore.isPresent()) {
                PerfectMatch before = new PerfectMatch(parent.getMatch().getName() + "----" + child.getMatch().getName(), mergedB4, newExplainationBefore.get());
                PerfectMatch after = new PerfectMatch(parent.getReplace().getName() + "----" + child.getReplace().getName(), mergedAfter, newExplainationAfter.get());
                return Try.of(() -> new MatchReplace(before, after)).onFailure(e -> e.printStackTrace()).toJavaOptional();
            }
        }
        return Optional.empty();

    }

}
