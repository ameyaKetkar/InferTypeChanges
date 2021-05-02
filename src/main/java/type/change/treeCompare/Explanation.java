package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import type.change.comby.Match;

import java.util.*;
import java.util.Map.Entry;

import static Utilities.CombyUtils.getAsCombyVariable;
import static java.util.stream.Collectors.*;

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
    private final Tuple2<String, String> MatchReplace;
    private String usageCapturingTV;
    // Before -> After
    private final BiMap<String, String> matchedTemplateVariables;
    private final String EntireCodeSnippet = "Everything";
    private final String PartialCodeSnippet = "Partial-";

    public Explanation(Tuple3<String, String, Match> explanationBefore, Tuple3<String, String, Match> explanationAfter) {
        this.explanationBefore = Tuple.of(explanationBefore._1(), explanationBefore._3());
        this.explanationAfter = Tuple.of(explanationAfter._1(), explanationAfter._3());
        this.baseTemplates = Tuple.of(explanationBefore._2(), explanationAfter._2());
        this.tvMapB4 = getTemplateVarSubstitutions(getExplanationBefore()._2());
        this.tvMapAfter = getTemplateVarSubstitutions(getExplanationAfter()._2());
        this.matchedTemplateVariables = getMatchingTemplateVariablesNewNew();
        this.MatchReplace = instantiate(baseTemplates);
    }


    public void setUsageCapturingTV(String name){

    }

    // Assumes that the AST types of the nodes did not match
    public void enhanceExplanation(){
        List<Tuple2<String, String>> unMappedBeforeTV = tvMapB4.keySet().stream()
                .filter(x -> !tvMapB4.get(x).equals(""))
                .filter(x -> !matchedTemplateVariables.containsKey(x) && !x.endsWith("c"))
                .map(x -> Tuple.of(x, tvMapB4.get(x)))
                .collect(toList());

        // break down these template variables further into templates

        List<Tuple2<String, Optional<Tuple3<String, String, Match>>>> finerB4TV = unMappedBeforeTV.stream()
                .map(x -> x.map2(GetIUpdate::matchWithHeuristics))
                .collect(toList());

        List<Tuple2<String, String>> unMappedAfterTV = tvMapAfter.keySet().stream()
                .filter(x -> !tvMapAfter.get(x).equals(""))
                .filter(x -> !matchedTemplateVariables.containsValue(x) && !x.endsWith("c"))
                .map(x -> Tuple.of(x, tvMapAfter.get(x)))
                .collect(toList());

        List<Tuple2<String, Optional<Tuple3<String, String, Match>>>> finerAfterTV = unMappedAfterTV.stream()
                .map(x -> x.map2(GetIUpdate::matchWithHeuristics))
                .collect(toList());



        System.out.println();

    }

    public Tuple2<String, String> getMatchReplace() {
        return MatchReplace;
    }


    private Optional<String> anyTemplateVarMatches(Map<String, String> templateVarMap, String codeSnippet) {
        return templateVarMap.entrySet().stream().filter(y -> !y.getKey().endsWith("c"))
                .filter(ea -> ea.getValue().equals(codeSnippet)).map(Entry::getKey)
                .findFirst();
    }

    private Optional<Tuple2<String, String>> anyTemplateVarMatchesPartially(Map<String, String> templateVarMap, String codeSnippet) {
        var x =  templateVarMap.entrySet().stream().filter(y -> !y.getKey().endsWith("c"))
                .filter(ea -> codeSnippet.contains(ea.getValue())).map(Tuple::fromEntry)
                .findFirst();
        return x;
    }

    private BiMap<String, String> getMatchingTemplateVariablesNewNew() {
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
        return anyTemplateVarMatches(tvMapAfter, explanationBefore._2().getMatched())
                .map(x -> new ImmutableBiMap.Builder<String,String>().put(EntireCodeSnippet, x).build())
                .or(() -> anyTemplateVarMatches(tvMapB4, explanationAfter._2().getMatched())
                        .map(x -> new ImmutableBiMap.Builder<String,String>().put(x, EntireCodeSnippet).build()))
//                .or(() -> anyTemplateVarMatchesPartially(tvMapB4, explanationAfter._2().getMatched())
//                        .map(x -> new ImmutableBiMap.Builder<String,String>().put(x._1(), PartialCodeSnippet+x._2()).build()))
//                .or(() -> anyTemplateVarMatchesPartially(tvMapAfter, explanationBefore._2().getMatched())
//                        .map(x -> new ImmutableBiMap.Builder<String,String>().put(PartialCodeSnippet+x._2(), x._1()).build()))
                .orElse(getMatchingTemplateVariables());
    }



    private BiMap<String, String> getMatchingTemplateVariablesNew() {
        return anyTemplateVarMatches(tvMapAfter, explanationBefore._2().getMatched())
                    .map(x -> new ImmutableBiMap.Builder<String,String>().put(EntireCodeSnippet, x).build())
                .or(() -> anyTemplateVarMatches(tvMapB4, explanationAfter._2().getMatched())
                            .map(x -> new ImmutableBiMap.Builder<String,String>().put(x, EntireCodeSnippet).build()))
                .orElse(getMatchingTemplateVariables());
    }


    private ImmutableBiMap<String, String> getMatchingTemplateVariables() {



        BiMap<String, String> templateVariableMap = HashBiMap.create();

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
            if(removeKeysThatContainValues.contains(key) || removeKeysThatContainValues.stream().noneMatch(values::contains))
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
            if(y.contains(x))
                removeKeys.add(k2);
            if(x.contains(y))
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



        return ImmutableBiMap.copyOf(templateVariableMap);
    }

    private boolean isContainedTokenize(String codeSnippetB4, String tv, Map<String, String> tvMap) {

        List<String> codeSnippetTokens = ASTUtils.getAllTokens(codeSnippetB4);
        List<String> tvTokens = ASTUtils.getAllTokens(tvMap.get(tv));
        if(tvTokens.size() == 0 || codeSnippetTokens.size() == 0){
            return codeSnippetB4.contains(tvMap.get(tv));
        }
        return Collections.indexOfSubList(codeSnippetTokens, tvTokens) != -1;
    }


    private Tuple2<String, String> instantiate(Tuple2<String, String> metaTemplate) {
        Map<String, String> replacements_before = new HashMap<>();
        Map<String, String> replacements_after = new HashMap<>();

        // If the a template variable before matches entire after snippet or vice versa
        if(matchedTemplateVariables.size() == 1 && matchedTemplateVariables.entrySet().stream()
                .anyMatch(x -> x.getKey().equals(EntireCodeSnippet) || x.getValue().equals(EntireCodeSnippet))){
            Entry<String, String> next = matchedTemplateVariables.entrySet().iterator().next();
            if(next.getKey().equals(EntireCodeSnippet)){
                metaTemplate = metaTemplate.update1(getAsCombyVariable(next.getValue()));
            }else{
                metaTemplate = metaTemplate.update2(getAsCombyVariable(next.getKey()));
            }
        }
        // Replace the matched template variables .
        // Substitute the after template variables in the after template,
        // with the corresponding before template variables
        matchedTemplateVariables.entrySet().stream()
                .filter(x -> !x.getKey().equals(EntireCodeSnippet))
                .filter(x -> !x.getKey().contains(PartialCodeSnippet))
                .filter(x -> !x.getValue().equals(EntireCodeSnippet))
                .filter(x -> !x.getValue().contains(PartialCodeSnippet))
                .forEach(r -> replacements_after.put(r.getValue(), getAsCombyVariable(r.getKey())));

        // Substitute constants (not found as values in matched template variables)
        tvMapAfter.entrySet().stream()
                .filter(x -> !matchedTemplateVariables.containsValue(x.getKey()))
                .forEach(r -> replacements_after.put(r.getKey(), r.getValue()));

        // Substitute constants (not found as keys in matched template variables)
        tvMapB4.entrySet().stream()
                .filter(x -> !matchedTemplateVariables.containsKey(x.getKey()))
                .forEach(r -> replacements_before.put(r.getKey(), r.getValue()));

        Tuple2<String, String> s = metaTemplate.map(x -> CombyUtils.substitute(x, replacements_before),
                x -> CombyUtils.substitute(x, replacements_after));

        for(var e: matchedTemplateVariables.entrySet()){
            if(e.getKey().contains(PartialCodeSnippet)){
                s = s.map1(x -> x.replace(e.getKey().replace(PartialCodeSnippet,""), getAsCombyVariable(e.getValue())));
            }else if(e.getValue().contains(PartialCodeSnippet))
                s = s.map2(x -> x.replace(e.getValue().replace(PartialCodeSnippet,""), getAsCombyVariable(e.getKey())));
        }
        return s;
    }


    public Map<String, String> getTemplateVarSubstitutions(Match m) {
        return m.getEnvironment().stream().collect(toMap(x -> x.getVariable(), x -> x.getValue()));
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

}