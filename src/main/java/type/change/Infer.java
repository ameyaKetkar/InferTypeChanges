package type.change;

import Utilities.*;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import logging.MyLogger;
import org.refactoringminer.RMinerUtils.Response;
import org.refactoringminer.RMinerUtils.TypeChange;
import type.change.treeCompare.GetUpdate;
import type.change.treeCompare.Update;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static Utilities.ASTUtils.isNotWorthLearning;
import static Utilities.ResolveTypeUtil.getResolvedTypeChangeTemplate;
import static java.util.stream.Collectors.*;
import static org.eclipse.jdt.core.dom.ASTNode.nodeClassForType;
import static org.refactoringminer.RMinerUtils.generateUrl;
import static type.change.treeCompare.Update.getAllDescendants;


public class Infer {


    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    public static void main(String[] args) throws IOException {
        MyLogger.setup();
        Path inputFile = Path.of(args[0]);
        Path outputFile  = Path.of(args[1]);

        Set<String> analyzedCommits = Files.exists(outputFile) ?
                Files.readAllLines(outputFile).stream()
                        .filter(x -> !x.isEmpty())
                        .map(x -> new Gson().fromJson(x, InferredMappings.class))
                        .map(i -> i.getInstances().getCommit())
                        .collect(toSet()) : new HashSet<>();

        CompletableFuture[] futures = Files.readAllLines(inputFile).stream().map(x -> x.split(","))
                .filter(x -> !analyzedCommits.contains(x[2]))
                .map(commit -> AnalyzeCommit(commit[0], commit[1], commit[2], outputFile))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();

    }


    public static CompletableFuture<Void> AnalyzeCommit(String repoName, String repoCloneURL, String commit, Path outputFile) {

//        if(!commit.startsWith("f6ca9e9025"))
//            return CompletableFuture.completedFuture(null);

        return CompletableFuture.supplyAsync(() -> Either.right(HttpUtils.makeHttpRequest(HttpUtils.getRequestFor(repoName, repoCloneURL, commit)))
                .filterOrElse(Optional::isPresent, x-> "REFACTORING MINER RESPONSE IS EMPTY !!!!! ").map(Optional::get))
                .thenApply(response -> response.map(x -> new Gson().fromJson(response.get(), Response.class))
                        .filterOrElse(r -> r != null && r.commits != null,r -> "REFACTORING MINER RESPONSE IS EMPTY !!!!! "))
                .thenApply(response -> response.map(r -> r.commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList())))
                .thenApply(x -> x.filterOrElse(z -> z.size() > 0, z ->"REFACTORING MINER RESPONSE IS EMPTY !!!!! "))
                .thenCompose(allRefactorings -> {
                    if (allRefactorings.isEmpty()) {
                        System.out.println(allRefactorings.getLeft());
                        return CompletableFuture.completedFuture(null);
                    }
                    Set<Tuple2<String, String>> allRenames = allRefactorings.get().stream()
                            .filter(x -> x.getB4Type() != null)
                            .filter(x -> !x.getReferences().stream().allMatch(y -> y.getBeforeStmt().contains("return ")))
                            .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                            .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

                    // Reported Type Change * MatchReplace templates
                    Map<Tuple2<String, String>, Tuple2<String, String>> typeChange_template = allRefactorings.get().stream().filter(x -> x.getB4Type() != null)
                            .collect(groupingBy(r -> Tuple.of(r.getB4Type(), r.getAfterType())))
                            .entrySet().stream()
                            .flatMap(x -> getResolvedTypeChangeTemplate(x.getKey(), x.getValue()).stream().map(t -> Tuple.of(x.getKey(), t)))
                            .collect(toMap(Tuple2::_1, Tuple2::_2));

                    CompletableFuture[] xx = allRefactorings.get().stream().filter(x -> x.getB4Type() != null).flatMap(rfctr -> {
                        Tuple2<String, String> typeChange = Tuple.of(rfctr.getB4Type(), rfctr.getAfterType());
                        if (!typeChange_template.containsKey(typeChange)) {
                            System.out.println("COULD NOT CAPTURE THE TYPE CHANGE PATTERN FOR " + rfctr.getB4Type() + "    " + rfctr.getAfterType());
                            return Stream.empty();
                        }
                        return getAsCodeMapping(repoCloneURL, rfctr, commit).stream().filter(x -> !isNotWorthLearning(x))
//                                .filter(x -> x.getAfter().contains("classesDir.resolve(endpointPath);"))
                                .map(x -> CompletableFuture.supplyAsync(() -> inferTransformation(x, rfctr, allRenames, commit))
                                        .thenApply(ls -> ls.stream().map(a -> new Gson()
                                                .toJson(new InferredMappings(typeChange_template.get(typeChange), a), InferredMappings.class))
                                                .collect(joining("\n")))
                                        .thenAccept(s -> RWUtils.FileWriterSingleton.inst.getInstance()
                                                .writeToFile(s, outputFile)));
                    }).toArray(CompletableFuture[]::new);

                    return CompletableFuture.allOf(xx);
                });
    }

    public static List<CodeMapping> getAsCodeMapping(String url, TypeChange tc, String commit) {
        return tc.getReferences().stream().map(sm -> CodeMapping.newBuilder().setB4(sm.getBeforeStmt())
                .setAfter(sm.getAfterStmt()).setIsSame(sm.isSimilar())
                .addAllReplcementInferred(sm.getReplacements().stream()
                        .map(x -> ReplacementInferred.newBuilder().setReplacementType(x).build())
                        .collect(toList()))
                .setUrlbB4(generateUrl(sm.getLocationInfoBefore(), url, commit, "L"))
                .setUrlAftr(generateUrl(sm.getLocationInfoAfter(), url, commit, "R")).build())
                .collect(toList());
    }

    private static List<Update> inferTransformation(CodeMapping codeMapping, TypeChange typeChange, Set<Tuple2<String, String>> otherRenames, String commit) {

        List<Update> explainableUpdates = new ArrayList<>();
        String stmtB4 = codeMapping.getB4();
        String nameB4 = typeChange.getBeforeName();
        String nameAfter = typeChange.getAfterName();
        //.filter(x -> !x.getReferences().stream().allMatch(y -> y.getBeforeStmt().contains("return ")))

        String stmtAftr = codeMapping.getAfter();
        if (typeChange.getReferences().size() == 0 || !typeChange.getReferences().stream().allMatch(y -> y.getBeforeStmt().contains("return "))) {
            stmtAftr = CombyUtils.performIdentifierRename(nameB4, nameAfter, codeMapping.getAfter());
        }
        for (Tuple2<String, String> rn : otherRenames)
            if (stmtB4.contains(rn._1()) && stmtAftr.contains(rn._2()))
                stmtAftr = CombyUtils.performIdentifierRename(rn._1(), rn._2(), stmtAftr);

        var stmt_b = ASTUtils.getStatement(stmtB4.replace("\n", ""));
        var stmt_a = ASTUtils.getStatement(stmtAftr.replace("\n", ""));

        if (stmt_a.isEmpty() || stmt_b.isEmpty())
            return new ArrayList<>();

        LOGGER.info(String.join("\n", nameB4 + " -> " + nameAfter,
                String.join(",", nodeClassForType(stmt_b.get().getNodeType()).toString(), stmtB4.replace("\n", "")),
                String.join(",", nodeClassForType(stmt_a.get().getNodeType()).toString(), stmtAftr.replace("\n", "")),
                codeMapping.getReplcementInferredList().stream().map(ReplacementInferred::getReplacementType).collect(joining(" "))));

        // If the number of tokens are too large skip
        NodeCounter nc = new NodeCounter();
        stmt_b.get().accept(nc);
        if (nc.getCount() > 50) {
            LOGGER.info("TOO LARGE!!!");
            return explainableUpdates;
        }
        System.out.println("Analyzing : " + commit);
        System.out.println(String.join("\n->\n", stmtB4, stmtAftr));

        GetUpdate gu = new GetUpdate(codeMapping, typeChange, commit);
        Update upd = gu.getUpdate(stmt_b.get(), stmt_a.get());

        if (upd == null) {
            LOGGER.info("NO UPDATE FOUND!!!");
            return explainableUpdates;
        }

        explainableUpdates = Stream.concat(Stream.of(upd), getAllDescendants(upd))
                .filter(i -> i.getMatchReplace().isPresent() && i.getAsInstance().isRelevant())
                .collect(toList());

        if (explainableUpdates.isEmpty())
            LOGGER.info("NO EXPLAINABLE UPDATE FOUND!!!");

        for (var expln : explainableUpdates)
            System.out.println(expln.getMatchReplace().get().getMatchReplace().toString());

        System.out.println("----------");

        return explainableUpdates;
    }


}
