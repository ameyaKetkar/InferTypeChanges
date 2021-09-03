package type.change;

import Utilities.CombyUtils;
import Utilities.InferredMappings;
import Utilities.ResolveTypeUtil;
import Utilities.SnippetMappings;
import Utilities.SnippetMappings.ChangesForCommit;
import Utilities.comby.CombyRewrite;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.RMinerUtils.TypeChange;
import type.change.GenerateResolvedResponse.ResolvedResponse;
import type.change.treeCompare.Update;

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

        var content = Files.readString(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/popularRules.json"));
        Map<String, List<List<String>>> popularRules = (Map<String, List<List<String>>>) new Gson().fromJson(content, Map.class);


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

        for (var snp : sn.sns) {
            snp.commits = snp.commits.stream().map(cmt -> NormalizeSnippet(cmt.commit, pathToResolvedCommits, cmt, snp.tc_)).collect(toList());
        }


        Map<String, List<List<List<InferredMappings>>>> inferred_mappings = sn.sns.parallelStream()
                .map(snp -> Tuple.of(snp.tc_, snp.commits.stream()
                        .flatMap(cmt -> AnalyzeSnippet(cmt.commit, pathToResolvedCommits, cmt, snp.tc_, popularRules)
                                .stream()).collect(toList())))
                .collect(groupingBy(Tuple2::_1, collectingAndThen(toList(), ls -> ls.stream().flatMap(z -> z._2().stream())
                        .collect(toList()))));

        var jsonStr = new Gson().toJson(inferred_mappings);
        Files.write(outputFile, jsonStr.getBytes(), StandardOpenOption.CREATE);

    }

    public static ChangesForCommit NormalizeSnippet(String commit, Path pathToResolvedCommits, ChangesForCommit snp, String tc_) {
        Optional<ResolvedResponse> response = Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional()
                .map(x -> new Gson().fromJson(x, ResolvedResponse.class));

        List<TypeChange> allRefactorings = response.get()
                .commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());

        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return snp;
        }
        Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

        for (var ba : snp.b4Aftrs) {
            for (var m : ba.getMappings()) {
                String stmtB4 = m.before;
                String stmtAftr = m.after;
                if (stmtB4 == null || stmtAftr == null)
                    continue;
                for (Tuple2<String, String> rn : allRenames)
                    if (stmtB4.contains(rn._1()) && stmtAftr.contains(rn._2()))
                        stmtAftr = CombyUtils.performIdentifierRename(rn._1(), rn._2(), stmtAftr);
                m.after = stmtAftr;
            }
        }
        return snp;
    }


    public static boolean isSAme(String s1, String s2) {
        return s1.replace("\n", "").equals(s2.replace("\n", ""))
                || s1.replace("\n", "").equals(s2.replace("\n", "").replace("\'",""));
    }


    public static List<List<List<InferredMappings>>> AnalyzeSnippet(String commit, Path pathToResolvedCommits, ChangesForCommit snp, String tc_, Map<String, List<List<String>>> popularRules) {

        if (!commit.equals("20b05ffe6fb1a4e8f7b79f687b28fbe9fd34801f"))
            return new ArrayList<>();

        Path path_to_output_file = Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/Evaluation").resolve(commit + ".json");

//        if(Files.exists(path_to_output_file))
//            return new ArrayList<>();

        Optional<ResolvedResponse> response = Try.of(() -> Files.readString(pathToResolvedCommits.resolve(commit + ".json"))).toJavaOptional()
                .map(x -> new Gson().fromJson(x, ResolvedResponse.class));

        if (response.isEmpty())
            return new ArrayList<>();


//        if (!tc_.contains("Optional"))
//            return new ArrayList<>();

        if (tc_.contains("java.lang.Boolean"))
            System.out.println();


        List<TypeChange> allRefactorings = response.get().commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return new ArrayList<>();
        }
        Set<Tuple2<String, String>> allRenames = allRefactorings.stream()
                .filter(x -> x.getB4Type() != null && !x.getRefactoringKind().equals("CHANGE_RETURN_TYPE"))
                .filter(r -> !r.getBeforeName().equals(r.getAfterName()))
                .map(r -> Tuple.of(r.getBeforeName(), r.getAfterName())).collect(toSet());

        var tc = Tuple.of(tc_.split("->")[0], tc_.split("->")[1]);

        List<Tuple2<String, String>> relevant_popular_rules = popularRules.get(tc_).stream().map(x -> Tuple.of(x.get(0), x.get(1))).collect(toList());

        var type_change_patterns = response.get().getResolvedTypeChanges().stream().map(Tuple2::_1).collect(toList());

        // () Identify relevant popular rules (for the type change)
        // (i) A popular rewrite correctly transformed the code -- TRUE POSITIVE
        // (ii) No popular rule could transform the code correctly
        //           Some rules matched
        //           No popular rule even matched
        // (iii) Infer the rules between the real code mapping
        //            If rule is generated then add it to the current relevant popular rules
        //            Else no rule is generated
        //                   Check if the some rules that matched produced "correct" output - TRUE POSITIVE.
        //                      The code did not match in (ii) due to overlapping NA changes.
        //                   Check if the edit is NOT APPLICABLE
        //                   Check if it is a FALSE NEGATIVE



        List<EvaluationResult> results = new ArrayList<>();

        for (List<Tuple2<CodeMapping, TypeChange>> mapping_granularity : getAsCodeMapping(snp, tc_, allRenames)) {
            boolean correctRewrite = false;
            EvaluationResult evaluation = null;

            if(mapping_granularity.stream().noneMatch(z -> z._1().getB4().contains("new ChannelBuffer[]")))
                continue;

            for (Tuple2<CodeMapping, TypeChange> m : mapping_granularity) {
                if(isSAme(m._1().getB4(), m._1().getAfter()))
                    continue;
                String source = m._1().getB4();
                String target = m._1().getAfter();
                String name = m._2.getBeforeName();

                evaluation = new EvaluationResult(commit, tc_, Collections.singletonList(m._1()), name);

                // Apply Type Change patterns
                List<String> typeChangedSources = new ArrayList<>();
                typeChangedSources.add(source);
                for (var p : type_change_patterns) {
                    String matcher = p._1() + " ";
                    String rewrite = p._2() + " ";
                    Optional<CombyRewrite> output = CombyUtils.rewrite(matcher, rewrite, source);
                    if(output.isPresent()){
                        evaluation.addMatchedRule(matcher, rewrite, output.get().getRewrittenSource());
                        typeChangedSources.add(output.get().getRewrittenSource());
                        if(isSAme(output.get().getRewrittenSource(), target)){
                            evaluation.addCorrectRule(Tuple.of(matcher, rewrite));
                            evaluation.setStatus("TP");
                            correctRewrite = true;
                            break;
                        }
                    }
                }
                if (correctRewrite)
                    break;

                // Apply rewrite rules
                for (var src : typeChangedSources) {
                    for (var p : relevant_popular_rules) {
                        String matcher = p._1().replace("TCIVar", "TCIVar~" + name);
                        String rewrite = p._2();
                        //TODO: Remove the ~regex from rewrite template
                        // 042bf228c796f5fa7c5b4b96b94e0afd7b465538 : rename fileNAme -> file
                        var isReturn = src.contains("return ");
                        Optional<String> output;
                        if (isReturn) {
                            String rm = "return " + matcher + ";";
                            String rr = "return " + rewrite + ";";
                            output = CombyUtils.rewrite(rm, rr, src)
                                    .map(CombyRewrite::getRewrittenSource);
                            if (output.isPresent()) {
                                evaluation.addMatchedRule(p._1(), p._2(), output.get());
                                if (isSAme(output.get(), target)) {
                                    evaluation.addCorrectRule(Tuple.of(p._1(), p._2()));
                                    evaluation.setStatus("TP");
                                    correctRewrite = true;
                                    break;
                                }
                            }
                        }
                        output = CombyUtils.rewrite(matcher, rewrite, src).map(CombyRewrite::getRewrittenSource);
                        if (output.isPresent()) {
                            evaluation.addMatchedRule(matcher, rewrite, output.get());
                            if (isSAme(output.get(), target)) {
                                evaluation.addCorrectRule(Tuple.of(p._1(), p._2()));
                                evaluation.setStatus("TP");
                                correctRewrite = true;
                                break;
                            }
                        }
                    }
                    if (correctRewrite)
                        break;
                }
                if (correctRewrite)
                    break;
            }
            if (!correctRewrite) {
                List<CodeMapping> allCodeMappings = new ArrayList<>();
                String name = "";
                List<Tuple2<String, String>> inferredRules = new ArrayList<>();
                for (Tuple2<CodeMapping, TypeChange> m : mapping_granularity) {
                    if(isSAme(m._1().getB4(), m._1().getAfter()))
                        continue;
                    name = m._2().getBeforeName();
                    List<Update> rules = CommitMode.inferTransformation(m._1(), m._2(), allRenames, commit, "testPrj");
                    if (!rules.isEmpty()) {
                        allCodeMappings.add(m._1());
                        for (Update r : rules) {
                            if(!r.getAsInstance().isSafe())
                                continue;
                            Tuple2<String, String> rule = Tuple.of(r.getMatchReplace().get().getMatch().getTemplate(),
                                    r.getMatchReplace().get().getReplace().getTemplate());

                            if(!rule._1().equals(rule._2())) {

                                List<String> typeChangedSources = new ArrayList<>();
                                String source1 = m._1().getB4();
                                String target = m._1().getAfter();

                                typeChangedSources.add(source1);
                                for (var p : type_change_patterns) {
                                    String matcher = p._1() + " ";
                                    String rewrite = p._2() + " ";
                                    Optional<CombyRewrite> output = CombyUtils.rewrite(matcher, rewrite, source1);
                                    if (output.isPresent()) {
                                        evaluation.addMatchedRule(matcher, rewrite, output.get().getRewrittenSource());
                                        typeChangedSources.add(output.get().getRewrittenSource());
                                    }
                                }

                                for (var src : typeChangedSources) {
                                    String matcher;
                                    matcher = rule._1().replace(":[[TCIVar]]",":[TCIVar]").replace("TCIVar", "TCIVar~" + name);
                                    String rewrite = rule._2();
                                    var isReturn = src.contains("return ");
                                    Optional<String> output;
                                    boolean isCorrect = false;
                                    if (isReturn) {
                                        String rm = "return " + matcher + ";";
                                        String rr = "return " + rewrite + ";";
                                        output = CombyUtils.rewrite(rm, rr, src)
                                                .map(CombyRewrite::getRewrittenSource);
                                        if (output.isPresent() && isSAme(output.get(), target)) {
                                            inferredRules.add(rule);
                                            isCorrect = true;
                                        }else{
                                            rm = "return :[a~!]" + matcher + ";";
                                            rr = "return :[a~!]" + rewrite + ";";
                                            output = CombyUtils.rewrite(rm, rr, src)
                                                    .map(CombyRewrite::getRewrittenSource);
                                            if (output.isPresent() && isSAme(output.get(), target)) {
                                                inferredRules.add(rule);
                                                isCorrect = true;
                                            }
                                        }
                                    }
                                    if (!isCorrect) {
                                        output = CombyUtils.rewrite(matcher, rewrite, src).map(CombyRewrite::getRewrittenSource);
                                        if (output.isPresent() && isSAme(output.get(), target)) {
                                            inferredRules.add(rule);
                                        }else{
                                            output = CombyUtils.rewrite(matcher + ";", rewrite + ";", src).map(CombyRewrite::getRewrittenSource);
                                            if (output.isPresent() && isSAme(output.get(), target)) {
                                                inferredRules.add(rule);
                                            }else{
                                                output = CombyUtils.rewrite(":[a0~"+name+"]="+matcher+";", ":[a0]="+rewrite + ";", src.replace("\n","")).map(CombyRewrite::getRewrittenSource);
                                                if (output.isPresent() && isSAme(output.get(), target)) {
                                                    inferredRules.add(rule);
                                                }else{
                                                    output = CombyUtils.rewrite(":[a0~"+name+"]:[s~\\s*]="+matcher+";", ":[a0]:[s]="+rewrite + ";", src.replace("\n","")).map(CombyRewrite::getRewrittenSource);
                                                    if (output.isPresent() && isSAme(output.get(), target)) {
                                                        inferredRules.add(rule);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
                if(!inferredRules.isEmpty()){
                    evaluation = new EvaluationResult(commit, tc_,allCodeMappings,name);
                    evaluation.setStatus("TP");
                    inferredRules.forEach(evaluation::addCorrectRule);
                    relevant_popular_rules.addAll(inferredRules);
                }else{
                    allCodeMappings = mapping_granularity.stream().map(x->x._1()).collect(toList());
                    evaluation = new EvaluationResult(commit, tc_,allCodeMappings,name);
                    evaluation.setStatus("TBD");
                }
            }
            results.add(evaluation);
        }
        if(!results.isEmpty()){
            String jsonStr = new Gson().toJson(results);
            try {


                int i = 0;
                while(Files.exists(path_to_output_file)){
                    path_to_output_file = Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/Evaluation").resolve(commit + "_" + i + ".json");
                    i++;
                }
                Files.write(path_to_output_file,jsonStr.getBytes(),StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }


    public static List<List<Tuple2<CodeMapping, TypeChange>>> getAsCodeMapping(ChangesForCommit cfc, String tc_, Set<Tuple2<String, String>> allRenames) {
        return cfc.b4Aftrs.stream()
                .map(z -> z.getMappings().stream()
                        .filter(x -> x.before != null && x.after != null)
                        .map(x -> normalizeRefactoring(Tuple.of(CodeMapping.newBuilder()
                                        .setB4(normalizeStrLit(x.before, x.before, x.after))
                                        .setAfter(normalizeStrLit(x.after, x.before, x.after))
                                        .setUrlbB4("").setUrlAftr("")
                                        .setIsSame(x.before.equals(x.after)).build()
                                , getTypeChangeFor(x, tc_)), allRenames, cfc.commit))
                        .collect(toList()))
                .collect(toList());
    }

    private static Tuple2<CodeMapping, TypeChange> normalizeRefactoring(Tuple2<CodeMapping, TypeChange> of, Set<Tuple2<String, String>> allRenames, String commit) {
        var codeMapping = of._1();
        var typeChange = of._2();
        String stmtB4 = codeMapping.getB4();
        String nameB4 = typeChange.getBeforeName();
        String nameAfter = typeChange.getAfterName();



        String stmtAftr = codeMapping.getAfter();
        if (!typeChange.getRefactoringKind().equals("Change Return Type") && !typeChange.getRefactoringKind().equals("CHANGE_RETURN_TYPE")) {
            stmtAftr = CombyUtils.performIdentifierRename(nameB4, nameAfter, codeMapping.getAfter());
        }
        for (Tuple2<String, String> rn : allRenames)
            if (stmtB4.contains(rn._1()) && stmtAftr.contains(rn._2()))
                stmtAftr = CombyUtils.performIdentifierRename(rn._1(), rn._2(), stmtAftr);

        if (commit.equals("4653a99a88c49d4b1b680b6592ed39f90de68210")){
            stmtAftr = stmtAftr.replace(" log="," logger=")
                                .replace("log =","logger =")
                                .replace(" log;"," logger;");
        }

        if (commit.equals("efc2362d2bae0877a427ce2c29beae94118d6567")){
            stmtAftr = stmtAftr.replace(" date="," instant=")
                    .replace("date =","instant =")
                    .replace(" log;"," logger;");
        }

        if (commit.equals("8d202f12589356344e26b7ca15097eec46886055")){
            stmtAftr = stmtAftr.replace(" currDate="," instant=")
                    .replace("currDate =","instant =")
                    .replace(" log;"," logger;");
        }


        return Tuple.of(codeMapping.toBuilder().setAfter(stmtAftr).build(), typeChange);


    }

    private static TypeChange getTypeChangeFor(SnippetMappings.Mpng b4Aftr, String tc_) {
        var tc = Tuple.of(tc_.split("->")[0], tc_.split("->")[1]);
        return new TypeChange(b4Aftr.element, b4Aftr.element,
                ImmutablePair.of("SnippetCu", "SnippetCu"),
                ImmutablePair.of("SnippetCu", "SnippetCu"), tc._1(), tc._2(),
                null, null, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                "TypeChange");

    }

    private static String normalizeStringLiterals(String s) {
        return s.replaceAll("\".*\"", "");
    }

    static class EvaluationResult {
        private final String commit;
        private final String typeChange;
        private final List<CodeMapping> codeMapping;
        private final String elementName;
        private String Status;
        private final List<Tuple2<String, String>> correctRules;
        private List<Tuple3<String,String,String>> matchedRewriteRules;


        EvaluationResult(String commit, String typeChange, List<CodeMapping> codeMapping, String elementName) {
            this.commit = commit;
            this.typeChange = typeChange;
            this.codeMapping = codeMapping;
            this.elementName = elementName;
            this.Status = "UNKNOWN";
            correctRules = new ArrayList<>();
            this.matchedRewriteRules = new ArrayList<>();
        }

        public void addMatchedRule(String matcher, String rewrite, String ouput){
            matchedRewriteRules.add(Tuple.of(matcher, rewrite, ouput));
        }

        public void resetMatchedRules(){
            this.matchedRewriteRules = new ArrayList<>();
        }

        public String getCommit() {
            return commit;
        }

        public String getTypeChange() {
            return typeChange;
        }

        public List<CodeMapping> getCodeMapping() {
            return codeMapping;
        }

        public String getElementName() {
            return elementName;
        }

        public String getStatus() {
            return Status;
        }

        public void setStatus(String status) {
            Status = status;
            if(status.equals("TP"))
                resetMatchedRules();
        }

        public List<Tuple2<String, String>> getCorrectRule() {
            return correctRules;
        }

        public void addCorrectRule(Tuple2<String, String> correctRule) {
            this.correctRules.add(correctRule);
        }
    }
}
