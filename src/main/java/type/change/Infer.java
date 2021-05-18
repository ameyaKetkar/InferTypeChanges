package type.change;

import Utilities.*;
import Utilities.RMinerUtils.Response;
import Utilities.comby.Match;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import logging.MyLogger;
import type.change.treeCompare.*;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static Utilities.CombyUtils.*;
import static Utilities.RMinerUtils.*;

import static java.util.stream.Collectors.*;
import static org.eclipse.jdt.core.dom.ASTNode.nodeClassForType;
import static Utilities.ResolveTypeUtil.getResolvedTypeChangeTemplate;


public class Infer {


    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    public static void main(String[] args) throws IOException {
        ///Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output
        MyLogger.setup();
        Path inputFile = Path.of(args[0]);
        Path outputFile = Path.of(args[1]);
        CompletableFuture[] futures = Files.readAllLines(inputFile).stream().map(x -> x.split(","))
                .flatMap(commit -> AnalyzeCommit(commit[0], commit[1], commit[2], outputFile))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    public static Stream<CompletableFuture<Void>> AnalyzeCommit(String repoName, String repoClonURL, String commit, Path outputFile) {

        if(!commit.equals("e9b5effe30cf68820a3dfb00bf736a325313206b"))
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

                     // Restrict input to a particular statement mapping
                     .filter(c -> c.getB4().contains("this.baseDir=baseDir.getAbsoluteFile();"))

                    .map(x -> CompletableFuture.supplyAsync(() -> inferTransformation(x, rfctr, allRenames))
                            .thenApply(ls -> ls.stream().map(a -> new Gson()
                                    .toJson(new InferredMappings(typeChange_template.get(typeChange), a), InferredMappings.class))
                                    .collect(joining("\n")))
                            .thenAccept(s -> RWUtils.FileWriterSingleton.inst.getInstance()
                                    .writeToFile(s, outputFile)));
        });

    }
//https://github.com/Graylog2/graylog2-server/commit/dcf7c59ac0853401dd3aa9395653d674c7f14bd6?diff=split#diff-feabaeebdf2909064687134cc0dc3776R146

    private static List<Update> inferTransformation(CodeMapping codeMapping, TypeChange typeChange, Set<Tuple2<String, String>> otherRenames) {

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

        GetUpdate gu = new GetUpdate(codeMapping, typeChange);
        Optional<Update> upd = gu.getUpdate(stmt_b.get(), stmt_a.get());
        if (upd.isEmpty()) {
            LOGGER.info("NO UPDATE FOUND!!!");
            return explainableUpdates;
        }

        explainableUpdates = explainableUpdates(upd.get());

        if (explainableUpdates.isEmpty())
            LOGGER.info("NO EXPLAINABLE UPDATE FOUND!!!");

        for (var expln : explainableUpdates)
            System.out.println(expln.getExplanation().get().getMatchReplace().toString());
//            LOGGER.info((((Explanation) expln.getExplanation()).getMatchReplace()).toString());


        System.out.println("----------");

        return explainableUpdates;
    }


    public static List<Update> explainableUpdates(Update u) {
        // Collect all updates!
        Collection<Update> allUpdates = Stream.concat(Stream.of(u), Update.getAllDescendants(u))
                .filter(i -> i.getExplanation().isPresent())
                .collect(toList());

        allUpdates = allUpdates.stream().collect(groupingBy(x -> Tuple.of(Tuple.of(x.getBefore().getPos(), x.getBefore().getEndPos()), Tuple.of(
                x.getAfter().getPos(), x.getAfter().getEndPos())), collectingAndThen(toList(), x -> x.stream().findFirst().get())))
                .values();

        if (allUpdates.size() == 1)
            return new ArrayList<>(allUpdates);

        // IF Descendants explain the change partially then remove descendants
        // IF Descendants explain the change incorrectly then remove descendants
        // IF descendants completely explain the change, then remove the ancestors
        Map<Update, List<Update>> merges = new HashMap<>();
        var removeRedundantUpdates = new ArrayList<Update>();
        for (var i : allUpdates) {
            List<Update> allDescendants = Update.getAllDescendants(i)
                    .filter(x -> x.getExplanation().isPresent())
                    .filter(allUpdates::contains)
                    .collect(toList());

            List<Update> simplestDescendants = removeSubsumedEdits(allDescendants);
            boolean applyDesc = Update.applyUpdatesAndMatch(simplestDescendants, i.getBeforeStr(), i.getAfterStr());
            if (applyDesc) {
                if(!i.getAsInstance().isRelevant())
                    removeRedundantUpdates.add(i);
            }
            else {
                merges.put(i, simplestDescendants);
//                removeRedundantUpdates.addAll(i);
            }
        }

        allUpdates = allUpdates.stream().filter(x -> !removeRedundantUpdates.contains(x)).collect(toList());

        for (var upd : allUpdates) {
            if (merges.containsKey(upd)) {
                Tuple2<Update, List<Update>> candidates = Tuple.of(upd, merges.get(upd));
                Optional<MatchReplace> e = candidates._1().getExplanation();
                if(e.isEmpty()) continue;
                for (var child : candidates._2()) {
                    if(child.getExplanation().isEmpty()) continue;
                    e = mergeExplanations(e.get(), child.getExplanation().get());
                    if (e.isEmpty())
                        break;
                    System.out.println();
                }
                if (e.isPresent()) {
                    if(candidates._1().getExplanation().isPresent())
                        if(e.get().getMatchReplace().equals(candidates._1().getExplanation().get().getMatchReplace()))
                            continue;
                    upd.setExplanation(e.get());
                }
            }
        }

        return new ArrayList<>(allUpdates);
    }

    public static List<Update> removeSubsumedEdits(Collection<Update> allUpdates) {
        List<Update> subsumedEdits = new ArrayList<>();
        for (var i : allUpdates) {
            for (var j : allUpdates) {
                if (isSubsumedBy(i.getBefore(), j.getBefore()) || isSubsumedBy(i.getAfter(), j.getAfter())) {
                    subsumedEdits.add(j);
                }
            }
        }
        return allUpdates.stream().filter(x -> !subsumedEdits.contains(x)).collect(toList());
    }

    /**
     *
     * @param parent
     * @param child
     * @return Returns an explanation where the parent template is merged with the child template
     * The merge can happen iff one of the template variables in the parent template perfectly match the before
     * and after of the child explanation
     */
    public static Optional<MatchReplace> mergeExplanations(MatchReplace parent, MatchReplace child) {
        try {
            Optional<Tuple2<String, String>> b4 = parent.getMatch().getTemplateVariableMapping().entrySet().stream()
                    .filter(x -> x.getValue().replace("\\\"", "\"").equals(child.getCodeSnippetB4()))
                    .map(x -> Tuple.of(x.getKey(), x.getValue().replace("\\\"", "\"")))
                    .findFirst();

            Optional<Tuple2<String, String>> aftr = parent.getReplace().getTemplateVariableMapping().entrySet().stream()
                    .filter(x -> x.getValue().replace("\\\"", "\"").equals(child.getCodeSnippetAfter()))
                    .map(x -> Tuple.of(x.getKey(), x.getValue().replace("\\\"", "\"")))
                    .findFirst();


        if (b4.isPresent() && aftr.isPresent()) {
            Tuple2<String, Map<String, String>> newTemplate_renamesB4 = renameTemplateVariable(child.getMatchReplace()._1(), x -> b4.get()._1() + "x" + x);
            Tuple2<String, Map<String, String>> newTemplate_renamesAfter = renameTemplateVariable(child.getMatchReplace()._2(), x -> aftr.get()._1() + "x" + x);
            String mergedB4 = parent.getMatchReplace()._1().replace("\\\"", "\"").replace(b4.get()._2(), newTemplate_renamesB4._1());
            String mergedAfter = parent.getMatchReplace()._2().replace("\\\"", "\"").replace(aftr.get()._2(), newTemplate_renamesAfter._1());

            if (mergedB4.equals(parent.getMatchReplace()._1().replace("\\\"", "\""))
                    && mergedAfter.equals(parent.getMatchReplace()._2().replace("\\\"", "\"")))
                return Optional.of(parent);


            Optional<Match> newExplainationBefore = getMatch(mergedB4, parent.getCodeSnippetB4(), null)
                    .filter(x -> isPerfectMatch(parent.getCodeSnippetB4(), x))
                    .map(x -> x.getMatches().get(0));

            Optional<Match> newExplainationAfter = getMatch(mergedAfter, parent.getCodeSnippetAfter(), null)
                    .filter(x -> isPerfectMatch(parent.getCodeSnippetAfter(), x))
                    .map(x -> x.getMatches().get(0));
            if (newExplainationAfter.isPresent() && newExplainationBefore.isPresent())
                return Optional.of(new MatchReplace(new PerfectMatch(parent.getMatch().getName() + "----" + child.getMatch().getName(), mergedB4, newExplainationBefore.get()),
                        new PerfectMatch(parent.getReplace().getName() + "----" + child.getReplace().getName(), mergedAfter, newExplainationAfter.get())));
        }
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
        return Optional.empty();

    }
}
