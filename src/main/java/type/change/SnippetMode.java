package type.change;

import Utilities.*;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.RMinerUtils;
import type.change.GenerateResolvedResponse.ResolvedResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static Utilities.ASTUtils.normalizeStrLit;
import static java.util.stream.Collectors.*;

public class SnippetMode {
    static void snippetMode(String[] args) throws IOException {

        Path outputFile = Path.of(args[2]);
        Path pathToResolvedCommits = Paths.get(args[3]);
        ResolveTypeUtil.allJavaClasses = new HashSet<>(Files.readAllLines(Paths.get(args[4])));
        ResolveTypeUtil.allJavaLangClasses = Files.readAllLines(Paths.get(args[5]))
                .stream().collect(toMap(x -> {
                            var spl = x.split("\\.");
                            return spl[spl.length - 1];
                        }
                        , x -> x, (a, b) -> a));
        Path pathToSnippets = Paths.get(args[6]);
        SnippetMappings sn = new Gson().fromJson(Files.readString(pathToSnippets), SnippetMappings.class);

        for (var snp: sn.sns){
            List<SnippetMappings.ChangesForCommit> ls = new ArrayList<>();
            if(snp.tc_.contains("slf4j"))
                continue;
            for(var cmt : snp.commits){

                ls.add(NormalizeSnippet(cmt.commit, pathToResolvedCommits, cmt, snp.tc_));
            }
            snp.commits = ls;
        }

        Map<String, List<List<List<InferredMappings>>>> futures = sn.sns.parallelStream()
                .map(snp -> Tuple.of(snp.tc_, snp.commits.stream()
                        .flatMap(cmt -> AnalyzeSnippet(cmt.commit, pathToResolvedCommits, cmt, snp.tc_).stream()).collect(toList())))
                .collect(groupingBy(x -> x._1(), collectingAndThen(toList(), ls -> ls.stream().flatMap(z -> z._2().stream()).collect(toList()))));

        var jsonStr = new Gson().toJson(futures);
        Files.write(outputFile, jsonStr.getBytes(), StandardOpenOption.CREATE);
    }

    public static SnippetMappings.ChangesForCommit NormalizeSnippet(String commit, Path pathToResolvedCommits, SnippetMappings.ChangesForCommit snp, String tc_) {
        Optional<ResolvedResponse> response = Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional()
                .map(x -> new Gson().fromJson(x, ResolvedResponse.class));

        List<RMinerUtils.TypeChange> allRefactorings = response.get().commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return snp;
        }
        Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

        for(var ba :snp.b4Aftrs){
            for(var m: ba.getMappings()){
                String stmtB4 = m.before;
                String stmtAftr = m.after;
                if (stmtB4==null || stmtAftr==null)
                    continue;
                for (Tuple2<String, String> rn : allRenames)
                    if (stmtB4.contains(rn._1()) && stmtAftr.contains(rn._2()))
                        stmtAftr = CombyUtils.performIdentifierRename(rn._1(), rn._2(), stmtAftr);
                m.after = stmtAftr;
            }
        }
        return snp;
    }


    public static List<List<List<InferredMappings>>> AnalyzeSnippet(String commit, Path pathToResolvedCommits, SnippetMappings.ChangesForCommit snp, String tc_) {
        Optional<ResolvedResponse> response = Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional()
                .map(x -> new Gson().fromJson(x, ResolvedResponse.class));

        List<RMinerUtils.TypeChange> allRefactorings = response.get().commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return new ArrayList<>();
        }
        Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

        var tc = Tuple.of(tc_.split("->")[0], tc_.split("->")[1]);

        List<List<List<InferredMappings>>> inferredMatchReplace = getAsCodeMapping(snp, tc_)
                .stream()
//                .filter(x -> x.getB4().contains("private Optional<Integer> cachedHashCode=Optional.empty();"))
                .map(xs -> xs.stream()
                        .filter(x -> x._1().getB4().contains("LinkedList<LoggingEvent>()"))
                        .map(x -> CommitMode.inferTransformation(x._1(), x._2(), allRenames, commit,"testPrj"))
                        .map(a -> a.stream().map(x -> new InferredMappings(tc, x)).collect(toList()))
                        .collect(toList()))
//                .map(x -> CommitMode.inferTransformation(x._1(), x._2(), allRenames, commit))
//                .map(updates -> updates.stream().map(a -> new InferredMappings(tc, a)).collect(toList()))
                .collect(toList());
        return inferredMatchReplace;

    }

    public static List<List<Tuple2<CodeMapping, RMinerUtils.TypeChange>>> getAsCodeMapping(SnippetMappings.ChangesForCommit cfc, String tc_) {
        return cfc.b4Aftrs.stream()
                .map(z -> z.getMappings().stream()
                        .filter(x -> x.before != null && x.after != null)
                        .map(x -> Tuple.of(CodeMapping.newBuilder()
                                        .setB4(normalizeStrLit(x.before, x.before, x.after))
                                        .setAfter(normalizeStrLit(x.after, x.before, x.after))
                                        .setUrlbB4("").setUrlAftr("")
                                        .setIsSame(x.before.equals(x.after)).build()
                                , getTypeChangeFor(x, tc_)))
                        .collect(toList()))
                .collect(toList());
    }

    private static RMinerUtils.TypeChange getTypeChangeFor(SnippetMappings.Mpng b4Aftr, String tc_) {
        var tc = Tuple.of(tc_.split("->")[0], tc_.split("->")[1]);
        return new RMinerUtils.TypeChange(b4Aftr.element, b4Aftr.element,
                ImmutablePair.of("SnippetCu", "SnippetCu"),
                ImmutablePair.of("SnippetCu", "SnippetCu"), tc._1(), tc._2(),
                null, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                "TypeChange");

    }

    private static String normalizeStringLiterals(String s) {
        return s.replaceAll("\".*\"", "");
    }
}
