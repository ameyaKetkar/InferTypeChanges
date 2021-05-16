package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import Utilities.comby.Match;
import Utilities.comby.Range__1;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static Utilities.CombyUtils.*;
import static java.util.stream.Collectors.*;
import Utilities.CombyUtils.*;

/**
 * If before and after MATCH SAME TEMPLATE
 * All template variables on LHS and RHS are the same
 * <p>
 * <p>
 * IF before and after MATCH DIFFERENT TEMPLATE
 * All template variables on LHS and RHS are different
 * Map<Tuple2<String, String>, String> -> Mapping between invariant template variables
 * Map<String, String> variant mapping before
 * Map<String, String> variant mapping after
 */
public class Explanation extends AbstractExplanation {

    private final Tuple2<String, Match> explanationBefore;
    private final Tuple2<String, Match> explanationAfter;


    private final Tuple2<String, String> baseTemplates;
    private final Map<String, String> tvMapB4;
    private final Map<String, String> tvMapAfter;
    private final Map<String, Range__1> tvMapB4Range;
    private final Map<String, Range__1> tvMapAfterRange;
    private final Tuple2<String, String> MatchReplace;
    private String usageCapturingTV;

    // Before -> After
    private final Map<String, String> matchedTemplateVariables;
    private final String EntireCodeSnippet = "Everything";
    private final String PartialCodeSnippet = "Partial-";

    public Explanation(Tuple3<String, String, Match> explanationBefore, Tuple3<String, String, Match> explanationAfter) {
        this.explanationBefore = Tuple.of(explanationBefore._1(), explanationBefore._3());
        this.explanationAfter = Tuple.of(explanationAfter._1(), explanationAfter._3());
        this.baseTemplates = Tuple.of(explanationBefore._2(), explanationAfter._2());
        this.tvMapB4 = getTemplateVarSubstitutions(getExplanationBefore()._2());
        this.tvMapAfter = getTemplateVarSubstitutions(getExplanationAfter()._2());
        this.tvMapB4Range = getTemplateVarSubstitutionsRange(getExplanationBefore()._2());
        this.tvMapAfterRange = getTemplateVarSubstitutionsRange(getExplanationAfter()._2());
        this.matchedTemplateVariables = getMatchingTemplateVariablesNewNew();
        this.MatchReplace = instantiate(baseTemplates);
    }

    public static AbstractExplanation merge(Explanation parent, Explanation child) {
        try {
            Optional<Tuple2<String, String>> b4 = parent.getTvMapB4().entrySet().stream()
                    .filter(x -> x.getValue().replace("\\\"", "\"").equals(child.getCodeSnippetB4()))
                    .map(x -> Tuple.of(x.getKey(), x.getValue().replace("\\\"", "\"")))
                    .findFirst();

            Optional<Tuple2<String, String>> aftr = parent.getTvMapAfter().entrySet().stream()
                    .filter(x -> x.getValue().replace("\\\"", "\"").equals(child.getCodeSnippetAfter()))
                    .map(x -> Tuple.of(x.getKey(), x.getValue().replace("\\\"", "\"")))
                    .findFirst();


        if (b4.isPresent() && aftr.isPresent()) {
            Tuple2<String, Map<String, String>> newTemplate_renamesB4 = renameTemplateVariable(child.getMatchReplace()._1(), x -> b4.get()._1() + "x" + x);
            Tuple2<String, Map<String, String>> newTemplate_renamesAfter = renameTemplateVariable(child.getMatchReplace()._2(), x -> aftr.get()._1() + "x" + x);
            String mergedB4 = parent.getMatchReplace()._1().replace("\\\"", "\"").replace(b4.get()._2(), newTemplate_renamesB4._1());
            String mergedAfter = parent.getMatchReplace()._2().replace("\\\"", "\"").replace(aftr.get()._2(), newTemplate_renamesAfter._1());

            if (mergedB4.equals(parent.getMatchReplace()._1().replace("\\\"", "\""))
                    && mergedAfter.equals(parent.getMatchReplace()._2().replace("\\\"", "\"")))
                return parent;


            Optional<Match> newExplainationBefore = getMatch(mergedB4, parent.getCodeSnippetB4(), null)
                    .filter(x -> isPerfectMatch(parent.getCodeSnippetB4(), x))
                    .map(x -> x.getMatches().get(0));

            Optional<Match> newExplainationAfter = getMatch(mergedAfter, parent.getCodeSnippetAfter(), null)
                    .filter(x -> isPerfectMatch(parent.getCodeSnippetAfter(), x))
                    .map(x -> x.getMatches().get(0));
            if (newExplainationAfter.isPresent() && newExplainationBefore.isPresent())
                return new Explanation(Tuple.of(parent.getExplanationBefore()._1() + "----" + child.getExplanationBefore()._1(), mergedB4, newExplainationBefore.get()),
                        Tuple.of(parent.getExplanationAfter()._1() + "----" + child.getExplanationAfter()._1(), mergedAfter, newExplainationAfter.get()));
        }
        }catch (Exception e){
            e.printStackTrace();
            return new NoExplanation();
        }
        return new NoExplanation();

    }


    public Map<String, String> getMatchedTemplateVariables() {
        return matchedTemplateVariables;
    }


    public String getCodeSnippetB4() {
        return getExplanationBefore()._2().getMatched();
    }

    public String getCodeSnippetAfter() {
        return getExplanationAfter()._2().getMatched();
    }

    public Map<String, String> getTvMapB4() {
        return tvMapB4;
    }

    public Map<String, String> getTvMapAfter() {
        return tvMapAfter;
    }

    public Tuple2<String, String> getBaseTemplates() {
        return baseTemplates;
    }

//    public Map<String, String> getMappingBetweenBaseAndMatch(){
//        return CombyUtils.getMatch(baseTemplates._1(), getMatchReplace()._1(), null)
//                .filter(x -> isPerfectMatch(getMatchReplace()._1(), x))
//                .stream()
//                .flatMap(x -> x.getMatches().get(0).getEnvironment().stream())
//                .collect(toMap(x -> x.getVariable(), x -> x.getValue()));
//    }

//    public Map<String, String> getMappingBetweenBaseAndReplace(){
//        return CombyUtils.getMatch(baseTemplates._2(), getMatchReplace()._2(), null)
//                .filter(x -> isPerfectMatch(getMatchReplace()._1(), x))
//                .stream()
//                .flatMap(x -> x.getMatches().get(0).getEnvironment().stream())
//                .collect(toMap(x -> x.getVariable(), x -> x.getValue()));
//    }



    public void setUsageCapturingTV(String name) {

    }
//
//    // Assumes that the AST types of the nodes did not match
//    public void enhanceExplanation() {
//        List<Tuple2<String, String>> unMappedBeforeTV = getUnMappedTVB4();
//
//        // break down these template variables further into templates
//
//        List<Tuple2<String, Optional<Tuple3<String, String, Match>>>> finerB4TV = unMappedBeforeTV.stream()
//                .map(x -> x.map2(source -> GetIUpdate.getMatch(source, CaptureMappingsLike.PATTERNS_HEURISTICS)))
//                .collect(toList());
//
//        List<Tuple2<String, String>> unMappedAfterTV = getUnmappedTVAfter();
//
//        List<Tuple2<String, Optional<Tuple3<String, String, Match>>>> finerAfterTV = unMappedAfterTV.stream()
//                .map(x -> x.map2(source -> GetIUpdate.getMatch(source, CaptureMappingsLike.PATTERNS_HEURISTICS)))
//                .collect(toList());
//
//        System.out.println();
//
//    }

    public List<Tuple2<String, String>> getUnmappedTVAfter() {
        return tvMapAfter.keySet().stream()
                .filter(x -> !tvMapAfter.get(x).equals(""))
                .filter(x -> !matchedTemplateVariables.containsValue(x) && !x.endsWith("c"))
                .map(x -> Tuple.of(x, tvMapAfter.get(x)))
                .collect(toList());
    }

    public List<Tuple2<String, String>> getUnMappedTVB4() {
        return tvMapB4.keySet().stream()
                .filter(x -> !tvMapB4.get(x).equals(""))
                .filter(x -> !matchedTemplateVariables.containsKey(x) && !x.endsWith("c"))
                .map(x -> Tuple.of(x, tvMapB4.get(x)))
                .collect(toList());
    }

    public Tuple2<String, String> getMatchReplace() {
        return MatchReplace;
    }


    private Optional<String> anyTemplateVarMatches(Map<String, String> templateVarMap, String codeSnippet) {
        Optional<String> c = templateVarMap.entrySet().stream().filter(y -> !y.getKey().endsWith("c"))
                .filter(ea -> ea.getValue().equals(codeSnippet)).map(Entry::getKey)
                .findFirst();
        return c;

    }

    /*
      There can be several cases:
       (i) Entire before maps to v \in TV_{after}
       (v) Entire after maps to a t \in TV_{b4}
       (iii) a subexpression of before matches to v \in TV_{after}
       (iv)  a subexpression of after matches to t \in TV_{b4}
       (ii) t \in TV_{b4} match with v \in TV_{after}


       There are two types of matches:
       (i) Direct match (l == r)
       (ii) Transformation match (l *= r)
     */
    private HashMap<String, String> getMatchingTemplateVariablesNewNew() {
        Optional<String> entireB4 = anyTemplateVarMatches(tvMapAfter, explanationBefore._2().getMatched());
        if(entireB4.isPresent()) return new HashMap<>() {{ put(EntireCodeSnippet, entireB4.get()); }};

        Optional<String> entireAfter = anyTemplateVarMatches(tvMapB4, explanationAfter._2().getMatched());
        if (entireAfter.isPresent()) return new HashMap<>() {{ put(entireAfter.get(), EntireCodeSnippet);}};

        return getMatchingTemplateVariables();

    }

    private HashMap<String, String> getMatchingTemplateVariables() {
        HashMap<String, String> templateVariableMap = new HashMap<>();
        // b4 * [After] where value matches
        Map<String, List<String>> before_afters =
                tvMapB4.entrySet().stream().filter(x -> !x.getKey().endsWith("c"))
                        .flatMap(x -> tvMapAfter.entrySet().stream()
                                .filter(y -> x.getValue().equals(y.getValue()))
                                .filter(y -> !y.getKey().endsWith("c"))
                                .map(y -> Tuple.of(x.getKey(), y.getKey())))
                        .collect(groupingBy(x -> x._1(),
                                collectingAndThen(toList(), ls -> ls.stream().map(Tuple2::_2).collect(toList()))));

        // if u find similar keys match, drop other mappings
        var removeKeysThatContainValues = new ArrayList<String>();

        before_afters.forEach((key, value) -> value.stream().filter(y -> y.equals(key)).findFirst()
                .ifPresent(x -> {
                    value.set(0, x);
                    removeKeysThatContainValues.add(key);
                }));

        before_afters.forEach((key, values) -> {
            if (removeKeysThatContainValues.contains(key) || removeKeysThatContainValues.stream().noneMatch(values::contains))
                values.forEach(value -> {
                    if (!templateVariableMap.containsValue(value))
                        templateVariableMap.putIfAbsent(key, value);
                });
        });


        /*
        for unmatched template variables, check if any template variable's value is == or substring of the other expression
         */

        List<String> unMappedBeforeTV = tvMapB4.keySet().stream()
                .filter(x -> !tvMapB4.get(x).equals(""))
                .filter(x -> !templateVariableMap.containsKey(x) && !x.endsWith("c")).collect(toList());
        List<String> unMappedAfterTV = tvMapAfter.keySet().stream()
                .filter(x -> !tvMapAfter.get(x).equals(""))
                .filter(x -> !templateVariableMap.containsValue(x) && !x.endsWith("c")).collect(toList());

        String codeSnippetB4 = explanationBefore._2().getMatched();
        String codeSnippetAfter = explanationAfter._2().getMatched();

        Map<String, String> tvToSubExprB4 = unMappedBeforeTV.stream().filter(tv -> isContainedTokenize(codeSnippetAfter, tv, tvMapB4))
                .collect(toMap(tv -> tv, tv -> PartialCodeSnippet + tvMapB4.get(tv)));
        Map<String, String> tvToSubExprAfter = unMappedAfterTV.stream().filter(tv -> isContainedTokenize(codeSnippetB4, tv, tvMapAfter))
                .collect(toMap(tv -> tv, tv -> PartialCodeSnippet + tvMapAfter.get(tv)));

        // If the matched subexpression overlap, remove the smaller one
        HashSet<String> removeKeys = new HashSet<>();
        tvToSubExprB4.forEach((k1, v1) -> tvToSubExprAfter.forEach((k2, v2) -> {
            String x = v2.replace(PartialCodeSnippet, "");
            String y = v1.replace(PartialCodeSnippet, "");
            if (y.contains(x))
                removeKeys.add(k2);
            if (x.contains(y))
                removeKeys.add(k1);
        }));

        tvToSubExprB4.forEach((k1, v1) -> {
            if (!removeKeys.contains(k1))
                templateVariableMap.put(k1, v1);
        });
        tvToSubExprAfter.forEach((k2, v2) -> {
            if (!removeKeys.contains(k2))
                templateVariableMap.put(v2, k2);
        });


        return templateVariableMap;
    }

    private boolean isContainedTokenize(String codeSnippetB4, String tv, Map<String, String> tvMap) {

        List<String> codeSnippetTokens = ASTUtils.getAllTokens(codeSnippetB4)
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        List<String> tvTokens = ASTUtils.getAllTokens(tvMap.get(tv))
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        if (tvTokens.size() == 0 || codeSnippetTokens.size() == 0) {
            return codeSnippetB4.contains(tvMap.get(tv));
        }
        return Collections.indexOfSubList(codeSnippetTokens, tvTokens) != -1;
    }


    private Tuple2<String, String> instantiate(Tuple2<String, String> metaTemplate) {
        Map<String, String> replacements_before = new HashMap<>();
        Map<String, String> replacements_after = new HashMap<>();


        Predicate<Entry<String, String>> isPartialOrEntire = e -> e.getKey().contains(PartialCodeSnippet)
          || e.getValue().contains(PartialCodeSnippet) || e.getKey().equals(EntireCodeSnippet) || e.getValue().equals(EntireCodeSnippet);

        // If a template variable before matches entire after snippet or vice versa
        if (matchedTemplateVariables.size() == 1 && matchedTemplateVariables.entrySet().stream()
                .anyMatch(x -> x.getKey().equals(EntireCodeSnippet) || x.getValue().equals(EntireCodeSnippet))) {
            Entry<String, String> next = matchedTemplateVariables.entrySet().iterator().next();
            if (next.getKey().equals(EntireCodeSnippet)) {
                metaTemplate = metaTemplate.update1(getAsCombyVariable(next.getValue()));
            } else {
                metaTemplate = metaTemplate.update2(getAsCombyVariable(next.getKey()));
            }
        }


        // :[27:e]:[28~\s*]:[29~[\+\-\*\&]*]=:[30~\s*]:[rxrxc~([A-Z][a-z0-9]+)+].:[[rxc]](:[rxa1])
        // Replace the matched template variables .
        // Substitute the after template variables in the after template,
        // with the corresponding before template variables
        Map<String, String> renameTVsAfter = new HashMap<>();
        Map<String, String> renameTVsBefore = new HashMap<>();

        for (var e : matchedTemplateVariables.entrySet()) {
            if (!isPartialOrEntire.test(e))
                if (tvMapAfter.containsKey(e.getKey()) && !e.getValue().equals(e.getKey())) {
                    renameTVsAfter.put(e.getValue(), e.getKey() + "z");
                    renameTVsBefore.put(e.getKey(), e.getKey() + "z");
                } else
                    renameTVsAfter.put(e.getValue(), e.getKey());
        }

        metaTemplate = metaTemplate.map(x -> renameTemplateVariable(x, renameTVsBefore), x -> renameTemplateVariable(x, renameTVsAfter));

        renameTVsBefore.forEach((k, v) -> {
            tvMapB4.put(v, tvMapB4.remove(k));
            tvMapB4Range.put(v, tvMapB4Range.remove(k));
            matchedTemplateVariables.put(v, matchedTemplateVariables.remove(k));
        });

        renameTVsAfter.forEach((k, v) -> {
            tvMapAfter.put(v, tvMapAfter.remove(k));
            tvMapAfterRange.put(v, tvMapAfterRange.remove(k));
            matchedTemplateVariables.entrySet().stream()
                    .filter(x->x.getValue().equals(k)).findFirst().map(Entry::getKey)
                    .ifPresent(x -> matchedTemplateVariables.replace(x, k, v));
        });

        // Substitute constants (not found as values in matched template variables)
        if(!matchedTemplateVariables.containsValue(EntireCodeSnippet))
            tvMapAfter.entrySet().stream()
                    .filter(x -> !matchedTemplateVariables.containsValue(x.getKey()))
                    .forEach(r -> replacements_after.put(r.getKey(), r.getValue()));

        // Substitute constants (not found as keys in matched template variables)
        if(!matchedTemplateVariables.containsKey(EntireCodeSnippet))
            tvMapB4.entrySet().stream()
                    .filter(x -> !matchedTemplateVariables.containsKey(x.getKey()))
                    .forEach(r -> replacements_before.put(r.getKey(), r.getValue()));

        metaTemplate = metaTemplate.map(x -> Utilities.CombyUtils.substitute(x, replacements_before),
                x -> CombyUtils.substitute(x, replacements_after));

        for (var e : matchedTemplateVariables.entrySet()) {
            if (e.getKey().contains(PartialCodeSnippet)) {
                metaTemplate = metaTemplate.map1(x -> x.replace(e.getKey().replace(PartialCodeSnippet, ""), getAsCombyVariable(e.getValue())));
            } else if (e.getValue().contains(PartialCodeSnippet))
                metaTemplate = metaTemplate.map2(x -> x.replace(e.getValue().replace(PartialCodeSnippet, ""), getAsCombyVariable(e.getKey())));
        }



        return metaTemplate;
    }


    public Map<String, String> getTemplateVarSubstitutions(Match m) {
        return m.getEnvironment().stream().collect(toMap(x -> x.getVariable(), x -> x.getValue()));
    }

    private Map<String, Range__1> getTemplateVarSubstitutionsRange(Match m) {
        return m.getEnvironment().stream().collect(toMap(x -> x.getVariable(), x -> x.getRange()));
    }

    public Tuple2<String, String> matchedTemplateNames() {
        return Tuple.of(explanationBefore._1(), explanationAfter._1());
    }

    public Tuple2<String, Match> getExplanationBefore() {
        return explanationBefore;
    }

    public Tuple2<String, Match> getExplanationAfter() {
        return explanationAfter;
    }

    public boolean similarTemplatesMatch() {
        return explanationBefore._1().equals(explanationAfter._1());
    }

    public Map<String, Range__1> getTvMapB4Range() {
        return tvMapB4Range;
    }

    public Map<String, Range__1> getTvMapAfterRange() {
        return tvMapAfterRange;
    }
}