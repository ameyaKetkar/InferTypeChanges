package Utilities;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import type.change.treeCompare.MatchReplace;
import type.change.treeCompare.PerfectMatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static Utilities.CaptureMappingsLike.SYNTACTIC_TYPE_CHANGES;
import static Utilities.CombyUtils.getPerfectMatch;
import static Utilities.CombyUtils.performIdentifierRename;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.refactoringminer.RMinerUtils.*;

public class ResolveTypeUtil {

    public static Set<String> allJavaClasses;

    private static Map<String, String> allJavaLangClasses;

    static {
        try {
            allJavaClasses = new HashSet<>(Files.readAllLines(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/javaClasses.txt")));
            allJavaLangClasses = Files.readAllLines(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/javaLangClasses.txt"))
                    .stream().collect(toMap(x -> {
                        var spl = x.split("\\.");
                        return spl[spl.length -1 ];
                    }, x -> x));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Optional<Tuple2<String, String>> getResolvedTypeChangeTemplate(Tuple2<String, String> reportedTypeChange, List<TypeChange> typeChanges) {

        return Optional.of(reportedTypeChange)
                .map(x -> x.map(ResolveTypeUtil::toPerfectMatch, ResolveTypeUtil::toPerfectMatch))
                .filter(m -> m._1().isPresent() && m._2().isPresent())
                .flatMap(m -> Try.of(() -> new MatchReplace(m._1().get(), m._2().get(), "***"))
                    .onFailure(x -> {
                        System.out.println(reportedTypeChange);
                        x.printStackTrace();
                    }).toJavaOptional())
                .map(x -> tryToResolveTypes(x, typeChanges));
    }

    private static Optional<PerfectMatch> toPerfectMatch(String t) {
        return SYNTACTIC_TYPE_CHANGES.entrySet().stream()
                .flatMap(x -> getPerfectMatch(x.getValue(), t, ".xml")
                        .map(y -> new PerfectMatch(x.getKey(), x.getValue()._1(), y.getMatches().get(0)))
                        .stream())
                .findFirst();
    }

    private static Map<String, String> getTypeNames(Map<String, String> unMatched, String template){
        return unMatched.entrySet().stream()
                .filter(x -> template.contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(":[c~\\w+[?:\\.\\w+]+]", x.getValue(), null).isPresent())
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private static Tuple2<String, String> tryToResolveTypes(MatchReplace expl, List<TypeChange> typeChanges) {

        Map<String, String> tvMapB4 = getTypeNames(expl.getUnMatchedBefore(), expl.getMatchReplace()._1());
        Map<String, String> tvMapAfter = getTypeNames(expl.getUnMatchedAfter(), expl.getMatchReplace()._2());

        Map<Boolean, List<String>> relevantImportsB4 = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getRemovedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));
        Map<String, String> resolvedTypeNamesBefore = resolveTypeNames(tvMapB4, relevantImportsB4);

        Map<Boolean, List<String>> relevantImportsAfter = typeChanges.stream()
                .flatMap(typeChange -> Stream.concat(typeChange.getAddedImportStatements().stream(), typeChange.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));
        Map<String, String> resolvedTypeNamesAfter = resolveTypeNames(tvMapAfter, relevantImportsAfter);

        return expl.getMatchReplace().map(x -> performRenameIdentifier(x, resolvedTypeNamesBefore),
                x -> performRenameIdentifier(x, resolvedTypeNamesAfter));

    }

    public static Map<String, String> resolveTypeNames(Map<String, String> tvMap, Map<Boolean, List<String>> relevantImports) {
        return tvMap.entrySet().stream()
                .flatMap(x -> resolveType(x, relevantImports).stream().map(y -> Tuple.of(x.getValue(), y)))
                .collect(toMap(Tuple2::_1, Tuple2::_2, (a, b) -> a));
    }

    private static String performRenameIdentifier(String source, Map<String, String> renameMapping) {
        String curr = source;
        for (var e : renameMapping.entrySet()) {
            curr = performIdentifierRename(e.getValue(), e.getKey(), curr);
        }
        return curr;
    }

    private static Optional<String> resolveType(Entry<String, String> tv_value, Map<Boolean, List<String>> relevantImports) {
        return isPrimitive(tv_value).or(() -> relevantImports.getOrDefault(true, new ArrayList<>()).stream()
                .filter(x -> x.endsWith("." + tv_value.getValue())).findFirst())
                .or(() -> findInBuiltInJava(relevantImports.getOrDefault(false, new ArrayList<>()), tv_value));
    }

    private static Optional<String> findInBuiltInJava(List<String> packages, Entry<String, String> lookupEntry) {
        return Optional.ofNullable(allJavaLangClasses.getOrDefault(lookupEntry.getValue(), null))
                .or(() -> packages.stream().map(x -> x + "." + lookupEntry.getValue()).filter(x -> allJavaClasses.contains(x)).findFirst());
    }

    private static Optional<String> isPrimitive(Entry<String, String> b) {
        return SYNTACTIC_TYPE_CHANGES.entrySet().stream().filter(x -> x.getKey().startsWith("prim"))
                .flatMap(x -> getPerfectMatch(x.getValue(), b.getValue(), null)
                        .map(y -> b.getValue()).stream()).findFirst();
    }
}
