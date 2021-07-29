package type.change;

import Utilities.*;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.refactoringminer.RMinerUtils;
import type.change.treeCompare.GetUpdate;
import type.change.treeCompare.Update;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static Utilities.ASTUtils.isNotWorthLearningOnlyStrings;
import static java.util.stream.Collectors.*;
import static org.eclipse.jdt.core.dom.ASTNode.nodeClassForType;
import static type.change.treeCompare.Update.getAllDescendants;

public class CommitMode {

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

//    public static Map<Tuple3<String, String, Tuple2<String, String>>, Set<Tuple2<String, String>>> performedAdaptations;
//
//    static {
//        try {
//            performedAdaptations = Stream.concat(Files.readAllLines(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/experiment.jsonl")).stream(),
//                    Files.readAllLines(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/experiment2.jsonl")).stream())
//                    .filter(x -> !x.isEmpty())
//                    .map(x -> new Gson().fromJson(x, InferredMappings.class))
//                    .map(x -> Tuple.of(x.getInstances().getOriginalCompleteBefore(), x.getInstances().getOriginalCompleteAfter(), x.getMatch(), x.getReplace(), x.getInstances().getNames()))
//                    .collect(groupingBy(x -> Tuple.of(x._1(), x._2(), x._5()), collectingAndThen(toList(), xs -> xs.stream()
//                            .map(x->Tuple.of(x._3(), x._4())).collect(toSet()))));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    static void commitMode(String[] args) throws IOException {

        Path inputFile = Path.of(args[1]);
        Path outputFile = Path.of(args[2]);
        Path pathToResolvedCommits = Paths.get(args[3]);
        ResolveTypeUtil.allJavaClasses = new HashSet<>(Files.readAllLines(Paths.get(args[4])));
        ResolveTypeUtil.allJavaLangClasses = Files.readAllLines(Paths.get(args[5]))
                .stream().collect(toMap(x -> {
                    var spl = x.split("\\.");
                    return spl[spl.length - 1];
                }, x -> x, (a, b) -> a));

        Path popularTypeChangesPath = Path.of(args[6]);

        List<Tuple2<String, String>> popularTypeChanges = Files.readAllLines(popularTypeChangesPath)
                                                .stream().map(x -> x.split("->")).map(x -> Tuple.of(x[0], x[1]))
                                                .collect(toList());

        Set<String> analyzedCommits = Files.exists(outputFile) ?
                Files.readAllLines(outputFile).stream()
                        .filter(x -> !x.isEmpty())
                        .map(x -> new Gson().fromJson(x, InferredMappings.class))
                        .map(i -> i.getInstance().getCommit())
                        .collect(toSet()) : new HashSet<>();

        var futures = Files.readAllLines(inputFile).stream().map(x -> x.split(","))
                .filter(x -> !analyzedCommits.contains(x[2]))
                .map(commit -> AnalyzeCommit(commit[0], commit[2], outputFile, pathToResolvedCommits, popularTypeChanges))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    public static CompletableFuture<Void> AnalyzeCommit(String repoName, String commit, Path outputFile, Path pathToResolvedCommits, List<Tuple2<String, String>> popularTypeChanges) {
        System.out.println("Analyzing : " + commit + " " + repoName);

//        if (!commit.equals("1104ca0fa49be07aeef3ee822d1cc1ecc6b598b5"))
//            return new CompletableFuture<>();

        return CompletableFuture.supplyAsync(() -> //Either.right(HttpUtils.makeHttpRequest(HttpUtils.getRequestFor(repoName, repoCloneURL, commit)))
                Either.right(Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional())
                        .filterOrElse(Optional::isPresent, x -> "REFACTORING MINER RESPONSE IS EMPTY !!!!! ").map(Optional::get))
                .thenApply(response -> response.map(x -> new Gson().fromJson(response.get(), GenerateResolvedResponse.ResolvedResponse.class))
                        .filterOrElse(r -> r != null && r.commits != null, r -> "REFACTORING MINER RESPONSE IS EMPTY !!!!! "))

                .thenCompose(response -> {
                    if (response.isEmpty()) {
                        System.out.println(response.getLeft());
                        return CompletableFuture.completedFuture(null);
                    }
                    List<RMinerUtils.TypeChange> allRefactorings = response.get().commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
                    if (allRefactorings.isEmpty()) {
                        System.out.println("No Refactorings found!");
                        return CompletableFuture.completedFuture(null);
                    }
                    Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                            .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                            .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                            .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

                    Map<Tuple2<String, String>, Tuple2<String, String>> typeChange_template = response.get().getResolvedTypeChanges().stream()
                            .collect(toMap(Tuple2::_1, Tuple2::_2));

                    CompletableFuture[] xx = allRefactorings.stream()
                            .filter(x -> x.getRefactoringKind().contains("TYPE"))
                            .filter(typeChange -> typeChange_template.containsKey(Tuple.of(typeChange.getB4Type(), typeChange.getAfterType())))
                            .flatMap(typeChange -> {
                                Tuple2<String, String> typeChangeStr = Tuple.of(typeChange.getB4Type(), typeChange.getAfterType());
                                if (!popularTypeChanges.contains(typeChange_template.get(typeChangeStr)))
                                    return Stream.empty();
                                System.out.println(typeChange_template.get(typeChangeStr));
                                return getAsCodeMapping(typeChange).stream().filter(x -> !isNotWorthLearningOnlyStrings(x))
//                                        .filter(x -> false)
//                                        .filter(x->x.getB4().contains("for(") && typeChangeStr._1().contains("List") && typeChangeStr._2().contains("Set"))
//                                        || x.getAfter().contains("Typeface.createFromResources(config,mAssets,file)"))
                                        .map(codeMapping -> CompletableFuture.supplyAsync(() -> inferTransformation(codeMapping, typeChange, allRenames, commit, repoName))
                                                .thenApply(updates -> updates.stream().map(a -> new Gson()
                                                        .toJson(new InferredMappings(typeChange_template.get(typeChangeStr), a), InferredMappings.class))
                                                        .collect(joining("\n")))
                                                .thenAccept(inferredMapping -> RWUtils.FileWriterSingleton.inst.getInstance()
                                                        .writeToFile(inferredMapping, outputFile)));
                            }).toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(xx);
                });
    }

    public static Stream<String> getAllStringLiterals(String s) {
//        boolean valid = input.matches("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");

        Pattern pattern = Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");

        Matcher matcher = pattern.matcher(s);
        return matcher.results().map(x -> x.group());
    }

    public static List<CodeMapping> getAsCodeMapping(RMinerUtils.TypeChange tc) {
        return tc.getReferences().stream().map(sm -> CodeMapping.newBuilder()
                .setB4(ASTUtils.normalizeStrLit(sm.getBeforeStmt(), sm.getBeforeStmt(), sm.getAfterStmt()))
                .setAfter(ASTUtils.normalizeStrLit(sm.getAfterStmt(), sm.getBeforeStmt(), sm.getAfterStmt()))
                .setIsSame(sm.isSimilar())
                .addAllReplcementInferred(sm.getReplacements().stream()
                        .map(x -> TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred.newBuilder()
                                .setReplacementType(x).build())
                        .collect(toList()))
                .build())
                .collect(toList());
    }


    // ee729dcafee513affb8482a375ff119e525a1001 commit with the stirng update for file to path

    static List<Update> inferTransformation(CodeMapping codeMapping, RMinerUtils.TypeChange typeChange, Set<Tuple2<String, String>> otherRenames, String commit, String repoName) {

        List<Update> explainableUpdates = new ArrayList<>();
        String stmtB4 = codeMapping.getB4();
        String nameB4 = typeChange.getBeforeName();
        String nameAfter = typeChange.getAfterName();

        String stmtAftr = codeMapping.getAfter();
        if (!typeChange.getRefactoringKind().equals("Change Return Type") && !typeChange.getRefactoringKind().equals("CHANGE_RETURN_TYPE")) {
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
                codeMapping.getReplcementInferredList().stream().map(TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred::getReplacementType).collect(joining(" "))));

        // If the number of tokens are too large skip
        NodeCounter nc = new NodeCounter();
        stmt_b.get().accept(nc);
        if (nc.getCount() > 50) {
            LOGGER.info("TOO LARGE!!!");
            return explainableUpdates;
        }

        System.out.println(String.join("\n->\n", stmtB4, stmtAftr));

        GetUpdate gu = new GetUpdate(codeMapping, typeChange, commit, repoName);
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
