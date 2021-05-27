package Utilities;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import type.change.treeCompare.MatchReplace;
import type.change.treeCompare.PerfectMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static Utilities.CaptureMappingsLike.SYNTACTIC_TYPE_CHANGES;
import static Utilities.CombyUtils.getPerfectMatch;
import static Utilities.CombyUtils.performIdentifierRename;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.refactoringminer.RMinerUtils.*;

public class ResolveTypeUtil {


    public static Optional<Tuple2<String, String>> getResolvedTypeChangeTemplate(Tuple2<String, String> reportedTypeChange, List<TypeChange> typeChanges) {

        Function<String, Optional<PerfectMatch>> matchType = t -> SYNTACTIC_TYPE_CHANGES.entrySet().stream()
                .flatMap(x -> getPerfectMatch(x.getValue(), t, ".xml")
                        .map(y -> new PerfectMatch(x.getKey(), x.getValue()._1(), y.getMatches().get(0)))
                        .stream())
                .findFirst();

        var matchedTypeSyntax = reportedTypeChange.map(matchType, matchType);

        if (matchedTypeSyntax._1().isEmpty() || matchedTypeSyntax._2().isEmpty())
            return Optional.empty();
        try {
            MatchReplace expl = new MatchReplace(matchedTypeSyntax._1().get(), matchedTypeSyntax._2().get(), "***");
            Tuple2<String, String> enrichedMatchReplace = tryToresolveTypes(expl, typeChanges);
            return Optional.of(enrichedMatchReplace);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Map<String, String> getTypeNames(Map<String, String> unMatched, String template){
        return unMatched.entrySet().stream()
                .filter(x -> template.contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(":[c~\\w+[?:\\.\\w+]+]", x.getValue(), null).isPresent())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Tuple2<String, String> tryToresolveTypes(MatchReplace expl, List<TypeChange> typeChanges) {

        Tuple2<String, String> matchReplace = expl.getMatchReplace();

        Map<String, String> tvMapB4 = getTypeNames(expl.getUnMatchedBefore(), matchReplace._1());
        Map<String, String> tvMapAfter = getTypeNames(expl.getUnMatchedAfter(), matchReplace._2());

        Map<Boolean, List<String>> relevantImportsB4 = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getRemovedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

        Map<String, String> c1 = resolveTypeNames(tvMapB4, relevantImportsB4);

        Map<Boolean, List<String>> relevantImportsAfter = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getAddedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

        Map<String, String> c2 = resolveTypeNames(tvMapAfter, relevantImportsAfter);

        Tuple2<String, String> s = expl.getMatchReplace().map(x -> performRenameIdentifier(x, c1), x -> performRenameIdentifier(x, c2));

        return s;

    }

    public static Map<String, String> resolveTypeNames(Map<String, String> tvMap, Map<Boolean, List<String>> relevantImports) {
        return tvMap.entrySet().stream()
                .map(x -> Tuple.of(x.getValue(), resolveType(x, relevantImports)))
                .filter(x -> x._2().isPresent())
                .map(x -> x.map2(y -> y.get()))
                .collect(toMap(x -> x._1(), x -> x._2(), (a, b) -> a));
    }

    private static String performRenameIdentifier(String source, Map<String, String> renameMapping) {
        String curr = source;
        for (var e : renameMapping.entrySet()) {
            curr = performIdentifierRename(e.getValue(), e.getKey(), curr);
        }
        return curr;
    }

    private static Optional<String> resolveType(Map.Entry<String, String> b, Map<Boolean, List<String>> relevantImports) {
        return isPrimitive(b).or(() -> relevantImports.getOrDefault(true, new ArrayList<>()).stream()
                .filter(x -> x.endsWith("." + b.getValue())).findFirst())
                .or(() -> findInBuiltInJava(relevantImports.getOrDefault(false, new ArrayList<>()), b));
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

    public static class ResolveResponse {
        public String QualifiedName;
    }
}
