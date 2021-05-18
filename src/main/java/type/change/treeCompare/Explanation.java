package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.comby.Environment;
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
//    private final Map<String, String> tvMapB4;
//    private final Map<String, String> tvMapAfter;
//    private final Map<String, Range__1> tvMapB4Range;
//    private final Map<String, Range__1> tvMapAfterRange;

//    private final Tuple2<String, String> MatchReplace;
//    private Map<String, String> TemplateVariableDeclarations;
    private final MatchReplace MatchReplace;
//    private Tuple2<String, String> CodeSnippets;

//    // Before -> After
//    private final Map<String, String> matchedTemplateVariables;
//    private final String EntireCodeSnippet = "Everything";
//    private final String PartialCodeSnippet = "Partial-";

    public Explanation(Tuple3<String, String, Match> basicMatchBefore, Tuple3<String, String, Match> basicMatchAfter) {
        this.explanationBefore = Tuple.of(basicMatchBefore._1(), basicMatchBefore._3());
        this.explanationAfter = Tuple.of(basicMatchAfter._1(), basicMatchAfter._3());
        this.baseTemplates = Tuple.of(basicMatchBefore._2(), basicMatchAfter._2());
        this.MatchReplace = new MatchReplace(new PerfectMatch(basicMatchBefore._1(), basicMatchBefore._2(), basicMatchBefore._3()),
                new PerfectMatch(basicMatchAfter._1(), basicMatchAfter._2(), basicMatchAfter._3()));
//        this.tvMapB4 = basicMatchBefore._3().getTemplateVarSubstitutions();
//        this.tvMapAfter = basicMatchAfter._3().getTemplateVarSubstitutions();
//        this.tvMapB4Range = basicMatchBefore._3().getTemplateVarSubstitutionsRange();
//        this.tvMapAfterRange = basicMatchAfter._3().getTemplateVarSubstitutionsRange();
//        this.matchedTemplateVariables = getMatchingTemplateVariablesNewNew();
//        this.MatchReplace = instantiate(baseTemplates);
    }



//    public MatchReplace computeMatchReplace(PerfectMatch basicMatchBefore, PerfectMatch basicMatchAfter){
//
//        var b4CodeSnippet = basicMatchBefore.getCodeSnippet();
//        var afterCodeSnippet = basicMatchAfter.getCodeSnippet();
//
//        var templateVarMapB4 = basicMatchBefore.getTemplateVariableMapping();
//        var templateVarMapAfter = basicMatchAfter.getTemplateVariableMapping();
//        Map<String, String> intersectingTemplateVars = new HashMap<>();
//
//        // Match template variables with the same value
//        // Prefers matches with same key
//        for(var entry_b4: templateVarMapB4.entrySet()){
//            if(entry_b4.getKey().equals("c")) continue;
//            String matchedTemplateVar = "";
//            for(var entry_after: templateVarMapAfter.entrySet()){
//                if(entry_after.getKey().equals("c")) continue;
//                if(entry_b4.getValue().equals(entry_after.getValue())){
//                    if(matchedTemplateVar.isEmpty())
//                        matchedTemplateVar = entry_after.getKey();
//                    if(entry_b4.getKey().equals(entry_after.getKey())){
//                        matchedTemplateVar = entry_after.getKey();
//                        break;
//                    }
//
//                }
//            }
//            if(!matchedTemplateVar.isEmpty())
//                intersectingTemplateVars.put(entry_b4.getKey(), matchedTemplateVar);
//        }
//
//
//
//
//
//
//        return null;
//
//    }

//
//    public void decomposeTemplateVariable(String templateVarName, boolean isBefore){
//        String expression = isBefore ? tvMapB4.get(templateVarName) : tvMapAfter.get(templateVarName);
//
//    }


    public Map<String, String> getMatchedTemplateVariables() {
        return MatchReplace.getTemplateVariableDeclarations();
    }


    public String getCodeSnippetB4() {
        return getExplanationBefore()._2().getMatched();
    }

    public String getCodeSnippetAfter() {
        return getExplanationAfter()._2().getMatched();
    }

    public Map<String, String> getTvMapB4() {
        return MatchReplace.getMatch().getTemplateVariableMapping();
    }
//
    public Map<String, String> getTvMapAfter() {
        return MatchReplace.getReplace().getTemplateVariableMapping();
    }
//
//    public Tuple2<String, String> getBaseTemplates() {
//        return baseTemplates;
//    }
//
    public List<Tuple2<String, String>> getUnmappedTVAfter() {
        return MatchReplace.getUnMatchedTemplateVarsAfter().entrySet().stream().map(Tuple::fromEntry)
                .collect(toList());
    }

    public List<Tuple2<String, String>> getUnMappedTVB4() {
        return MatchReplace.getUnMatchedTemplateVarsBefore().entrySet().stream().map(Tuple::fromEntry)
                .collect(toList());
    }
//
    public Tuple2<String, String> getMatchReplace() {
        return Tuple.of(MatchReplace.getMatch().getTemplate(), MatchReplace.getReplace().getTemplate());
    }
//
//
//    private Optional<String> anyTemplateVarMatches(Map<String, String> templateVarMap, String codeSnippet) {
//        Optional<String> c = templateVarMap.entrySet().stream().filter(y -> !y.getKey().endsWith("c"))
//                .filter(ea -> ea.getValue().equals(codeSnippet)).map(Entry::getKey)
//                .findFirst();
//        return c;
//
//    }

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
//    private Map<String, String> getMatchingTemplateVariablesNewNew() {
//        Optional<String> entireB4 = anyTemplateVarMatches(tvMapAfter, explanationBefore._2().getMatched());
//        if(entireB4.isPresent()) return Map.of(EntireCodeSnippet, entireB4.get());;
//
//        Optional<String> entireAfter = anyTemplateVarMatches(tvMapB4, explanationAfter._2().getMatched());
//        if (entireAfter.isPresent()) return Map.of(entireAfter.get(), EntireCodeSnippet);
//
//        return getMatchingTemplateVariables();
//
//    }

//    private HashMap<String, String> getMatchingTemplateVariables() {
//        HashMap<String, String> templateVariableMap = new HashMap<>();
//        // b4TVs * [AfterTVs] where value matches
//        Map<String, List<String>> before_afters =
//                tvMapB4.entrySet().stream().filter(x -> !x.getKey().endsWith("c"))
//                        .flatMap(x -> tvMapAfter.entrySet().stream()
//                                .filter(y -> x.getValue().equals(y.getValue()))
//                                .filter(y -> !y.getKey().endsWith("c"))
//                                .map(y -> Tuple.of(x.getKey(), y.getKey())))
//                        .collect(groupingBy(x -> x._1(),
//                                collectingAndThen(toList(), ls -> ls.stream().map(Tuple2::_2).collect(toList()))));
//
//        // if u find similar keys match, drop other mappings
//        var removeKeysThatContainValues = new ArrayList<String>();
//
//        before_afters.forEach((key, value) -> value.stream().filter(y -> y.equals(key)).findFirst()
//                .ifPresent(x -> {
//                    value.set(0, x);
//                    removeKeysThatContainValues.add(key);
//                }));
//
//        before_afters.forEach((key, values) -> {
//            if (removeKeysThatContainValues.contains(key) || removeKeysThatContainValues.stream().noneMatch(values::contains))
//                values.forEach(value -> {
//                    if (!templateVariableMap.containsValue(value))
//                        templateVariableMap.putIfAbsent(key, value);
//                });
//        });
//
//
//
//        /*
//        for unmatched template variables, check if any template variable's value is == or substring of the other expression
//         */
//
//
//        List<String> unMappedBeforeTV = tvMapB4.keySet().stream()
//                .filter(x -> !tvMapB4.get(x).equals("") && !templateVariableMap.containsKey(x) && !x.endsWith("c"))
//                .collect(toList());
//        List<String> unMappedAfterTV = tvMapAfter.keySet().stream()
//                .filter(x -> !tvMapAfter.get(x).equals("") && !templateVariableMap.containsValue(x) && !x.endsWith("c"))
//                .collect(toList());
//
//        String codeSnippetB4 = explanationBefore._2().getMatched();
//        String codeSnippetAfter = explanationAfter._2().getMatched();
//
//        Map<String, String> tvToSubExprB4 = unMappedBeforeTV.stream().filter(tv -> isContainedTokenize(codeSnippetAfter, tv, tvMapB4))
//                .collect(toMap(tv -> tv, tv -> PartialCodeSnippet + tvMapB4.get(tv)));
//        Map<String, String> tvToSubExprAfter = unMappedAfterTV.stream().filter(tv -> isContainedTokenize(codeSnippetB4, tv, tvMapAfter))
//                .collect(toMap(tv -> tv, tv -> PartialCodeSnippet + tvMapAfter.get(tv)));
//
//        // If the matched subexpression overlap, remove the smaller one
//        HashSet<String> removeKeys = new HashSet<>();
//        tvToSubExprB4.forEach((k1, v1) -> tvToSubExprAfter.forEach((k2, v2) -> {
//            String x = v2.replace(PartialCodeSnippet, "");
//            String y = v1.replace(PartialCodeSnippet, "");
//            if (y.contains(x))
//                removeKeys.add(k2);
//            if (x.contains(y))
//                removeKeys.add(k1);
//        }));
//
//        tvToSubExprB4.forEach((k1, v1) -> {
//            if (!removeKeys.contains(k1))
//                templateVariableMap.put(k1, v1);
//        });
//        tvToSubExprAfter.forEach((k2, v2) -> {
//            if (!removeKeys.contains(k2))
//                templateVariableMap.put(v2, k2);
//        });
//
//
//        return templateVariableMap;
//    }

//    private boolean isContainedTokenize(String codeSnippetB4, String tv, Map<String, String> tvMap) {
//
//        List<String> codeSnippetTokens = ASTUtils.getAllTokens(codeSnippetB4)
//                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
//        List<String> tvTokens = ASTUtils.getAllTokens(tvMap.get(tv))
//                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
//        if (tvTokens.size() == 0 || codeSnippetTokens.size() == 0) {
//            return codeSnippetB4.contains(tvMap.get(tv));
//        }
//        return Collections.indexOfSubList(codeSnippetTokens, tvTokens) != -1;
//    }


//    private Tuple2<String, String> instantiate(Tuple2<String, String> metaTemplate) {
//        Map<String, String> replacements_before = new HashMap<>();
//        Map<String, String> replacements_after = new HashMap<>();
//
//
//        // If a template variable before matches entire after snippet or vice versa
//        if (matchedTemplateVariables.size() == 1 && matchedTemplateVariables.entrySet().stream()
//                .anyMatch(x -> x.getKey().equals(EntireCodeSnippet) || x.getValue().equals(EntireCodeSnippet))) {
//            Entry<String, String> next = matchedTemplateVariables.entrySet().iterator().next();
//            if (next.getKey().equals(EntireCodeSnippet)) {
//                metaTemplate = metaTemplate.update1(getAsCombyVariable(next.getValue()));
//            } else {
//                metaTemplate = metaTemplate.update2(getAsCombyVariable(next.getKey()));
//            }
//        }
//
//
//        // :[27:e]:[28~\s*]:[29~[\+\-\*\&]*]=:[30~\s*]:[rxrxc~([A-Z][a-z0-9]+)+].:[[rxc]](:[rxa1])
//        // Replace the matched template variables .
//        // Substitute the after template variables in the after template,
//        // with the corresponding before template variables
//        Map<String, String> renameTVsAfter = new HashMap<>();
//        Map<String, String> renameTVsBefore = new HashMap<>();
//
//        for (var e : matchedTemplateVariables.entrySet()) {
//            if (!((Predicate<Entry<String, String>>) e1 -> e1.getKey().contains(PartialCodeSnippet)
//                    || e1.getValue().contains(PartialCodeSnippet) || e1.getKey().equals(EntireCodeSnippet) || e1.getValue().equals(EntireCodeSnippet)).test(e))
//                if (tvMapAfter.containsKey(e.getKey()) && !e.getValue().equals(e.getKey())) {
//                    renameTVsAfter.put(e.getValue(), e.getKey() + "z");
//                    renameTVsBefore.put(e.getKey(), e.getKey() + "z");
//                } else
//                    renameTVsAfter.put(e.getValue(), e.getKey());
//        }
//
//        metaTemplate = metaTemplate.map(x -> renameTemplateVariable(x, renameTVsBefore), x -> renameTemplateVariable(x, renameTVsAfter));
//
//        renameTVsBefore.forEach((k, v) -> {
//            tvMapB4.put(v, tvMapB4.remove(k));
//            tvMapB4Range.put(v, tvMapB4Range.remove(k));
//            matchedTemplateVariables.put(v, matchedTemplateVariables.remove(k));
//        });
//
//        renameTVsAfter.forEach((k, v) -> {
//            tvMapAfter.put(v, tvMapAfter.remove(k));
//            tvMapAfterRange.put(v, tvMapAfterRange.remove(k));
//            matchedTemplateVariables.entrySet().stream()
//                    .filter(x->x.getValue().equals(k)).findFirst().map(Entry::getKey)
//                    .ifPresent(x -> matchedTemplateVariables.replace(x, k, v));
//        });
//
//        // Substitute constants (not found as values in matched template variables)
//        if(!matchedTemplateVariables.containsValue(EntireCodeSnippet))
//            tvMapAfter.entrySet().stream()
//                    .filter(x -> !matchedTemplateVariables.containsValue(x.getKey()))
//                    .forEach(r -> replacements_after.put(r.getKey(), r.getValue()));
//
//        // Substitute constants (not found as keys in matched template variables)
//        if(!matchedTemplateVariables.containsKey(EntireCodeSnippet))
//            tvMapB4.entrySet().stream()
//                    .filter(x -> !matchedTemplateVariables.containsKey(x.getKey()))
//                    .forEach(r -> replacements_before.put(r.getKey(), r.getValue()));
//
//        metaTemplate = metaTemplate.map(x -> Utilities.CombyUtils.substitute(x, replacements_before),
//                x -> CombyUtils.substitute(x, replacements_after));
//
//        for (var e : matchedTemplateVariables.entrySet()) {
//            if (e.getKey().contains(PartialCodeSnippet)) {
//                metaTemplate = metaTemplate.map1(x -> x.replace(e.getKey().replace(PartialCodeSnippet, ""), getAsCombyVariable(e.getValue())));
//            } else if (e.getValue().contains(PartialCodeSnippet))
//                metaTemplate = metaTemplate.map2(x -> x.replace(e.getValue().replace(PartialCodeSnippet, ""), getAsCombyVariable(e.getKey())));
//        }
//        return metaTemplate;
//    }

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
        return MatchReplace.getMatch().getTemplateVariableMappingRange();
    }

    public Map<String, Range__1> getTvMapAfterRange() {
        return MatchReplace.getReplace().getTemplateVariableMappingRange();
    }
}