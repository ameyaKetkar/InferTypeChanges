package type.change;

import Utilities.*;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.utilities.GitUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.eclipse.jgit.api.Git;
import org.refactoringminer.RMinerUtils;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import type.change.treeCompare.GetUpdate;
import type.change.treeCompare.Update;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utilities.ASTUtils.isNotWorthLearningOnlyStrings;
import static Utilities.ResolveTypeUtil.getResolvedTypeChangeTemplate;
import static java.util.stream.Collectors.*;
import static org.eclipse.jdt.core.dom.ASTNode.nodeClassForType;
import static type.change.GenerateResolvedResponse.*;
import static type.change.treeCompare.Update.getAllDescendants;

public class FullMode {

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static void fullMode(String[] args) throws IOException {
        Path inputFile = Path.of(args[1]);
        Path outputFolder = Path.of(args[2]);
        Path pathToCorpus = Paths.get(args[3]);
        ResolveTypeUtil.allJavaClasses = new HashSet<>(Files.readAllLines(Paths.get(args[4])));
        ResolveTypeUtil.allJavaLangClasses = Files.readAllLines(Paths.get(args[5]))
                .stream().collect(toMap(x -> {
                    var spl = x.split("\\.");
                    return spl[spl.length - 1];
                }, x -> x, (a, b) -> a));

//        Set<String> analyzedCommits = Files.exists(outputFile) ?
//                Files.readAllLines(outputFile).stream()
//                        .filter(x -> !x.isEmpty())
//                        .map(x -> new Gson().fromJson(x, InferredMappings.class))
//                        .map(i -> i.getInstances().getCommit())
//                        .collect(toSet()) : new HashSet<>();

        for (String x : Files.readAllLines(inputFile)) {
            String[] commit = x.split(",");
            try {
                fullyAnalyzeCommit(commit[0], commit[1], commit[2], outputFolder, pathToCorpus);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//                .toArray(CompletableFuture[]::new);
//        CompletableFuture.allOf(futures).join();
    }


    public static class RefHandler extends RefactoringHandler {
        private final String cloneURL;
        private final String currentCommitId;
        private ResolvedResponse resolvedResponse;

        public RefHandler(String cloneURL, String currentCommitId) {
            this.cloneURL = cloneURL;
            this.currentCommitId = currentCommitId;
        }
        @Override
        public void handle(String commitId, List<Refactoring> refactorings) {
            String jsonStr = RefactoringMiner.commitJSONString(cloneURL, currentCommitId, refactorings).toString();
            resolvedResponse = new Gson().fromJson(jsonStr, ResolvedResponse.class);
            var resolvedTypeChangeTemplate = resolvedResponse.commits.stream().flatMap(x -> x.refactorings.stream())
                    .filter(x -> x.getB4Type() != null)
                    .collect(groupingBy(r -> Tuple.of(r.getB4Type(), r.getAfterType())))
                    .entrySet().stream()
                    .flatMap(x -> getResolvedTypeChangeTemplate(x.getKey(), x.getValue()).stream().map(t -> Tuple.of(x.getKey(), t)))
                    .collect(Collectors.toList());
            resolvedResponse.setResolvedTypeChanges(resolvedTypeChangeTemplate);
        }

        public ResolvedResponse getResolvedResponse() {
            return resolvedResponse;
        }
    }


    public static Optional<ResolvedResponse> getRefactoringsJson(String repoName, String repoCloneURL, String commit, Path pathToCorpus){
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        Try<Git> repo = GitUtil.tryToClone(repoCloneURL, pathToCorpus.resolve("Project_" + repoName).resolve(repoName));
        if(repo.isSuccess()){
            RefHandler r = new RefHandler(repoCloneURL, commit);
            try {
                miner.detectAtCommit(repo.get().getRepository(), commit, r, 200);
            }catch (Exception e){
                e.printStackTrace();
            }
            return Optional.of(r.getResolvedResponse());
        }
        return Optional.empty();
    }

    public static void fullyAnalyzeCommit(String repoName, String repoCloneURL, String commit, Path outputFolder, Path pathToCorpus) throws IOException {
        System.out.println("Analyzing : " + commit + " " + repoName);

        Path commitFolder = outputFolder.resolve(commit);
        Path pathToRules = commitFolder.resolve("Rules.jsonl");

        ResolvedResponse resolvedResponse;
        if(!Files.exists(pathToRules)) {
            resolvedResponse = getRefactoringsJson(repoName, repoCloneURL, commit, pathToCorpus).orElse(null);
            if(!Files.exists(commitFolder)) {
                Files.createDirectory(commitFolder);
            }
            Files.write(commitFolder.resolve(commit+".json"), new Gson().toJson(resolvedResponse,ResolvedResponse.class).getBytes(),
                    StandardOpenOption.CREATE_NEW);
        }
        else{
            resolvedResponse = new Gson().fromJson(Files.readString(pathToRules), ResolvedResponse.class);
        }
        if (resolvedResponse == null)
            return;


        List<RMinerUtils.TypeChange> allRefactorings = resolvedResponse.commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return;
        }

        Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

        Map<Tuple2<String, String>, Tuple2<String, String>> typeChange_template = resolvedResponse.getResolvedTypeChanges().stream()
                .collect(toMap(Tuple2::_1, Tuple2::_2));

        String xx = allRefactorings.stream()
                .filter(x -> x.getRefactoringKind().contains("TYPE"))
                .filter(typeChange -> typeChange_template.containsKey(Tuple.of(typeChange.getB4Type(), typeChange.getAfterType())))
                .flatMap(typeChange -> {
                    Tuple2<String, String> typeChangeStr = Tuple.of(typeChange.getB4Type(), typeChange.getAfterType());
                    return getAsCodeMapping(repoCloneURL, typeChange, commit).stream().filter(x -> !isNotWorthLearningOnlyStrings(x))
//                                        .filter(x->x.getB4().contains("Typeface.createFromResources(config,mAssets,file)")
//                                        || x.getAfter().contains("Typeface.createFromResources(config,mAssets,file)"))
                            .map(codeMapping -> inferTransformation(codeMapping, typeChange, allRenames, commit))
                            .map(updates -> updates.stream().map(a -> new Gson()
                                    .toJson(new InferredMappings(typeChange_template.get(typeChangeStr), a), InferredMappings.class))
                                    .collect(joining("\n")));
                })
                .collect(joining("\n"));
        Files.write(pathToRules, xx.getBytes(), StandardOpenOption.CREATE_NEW );


    }

    public static CompletableFuture<Void> AnalyzeCommit(String repoName, String repoCloneURL, String commit, Path outputFile, Path pathToResolvedCommits) {
        System.out.println("Analyzing : " + commit + " " + repoName);



        return CompletableFuture.supplyAsync(() -> //Either.right(HttpUtils.makeHttpRequest(HttpUtils.getRequestFor(repoName, repoCloneURL, commit)))
                Either.right(Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional())
                        .filterOrElse(Optional::isPresent, x -> "REFACTORING MINER RESPONSE IS EMPTY !!!!! ").map(Optional::get))
                .thenApply(response -> response.map(x -> new Gson().fromJson(response.get(), ResolvedResponse.class))
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
                                return getAsCodeMapping(repoCloneURL, typeChange, commit).stream().filter(x -> !isNotWorthLearningOnlyStrings(x))
//                                        .filter(x->x.getB4().contains("Typeface.createFromResources(config,mAssets,file)")
//                                        || x.getAfter().contains("Typeface.createFromResources(config,mAssets,file)"))
                                        .map(codeMapping -> CompletableFuture.supplyAsync(() -> inferTransformation(codeMapping, typeChange, allRenames, commit))
                                                .thenApply(updates -> updates.stream().map(a -> new Gson()
                                                        .toJson(new InferredMappings(typeChange_template.get(typeChangeStr), a), InferredMappings.class))
                                                        .collect(joining("\n")))
                                                .thenAccept(inferredMapping -> RWUtils.FileWriterSingleton.inst.getInstance()
                                                        .writeToFile(inferredMapping, outputFile)));
                            }).toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(xx);
                });
    }

//    public static Stream<String> getAllStringLiterals(String s) {
////        boolean valid = input.matches("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");
//
//        Pattern pattern = Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");
//
//        Matcher matcher = pattern.matcher(s);
//        return matcher.results().map(x -> x.group());
//    }

    public static List<CodeMapping> getAsCodeMapping(String url, RMinerUtils.TypeChange tc, String commit) {

        return tc.getReferences().stream().map(sm -> CodeMapping.newBuilder()
                .setB4(ASTUtils.normalizeStrLit(sm.getBeforeStmt(), sm.getBeforeStmt(), sm.getAfterStmt()))
                .setAfter(ASTUtils.normalizeStrLit(sm.getAfterStmt(), sm.getBeforeStmt(), sm.getAfterStmt()))
                .setIsSame(sm.isSimilar())
                .addAllReplcementInferred(sm.getReplacements().stream()
                        .map(x -> TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred.newBuilder().setReplacementType(x).build())
                        .collect(toList()))
//                .setUrlbB4(generateUrl(sm.getLocationInfoBefore(), url, commit, "L"))
//                .setUrlAftr(generateUrl(sm.getLocationInfoAfter(), url, commit, "R"))
                .build())
                .collect(toList());
    }



    static List<Update> inferTransformation(CodeMapping codeMapping, RMinerUtils.TypeChange typeChange, Set<Tuple2<String, String>> otherRenames, String commit) {

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
