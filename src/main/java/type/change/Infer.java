package type.change;

import Utilities.*;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred;
import io.vavr.Tuple;
import io.vavr.Tuple2;
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
        Path outputFile = Path.of(args[1]);
        Set<String> analyzedCommits = Files.exists(outputFile)?
                Files.readAllLines(outputFile).stream()
                .filter(x -> !x.isEmpty())
                .map(x -> new Gson().fromJson(x, InferredMappings.class))
                .map(i->i.getInstances().getCommit())
                .collect(toSet()): new HashSet<>();

        CompletableFuture[] futures = Files.readAllLines(inputFile).stream().map(x -> x.split(","))
                .filter(x -> !analyzedCommits.contains(x[2]))
                .flatMap(commit -> AnalyzeCommit(commit[0], commit[1], commit[2], outputFile))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    public static Stream<CompletableFuture<Void>> AnalyzeCommit(String repoName, String repoClonURL, String commit, Path outputFile) {

        if(!commit.startsWith("6f26cd"))
            return Stream.empty();

        System.out.println("Analyzing commit " + commit + " " + repoName);
        // Call Refactoring Miner
        var response = HttpUtils.makeHttpRequest(Map.of("purpose", "RMiner",
                "commit", commit, "project", repoName, "url", repoClonURL));

        if (response.isEmpty()) {
            System.out.println("REFACTORING MINER RESPONSE IS EMPTY !!!!! ");
            return Stream.empty();
        }

        Response response1 = new Gson().fromJson(response.get(), Response.class);

        // All the collected refactorings
        List<TypeChange> allRefactorings = response1.commits.stream().flatMap(x -> x.refactorings.stream())
//                .filter(x -> x.getReferences()!= null && x.getReferences().stream()
//                        .anyMatch(y -> y.getBeforeStmt().contains("new FileResourceManager(newSymlink,10485760,true,rootPath.getAbsolutePath().concat(\"/otherDir\"))")))
                .collect(toList());

        // All the reported renames
        Set<Tuple2<String, String>> allRenames = allRefactorings.stream().filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());


        // 5299
        // Reported Type Change * MatchReplace templates
        Map<Tuple2<String, String>, Tuple2<String, String>> typeChange_template = allRefactorings.stream().filter(x -> x.getB4Type() != null)
                .collect(groupingBy(r -> Tuple.of(r.getB4Type(), r.getAfterType())))
                .entrySet().stream()
                .flatMap(x -> getResolvedTypeChangeTemplate(x.getKey(), x.getValue()).stream().map(t->Tuple.of(x.getKey(), t)))
                .collect(toMap(Tuple2::_1, Tuple2::_2));

         return allRefactorings.stream().filter(x -> x.getB4Type() != null).flatMap(rfctr -> {
             Tuple2<String, String> typeChange = Tuple.of(rfctr.getB4Type(), rfctr.getAfterType());

             if(!typeChange_template.containsKey(typeChange)) {
                System.out.println("COULD NOT CAPTURE THE TYPE CHANGE PATTERN FOR " + rfctr.getB4Type()
                        + "    " + rfctr.getAfterType());
                return Stream.empty();
            }

             return getAsCodeMapping(repoClonURL, rfctr, commit).stream().filter(x -> !isNotWorthLearning(x))
                    .map(x -> CompletableFuture.supplyAsync(() -> inferTransformation(x, rfctr, allRenames, commit))
                            .thenApply(ls -> ls.stream().map(a -> new Gson()
                                    .toJson(new InferredMappings(typeChange_template.get(typeChange), a), InferredMappings.class))
                                    .collect(joining("\n")))
                            .thenAccept(s -> RWUtils.FileWriterSingleton.inst.getInstance()
                                    .writeToFile(s, outputFile)));
        });

    }

    public static List<CodeMapping> getAsCodeMapping(String url, TypeChange tc, String commit) {
        return tc.getReferences().stream().map(sm -> CodeMapping.newBuilder().setB4(sm.getBeforeStmt())
                .setAfter(sm.getAfterStmt()).setIsSame(sm.isSimilar())
                .addAllReplcementInferred(sm.getReplacements().stream()
                        .map(x -> ReplacementInferred.newBuilder().setReplacementType(x).build())
                        .collect(toList()))
                .setUrlbB4(generateUrl(sm.getLocationInfoBefore(), url,commit, "L"))
                .setUrlAftr(generateUrl(sm.getLocationInfoAfter(), url,commit, "R")).build())
                .collect(toList());
    }

    private static List<Update> inferTransformation(CodeMapping codeMapping, TypeChange typeChange, Set<Tuple2<String, String>> otherRenames, String commit) {

        List<Update> explainableUpdates = new ArrayList<>();
        String stmtB4 = codeMapping.getB4();
        String nameB4 = typeChange.getBeforeName();
        String nameAfter = typeChange.getAfterName();


        String stmtAftr = CombyUtils.performIdentifierRename(nameB4, nameAfter, codeMapping.getAfter());
        for (Tuple2<String, String> rn : otherRenames)
            if (stmtB4.contains(rn._1()) && stmtAftr.contains(rn._2()))
                stmtAftr = CombyUtils.performIdentifierRename(rn._1(), rn._2(), codeMapping.getAfter());

        var stmt_b = ASTUtils.getStatement(stmtB4.replace("\n", ""));
        var stmt_a = ASTUtils.getStatement(stmtAftr.replace("\n", ""));

        if(stmt_a.isEmpty() || stmt_b.isEmpty())
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
        GetUpdate gu = new GetUpdate(codeMapping, typeChange);
        Optional<Update> upd = gu.getUpdate(stmt_b.get(), stmt_a.get());
        if (upd.isEmpty()) {
            LOGGER.info("NO UPDATE FOUND!!!");
            return explainableUpdates;
        }

        explainableUpdates = Stream.concat(Stream.of(upd.get()), getAllDescendants(upd.get()))
                .filter(i -> i.getExplanation().isPresent())
                .collect(toList());

        if (explainableUpdates.isEmpty())
            LOGGER.info("NO EXPLAINABLE UPDATE FOUND!!!");

        for (var expln : explainableUpdates)
            System.out.println(expln.getExplanation().get().getMatchReplace().toString());
        
        System.out.println("----------");

        return explainableUpdates;
    }


//    public static List<Update> explainableUpdates(Update u) {
//        // Collect all updates!
//        Collection<Update> allUpdates = Stream.concat(Stream.of(u), getAllDescendants(u))
//                .filter(i -> i.getExplanation().isPresent())
//                .collect(toList());
//
////        allUpdates = allUpdates.stream().collect(groupingBy(x -> Tuple.of(Tuple.of(x.getBefore().getPos(), x.getBefore().getEndPos()), Tuple.of(
////                x.getAfter().getPos(), x.getAfter().getEndPos())), collectingAndThen(toList(), x -> x.stream().findFirst().get())))
////                .values();
//
//        if (allUpdates.size() == 1)
//            return new ArrayList<>(allUpdates);
//
//        // IF Descendants explain the change partially then remove descendants
//        // IF Descendants explain the change incorrectly then remove descendants
//        // IF descendants completely explain the change, then remove the ancestors
//        Map<Update, List<Update>> merges = new HashMap<>();
//        var removeRedundantUpdates = new ArrayList<Update>();
//        for (var i : allUpdates) {
//            List<Update> simplestDescendants = removeSubsumedEdits(getAllDescendants(i)
//                    .filter(x -> x.getExplanation().isPresent())
//                    .filter(allUpdates::contains)
//                    .collect(toList()));
//
//            if (applyUpdatesAndMatch(simplestDescendants, i.getBeforeStr(), i.getAfterStr())
//                    && !i.getAsInstance().isRelevant())
//                removeRedundantUpdates.add(i);
//            else
//                merges.put(i, simplestDescendants);
//        }
//
//        allUpdates = allUpdates.stream().filter(x -> !removeRedundantUpdates.contains(x)).collect(toList());
//
//        for (var upd : allUpdates) {
//            if (merges.containsKey(upd)) {
//                Tuple2<Update, List<Update>> candidates = Tuple.of(upd, merges.get(upd));
//                Optional<MatchReplace> e = candidates._1().getExplanation();
//                if(e.isEmpty()) continue;
//                for (var child : candidates._2()) {
//                    if(child.getExplanation().isEmpty()) continue;
//                    e = MatchReplace.mergeParentChildMatchReplace(e.get(), child.getExplanation().get());
//                    if (e.isEmpty())
//                        break;
//                    System.out.println();
//                }
//                if (e.isPresent()) {
//                    if(candidates._1().getExplanation().isPresent())
//                        if(e.get().getMatchReplace().equals(candidates._1().getExplanation().get().getMatchReplace()))
//                            continue;
//                    upd.setExplanation(e);
//                }else{
////                    removeRedundantUpdates.addAll(candidates._2());
//                }
//            }
//        }
//
//        return allUpdates.stream().filter(x -> !removeRedundantUpdates.contains(x)).collect(toList());
//    }


}
