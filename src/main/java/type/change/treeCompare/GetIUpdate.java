package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CaptureMappingsLike;
import Utilities.CombyUtils;
import Utilities.HttpUtils;
import Utilities.RMinerUtils.TypeChange;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.Gson;
import com.jasongoodwin.monads.Try;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import type.change.comby.CombyMatch;
import type.change.comby.Environment;
import type.change.comby.Match;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utilities.ASTUtils.getChildren;
import static Utilities.ASTUtils.getCoveringNode;

import static Utilities.CaptureMappingsLike.SYNTACTIC_TYPE_CHANGES;
import static Utilities.CombyUtils.*;
import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.*;

public class GetIUpdate {


    private Map<Tuple2<Integer, Integer>, Optional<Tuple3<String, String, Match>>> matchesB4;
    private Map<Tuple2<Integer, Integer>, Optional<Tuple3<String, String, Match>>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;

    public GetIUpdate(CodeMapping codeMapping, TypeChange typeChange) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();

    }

    public static Optional<Tuple2<String, String>> getResolvedTypeChangeTemplate(Tuple2<String, String> reportedTypeChange, List<TypeChange> typeChanges) {

        Function<String, Optional<Tuple3<String, String, Match>>> matchType = t -> SYNTACTIC_TYPE_CHANGES.entrySet().stream()
                        .flatMap(x -> getPerfectMatch(x.getValue(), t, ".xml")
                                .map(y -> Tuple.of(x.getKey(), x.getValue()._1(), y.getMatches().get(0)))
                                .stream())
                        .findFirst();

        var matchedTypeSyntax = reportedTypeChange.map(matchType, matchType);

        if(matchedTypeSyntax._1().isEmpty() || matchedTypeSyntax._2().isEmpty())
            return Optional.empty();

        Explanation expl = new Explanation(matchedTypeSyntax._1().get(), matchedTypeSyntax._2().get());
        Tuple2<String, String> enrichedMatchReplace = tryToresolveTypes(expl, typeChanges);

        if(enrichedMatchReplace._1().contains(":[") && !enrichedMatchReplace._2().contains(":[") )
            System.out.println();

        if(!enrichedMatchReplace._1().contains(":[") && enrichedMatchReplace._2().contains(":[") )
            System.out.println();

        System.out.println(reportedTypeChange + " -> " + enrichedMatchReplace);
        return Optional.ofNullable(enrichedMatchReplace);
    }

    private static Tuple2<String, String> tryToresolveTypes(Explanation expl, List<TypeChange> typeChanges) {
        Tuple2<String, String> matchReplace = expl.getMatchReplace();
        Map<String, String> tvMapB4 = expl.getTvMapB4()
                .entrySet().stream().filter(x -> !expl.getMatchedTemplateVariables().containsKey(x.getKey()))
                .filter(x -> matchReplace._1().contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s->true), x.getValue(), null).isPresent())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> tvMapAfter = expl.getTvMapAfter()
                .entrySet().stream().filter(x -> !expl.getMatchedTemplateVariables().containsValue(x.getKey()))
                .filter(x -> matchReplace._2().contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s->true), x.getValue(), null).isPresent())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<Boolean, List<String>> relevantImportsB4 = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getRemovedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));



        Map<String, String> c1 = tvMapB4.entrySet().stream()
                .map(x -> Tuple.of(x.getValue(), resolveType(x, relevantImportsB4)))
                .filter(x -> x._2().isPresent())
                .map(x -> x.map2(y -> y.get()))
                .collect(toMap(x -> x._1(), x -> x._2()));

        Map<Boolean, List<String>> relevantImportsAfter = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getAddedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

        Map<String, String> c2 = tvMapAfter.entrySet().stream()
                .map(x -> Tuple.of(x.getValue(), resolveType(x, relevantImportsAfter)))
                .filter(x -> x._2().isPresent())
                .map(x -> x.map2(y -> y.get()))
                .collect(toMap(x -> x._1(), x -> x._2()));

        Tuple2<String, String> s = expl.getMatchReplace().map(x -> performRenameIdentifier(x, c1), x -> performRenameIdentifier(x, c2));

        return s;

    }


    public static String performRenameIdentifier(String source, Map<String, String> renameMapping){
        String curr = source;
        for(var e: renameMapping.entrySet()){
            curr = performIdentifierRename(e.getValue(), e.getKey(), source);
        }
        return curr;
    }


    public static Optional<String> resolveType(Map.Entry<String, String> b, Map<Boolean, List<String>> relevantImports) {
        return isPrimitive(b).or(() -> relevantImports.getOrDefault(true, new ArrayList<>()).stream()
                                .filter(x -> x.endsWith("." + b.getValue())).findFirst())
                .or(() -> findInBuiltInJava(relevantImports.getOrDefault(false, new ArrayList<>()), b));
    }

    public static class ResolveResponse {
        public String QualifiedName;
    }

    private static Optional<String> findInBuiltInJava(List<String> packages, Map.Entry<String, String> b) {
        Optional<String> response = HttpUtils.makeHttpRequest(Map.of("purpose", "Resolve", "lookup", b.getValue(), "packages",
                String.join(",", packages)))
                .map(x -> new Gson().fromJson(x, ResolveResponse.class).QualifiedName)
                .filter(x -> !x.isEmpty());
        return response;
    }

    private static Optional<String> isPrimitive(Map.Entry<String, String> b) {
        return SYNTACTIC_TYPE_CHANGES.entrySet().stream().filter(x -> x.getKey().startsWith("prim"))
                .flatMap(x -> getPerfectMatch(x.getValue(), b.getValue(), null)
                        .map(y -> b.getValue()).stream()).findFirst();
    }

    public IUpdate getUpdate(ASTNode before, ASTNode after, ITree root1, ITree root2) {

        if (root1 == null || root2 == null) return new NoUpdate();

        if (!root1.hasSameType(root2))
            System.out.println();

        AbstractExplanation explanation = before instanceof Expression && after instanceof Expression ?
                getInstance(before.toString(), after.toString(), Tuple.of(root1.getPos(), root1.getEndPos())
                            , Tuple.of(root2.getPos(), root2.getEndPos()))
                : new NoExplanation();

        Update upd = new Update(root1, root2, before.toString(), after.toString(),explanation, codeMapping, typeChange);

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
                Explanation expl = (Explanation) upd.getExplanation();
                List<Tuple2<String, String>> unMappedTVB4 = expl.getUnMappedTVB4();
                List<Tuple2<String, String>> unMappedTVAfter = expl.getUnmappedTVAfter();
                if(unMappedTVB4.size()==unMappedTVAfter.size() && unMappedTVB4.size() == 1){
                    Optional<ASTNode> n1 = getCoveringNode(before, expl.getTvMapB4Range().get(unMappedTVB4.get(0)._1()));
                    Optional<ASTNode> n2 = getCoveringNode(after, expl.getTvMapAfterRange().get(unMappedTVAfter.get(0)._1()));
                    if(n1.isPresent() && n2.isPresent()){
                        if(!n1.get().toString().equals(before.toString()) && !n2.get().toString().equals(after.toString()))
                            upd.setSubUpdates(List.of(getUpdate(n1.get(), n2.get())));
                    }
                    else{
                        System.out.println("Could not find expr");
                        System.out.println(before.toString());
                        System.out.println(after.toString());
                    }
                }else{
                    if (unMappedTVB4.size() != unMappedTVAfter.size() || unMappedTVB4.size() != 0) {
                        System.out.println("Ow! Too many unmatched vars");
                        System.out.println(before.toString());
                        System.out.println(after.toString());
                    }
                }
            }
        }
        return upd;
    }

    public IUpdate getUpdate(ASTNode before, ASTNode after) {
        ITree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(TreeContext::getRoot).orElse(null);
        ITree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(TreeContext::getRoot).orElse(null);
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
            explanationAfter = getMatch(after);
        }
        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || (explanationBefore.get()._1().equals(explanationAfter.get()._1())
                && (explanationAfter.get()._1().equals("Identifier") || explanationAfter.get()._1().equals("ClassName")
        || explanationAfter.get()._1().equals("StringLiteral")) )) return new NoExplanation();

        return new Explanation(explanationBefore.get(), explanationAfter.get());
    }
    static Optional<Tuple3<String, String, Match>> getMatch(Tuple2<String, String> name_template, Match cm, String source,
                                                            List<String> recurssiveTVs) {
        LinkedHashMap<String, Tuple2<String, Predicate<String>>> patternsHeuristics = CaptureMappingsLike.PATTERNS_HEURISTICS;

        Optional<Tuple3<String, String, Match>> result = Optional.empty();

        if(isPerfectMatch(source, cm) && recurssiveTVs.isEmpty())
            return Optional.of(name_template.concat(Tuple.of(cm)));

        for (int i = 0, recurssiveTVsSize = recurssiveTVs.size(); i < recurssiveTVsSize; i++) {
            String tv = recurssiveTVs.get(i);
            Environment env = cm.getEnvironment().stream().filter(x -> x.getVariable().equals(tv)).findFirst().orElse(null);
            if(env == null)
                System.out.println();
            for (var var_values : patternsHeuristics.entrySet()) {

                Predicate<String> heuristic = var_values.getValue()._2();
                boolean tv_Val_matches = heuristic.test(env.getValue());
                boolean anyRemaining = !source.replace("\\\"", "\"").equals(cm.getMatched());
                boolean remainingMatches = anyRemaining && heuristic.test(source.replace("\\\"", "\"").replace(cm.getMatched(), ""));
                if (!tv_Val_matches && !remainingMatches) continue;
                Tuple2<String, Map<String, String>> newTemplate_renames = renameTemplateVariable(var_values.getValue()._1(), x -> tv + "x" + x);
                String tryTemplate = substitute(name_template._2(), tv, newTemplate_renames._1());
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



    /*
    pattern: 1= pattern name, 2= pattern with holes
     */
    static Optional<Tuple3<String, String, Match>> getMatch(String source) {
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
                .map(x-> Try.ofFailable(x::get).orElse(null))
                .filter(x->x!=null && x.isPresent())
                .map(Optional::get)
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
            return perfectMatch;

        Optional<Tuple3<String, String, Match>> currentMatch;

        for (var basicMatch : basicMatches) {
            List<Match> matches = basicMatch._2().getMatches().stream()
                    .sorted(Comparator.comparingInt(x -> x.getMatched().length()))
                    .collect(Collectors.toList());
            Collections.reverse(matches);
            for(var m: matches) { currentMatch = getMatch(basicMatch._1(), m, source,
                            basicMatch._1()._2().contains("r") ? List.of("r") : List.of());
                if (currentMatch.isPresent()) return currentMatch;

            }
        }
        return Optional.empty();
    }

    public static List<String> getAllTemplateVariableName(String template){

        List<String> allMatches = CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched().replace("\\\\", "\\"), z))))
                .map(t -> {
                    Environment x = t._2();
                    if (x.getValue().startsWith("[")) return x.getValue().substring(1, x.getValue().length() - 1);
                    else if (x.getValue().contains("~")) return x.getValue().substring(0, x.getValue().indexOf("~"));
                    else return x.getValue();
                })
                .collect(toList());
        return allMatches;

    }

}

