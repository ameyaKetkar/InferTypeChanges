package type.change.treeCompare;

import Utilities.CaptureMappingsLike;
import Utilities.CombyUtils;
import Utilities.comby.CombyMatch;
import Utilities.comby.Environment;
import Utilities.comby.Match;
import Utilities.comby.Range__1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utilities.CombyUtils.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PerfectMatch {

    private final String Name;
    private final String Template;
    private final Match Match;
    private final String CodeSnippet;

    public PerfectMatch(String name, String template, Match match) {
        Name = name;
        Template = template;
        Match = match;
        CodeSnippet = match.getMatched();
    }

    public PerfectMatch(String name, String template, CombyMatch cm){
        Name = name;
        Template = template;
        Match = cm.getMatches().get(0);
        CodeSnippet = Match.getMatched();
    }

    // Renames the after to before in the after template
    public static Tuple2<PerfectMatch, PerfectMatch> safeRename(PerfectMatch before, PerfectMatch after, Map<String, String> afterNameBeforeName){
        Map<String, String> templateVariableMapping = after.getTemplateVariableMapping();
        // conflict check
        List<String> conflictingBeforeNames = afterNameBeforeName.values().stream().filter(x -> templateVariableMapping.keySet().contains(x))
                .collect(toList());

        afterNameBeforeName = afterNameBeforeName.entrySet().stream()
                .collect(toMap(x -> x.getKey(), x -> conflictingBeforeNames.contains(x.getValue()) ? x.getValue() + "z": x.getValue()));

        Map<String, String> beforeNameNewBeforeName = conflictingBeforeNames.stream().collect(toMap(x -> x, x -> x + "z"));

        after = after.rename(afterNameBeforeName);

        if(!beforeNameNewBeforeName.isEmpty())
            before = before.rename(beforeNameNewBeforeName);

        return Tuple.of(before, after);
    }

    public PerfectMatch rename(Map<String, String> renames){
        String newTemplate = CombyUtils.renameTemplateVariable(Template, renames);
        Optional<PerfectMatch> perfectMatch = getPerfectMatch(Tuple.of(newTemplate, s -> true), CodeSnippet, null)
                .or(() -> getPerfectMatch(Tuple.of(newTemplate, s -> true), CodeSnippet, ".xml"))
                .map(x -> new PerfectMatch(Name, newTemplate, x));
        if(perfectMatch.isEmpty())
            System.out.println();
        return perfectMatch.get();
    }


    public PerfectMatch substitute(Map<String, String> substitutions){
        String newTemplate = CombyUtils.substitute(Template, substitutions);
        Optional<PerfectMatch> perfectMatch = getPerfectMatch(Tuple.of(newTemplate, s -> true), CodeSnippet, null)
                .or(() -> getPerfectMatch(Tuple.of(newTemplate, s -> true), CodeSnippet, ".xml"))
                .map(x -> new PerfectMatch(Name, newTemplate, x));
        if(perfectMatch.isEmpty())
            System.out.println();
        return perfectMatch.get();

    }


    public String getName() {
        return Name;
    }

    public String getTemplate() {
        return Template;
    }

    public Match getMatch() {
        return Match;
    }

    public Map<String, String> getTemplateVariableMapping(){
        return Match.getTemplateVarSubstitutions();
    }

    public Map<String, Range__1> getTemplateVariableMappingRange(){
        return Match.getTemplateVarSubstitutionsRange();
    }

    public String getCodeSnippet() {
        return CodeSnippet;
    }

    public Optional<PerfectMatch> decompose(String templateVariable){
        String decomposeSnippet = Match.getTemplateVarSubstitutions().get(templateVariable);
        Optional<PerfectMatch> decomposedTemplate = getMatch(decomposeSnippet);
        if(decomposedTemplate.isEmpty())
            return Optional.empty();

        Tuple2<String, Map<String, String>> newTemplate_renames = renameTemplateVariable(decomposedTemplate.get().Template, x -> templateVariable + "x" + x);
        String tryTemplate = CombyUtils.substitute(Template, templateVariable, newTemplate_renames._1());
        return getPerfectMatch(Tuple.of(tryTemplate, s->true),CodeSnippet,null)
                .map(x->new PerfectMatch(Name + "-" + decomposedTemplate.get().getName(), tryTemplate, x));



    }


    static Optional<PerfectMatch> getMatch(String source) {
        LinkedHashMap<String, Tuple2<String, Predicate<String>>> patternsHeuristics = CaptureMappingsLike.PATTERNS_HEURISTICS;

        ToIntFunction<Tuple2<Tuple2<String, String>, CombyMatch>> indexOf = x -> {
            int cntr = 0;
            for(var y: patternsHeuristics.entrySet()){
                if(y.getKey().equals(x._1()._1()))
                    return cntr;
                cntr++;
            }
            return cntr;
        };

        CompletableFuture<Optional<Tuple2<Tuple2<String, String>, CombyMatch>>>[] futures = patternsHeuristics.entrySet().stream()
                .filter(x->!x.getKey().equals("Anything"))
                .filter(x -> x.getValue()._2().test(source))
                .map(x -> CompletableFuture.supplyAsync(() -> CombyUtils.getMatch(x.getValue()._1(), source, null)
                        .map(y -> Tuple.of(Tuple.of(x.getKey(), x.getValue()._1()), y))))
                .toArray((IntFunction<CompletableFuture<Optional<Tuple2<Tuple2<String, String>, CombyMatch>>>[]>) CompletableFuture[]::new);

        CompletableFuture.allOf(futures);

        List<Tuple2<Tuple2<String, String>, CombyMatch>> basicMatches =Stream.of(futures)
                .flatMap(x-> Try.of(() -> x.get()).getOrElse(Optional.empty()).stream())
                .sorted(Comparator.comparingInt((Tuple2<Tuple2<String, String>, CombyMatch> x) -> x._2().getMatches().stream().mapToInt(z->z.getMatched().length())
                        .max().getAsInt())
                        .reversed().thenComparingInt(indexOf))
                .collect(Collectors.toList());

        Optional<Tuple3<String, String, Match>> perfectMatch = basicMatches.stream()
                .filter(x -> isPerfectMatch(source, x._2()))
                .map(x -> x._1().concat(Tuple.of(x._2().getMatches().get(0))))
                .filter(x -> !x._2().contains("[r]"))
                .findFirst();

        if (perfectMatch.isPresent())
            return perfectMatch.map(x -> new PerfectMatch(x._1(), x._2(), x._3()));

        Optional<PerfectMatch> currentMatch;

        for (var basicMatch : basicMatches) {
            List<Match> matches = basicMatch._2().getMatches().stream()
                    .sorted(Comparator.comparingInt(x -> x.getMatched().length())).collect(Collectors.toList());
            Collections.reverse(matches);
            for(var m: matches) {
                currentMatch = getMatch(basicMatch._1(), m, source,
                        basicMatch._1()._2().contains("r") ? List.of("r") : List.of());
                if (currentMatch.isPresent()) return currentMatch;

            }
        }
        return Optional.empty();
    }



    static Optional<PerfectMatch> getMatch(Tuple2<String, String> name_template, Match cm, String source,
                                                            List<String> recurssiveTVs) {

        Optional<PerfectMatch> result = Optional.empty();

        if(isPerfectMatch(source, cm) && recurssiveTVs.isEmpty())
            return Optional.of(new PerfectMatch(name_template._1(), name_template._2(), cm));

        for (int i = 0, recurssiveTVsSize = recurssiveTVs.size(); i < recurssiveTVsSize; i++) {
            String tv = recurssiveTVs.get(i);
            Environment env = cm.getEnvironment().stream().filter(x -> x.getVariable().equals(tv)).findFirst().orElse(null);
            if(env == null)
                System.out.println();
            for (Entry<String, Tuple2<String, Predicate<String>>> var_values : CaptureMappingsLike.PATTERNS_HEURISTICS.entrySet()) {
                Predicate<String> heuristic = var_values.getValue()._2();
                boolean tv_Val_matches = heuristic.test(env.getValue());
                boolean anyRemaining = !source.replace("\\\"", "\"").equals(cm.getMatched());
                boolean remainingMatches = anyRemaining && heuristic.test(source.replace("\\\"", "\"").replace(cm.getMatched(), ""));
                if (!tv_Val_matches && !remainingMatches) continue;
                Tuple2<String, Map<String, String>> newTemplate_renames = renameTemplateVariable(var_values.getValue()._1(), x -> tv + "x" + x);
                String tryTemplate = CombyUtils.substitute(name_template._2(), tv, newTemplate_renames._1());
                Optional<CombyMatch> match = CombyUtils.getMatch(tryTemplate, source, null);
                if (match.isPresent()) {
                    for (var m : match.get().getMatches()) {
                        if (m.getMatched().equals(cm.getMatched()) || m.getMatched().contains(cm.getMatched())) {
                            List<String> recurssiveTVs_Temp = Stream.concat(recurssiveTVs.subList(i + 1, recurssiveTVsSize).stream(),
                                    Stream.ofNullable(newTemplate_renames._2().getOrDefault("r", null)))
                                    .collect(Collectors.toList());
                            result = getMatch(name_template.map(x -> String.format("%s--%s", var_values.getKey(), x), x -> tryTemplate), m,
                                    source, recurssiveTVs_Temp);
                            if (result.isPresent()) break;
                        }
                    }
                    if (result.isPresent()) break;
                }
            }
            if (result.isPresent()) break;
        }
        return result;
    }
}
