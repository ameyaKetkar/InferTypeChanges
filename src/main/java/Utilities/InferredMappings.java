package Utilities;

import Utilities.RMinerUtils.TypeChange;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import Utilities.comby.CombyMatch;
import Utilities.comby.Environment;
import type.change.treeCompare.AbstractExplanation;
import type.change.treeCompare.Explanation;
import type.change.treeCompare.Update;

import java.util.*;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static Utilities.ASTUtils.extractLineNumber;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class InferredMappings {

    private final String BeforeTypeTemplate;
    private final String AfterTypeTemplate;
    private final String Match;
    private final String Replace;
    private final Instance Instance;
    private boolean noTv;

    public InferredMappings(Tuple2<String, String> templateBeforeAfter, Update u) {
        BeforeTypeTemplate = templateBeforeAfter._1();
        AfterTypeTemplate = templateBeforeAfter._2();
        Match = ((Explanation)u.getExplanation()).getMatchReplace()._1();
        Replace = ((Explanation)u.getExplanation()).getMatchReplace()._2();
        Instance = u.getAsInstance();
        noTv = !Match.contains(":[");
    }

    public InferredMappings(String beforeTypeTemplate, String afterTypeTemplate, String match, String replace, Instance instance) {
        BeforeTypeTemplate = beforeTypeTemplate;
        AfterTypeTemplate = afterTypeTemplate;
        Match = match;
        Replace = replace;
        Instance = instance;
        noTv = !Match.contains(":[");
    }

    public String getMatch() {
        return Match;
    }

    public String getReplace() {
        return Replace;
    }

    public Instance getInstances() {
        return Instance;
    }

//    public void isUsageMapping() {
//        List<Tuple2<Instance, String>> xs = getInstances().stream().flatMap(x -> isInstanceAnUsage(x).map(u -> Tuple.of(x, u)).stream())
//                .collect(toList());
//        if ((double) xs.size() / getInstances().size() >= 0.5) {
//            capturesUsage = (xs.get(0)._2());
//        }
//    }

    public Optional<String> isInstanceAnUsage(Instance i) {
        String matcher = getMatch().replace("\\\"", "\"");
        if (matcher.equals(i.getBefore())) return Optional.empty();
        CombyMatch cm_m = CombyUtils.getMatch(matcher, i.getBefore(), null).orElseThrow(() -> new RuntimeException(i.getBefore() + "     " + matcher));
        Optional<Environment> tv_b4 = cm_m.getMatches().get(0).getEnvironment().stream().filter(e -> e.getValue().equals(i.getNames()._1()))
                .findFirst();
        if (tv_b4.isPresent()) {
            CombyMatch cm_r = CombyUtils.getMatch(getReplace(), i.getAfter(), null).get();
            Optional<Environment> tv_After = cm_r.getMatches().get(0).getEnvironment().stream().filter(e -> e.getValue().equals(i.getNames()._1())
                    || e.getValue().equals(i.getNames()._2())).findFirst();
            if (tv_After.isPresent() && tv_b4.get().getVariable().equals(tv_After.get().getVariable())) {
                return Optional.ofNullable(tv_b4.get().getVariable());
            }
        }
        return Optional.empty();
    }

    public boolean isNoTv() {
        return noTv;
    }

    public String getBeforeTypeTemplate() {
        return BeforeTypeTemplate;
    }

    public String getAfterTypeTemplate() {
        return AfterTypeTemplate;
    }


    public static class Instance {
        private final String OriginalCompleteBefore;
        private final String OriginalCompleteAfter;
        private final String Before;
        private final String After;
        private final String Project;
        private final String Commit;
        private final String CompilationUnit;
        private final Tuple2<String, String> LineNos;
        private final Tuple2<String, String> Names;
        private final Map<String, String> TemplateVariableToCodeBefore;
        private final List<String> RelevantImports;

        private final Map<String, String> TemplateVariableToCodeAfter;
        private final boolean isRelevant;

        public Instance(CodeMapping cm, Update upd, TypeChange tc, AbstractExplanation explanation){
            OriginalCompleteBefore = cm.getB4().replace("\n","");
            OriginalCompleteAfter = cm.getAfter().replace("\n","");
            Before = upd.getBeforeStr().replace("\n","");
            After = upd.getAfterStr().replace("\n","");
            Project =extractProject(cm.getUrlbB4()) ;
            Commit = extractCommit(cm.getUrlbB4());
            CompilationUnit = tc.getBeforeCu().right;
            LineNos = Tuple.of(extractLineNumber(cm.getUrlbB4()), extractLineNumber(cm.getUrlAftr()));
            Names = Tuple.of(tc.getBeforeName(), tc.getAfterName());
            TemplateVariableToCodeBefore = upd.getExplanation() instanceof  Explanation
                    ? ((Explanation)upd.getExplanation()).getTvMapB4() : new HashMap<>();
            TemplateVariableToCodeAfter = upd.getExplanation() instanceof  Explanation
                    ? ((Explanation)upd.getExplanation()).getTvMapAfter() : new HashMap<>();
            isRelevant = upd.getExplanation() instanceof Explanation
                    && ((TemplateVariableToCodeBefore.containsValue(Names._1()) && TemplateVariableToCodeAfter.containsValue(Names._1()))
                    // Because the statements are normalized to renames
                    || (varOnLHS(Names._1(), Before, OriginalCompleteBefore) &&
                    (varOnLHS(Names._1(), After, OriginalCompleteAfter) || varOnLHS(Names._2(), After, OriginalCompleteAfter)))
                    || (isReturnExpression(OriginalCompleteBefore, Before) && isReturnExpression(OriginalCompleteAfter, After)));
            RelevantImports = explanation instanceof Explanation ? relevantImports(tc, (Explanation) explanation) : new ArrayList<>();

        }

        private List<String> relevantImports(TypeChange tc, Explanation expl) {
            Map<String, String> classNamesReferredAfter = expl.getTvMapAfter()
                    .entrySet().stream().filter(x -> !expl.getMatchedTemplateVariables().containsValue(x.getKey()))
                    .filter(x -> CombyUtils.getPerfectMatch(Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s->true), x.getValue(), null).isPresent())
                    .filter(x -> Character.isUpperCase(x.getValue().charAt(0)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            if(classNamesReferredAfter.isEmpty()) return new ArrayList<>();

            Map<Boolean, List<String>> relevantImportsAfter = Stream.concat(tc.getAddedImportStatements().stream(),
                    tc.getUnchangedImportStatements().stream())
                    .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));


            Map<String, String> c2 = classNamesReferredAfter.entrySet().stream()
                    .flatMap(x -> relevantImportsAfter.getOrDefault(true, new ArrayList<>()).stream()
                            .filter(y -> y.endsWith("." + x.getValue())).findFirst().stream().map(y -> Tuple.of(x.getKey(), y)))
                    .collect(toMap(x -> x._1(), x -> x._2()));

            return new ArrayList<>(c2.values());

        }

        private boolean isReturnExpression(String originalComplete, String codeSnippet) {
            Optional<Utilities.comby.Match> cm = CombyUtils.getPerfectMatch(CaptureMappingsLike.PATTERNS_HEURISTICS.get("ReturnStmt"),
                    originalComplete.replace(";",""), null)
                    .map(x -> x.getMatches().get(0));
            return cm.map(match -> match.getEnvironment().stream().anyMatch(x -> x.getVariable().equals("r")
                        && x.getValue().replace("\\\"", "\"").equals(codeSnippet)))
                    .orElse(false);
        }

        private boolean varOnLHS(String tciVarName, String codeSnippet, String source){
            Optional<Utilities.comby.Match> cm = CaptureMappingsLike.PATTERNS_HEURISTICS.entrySet().stream()
                    .filter(x -> x.getKey().contains("Assignment"))
                    .flatMap(x -> CombyUtils.getPerfectMatch(x.getValue(), source, null).stream())
                    .findFirst().map(x -> x.getMatches().get(0));

            if(cm.isPresent()){
                boolean tciVarOnLHs = cm.get().getEnvironment().stream().anyMatch(x -> x.getVariable().equals("nm")
                        && (x.getValue().equals(tciVarName) || x.getValue().endsWith("." + tciVarName)));
                boolean beforeOnRHS = cm.get().getEnvironment().stream().anyMatch(x -> x.getVariable().equals("r")
                        && x.getValue().replace("\\\"", "\"").contains(codeSnippet));
                return tciVarOnLHs && beforeOnRHS;
            }
            return false;

        }



        public String getBefore() {
            return Before;
        }

        public String getProject() {
            return Project;
        }

        public String getCommit() {
            return Commit;
        }

        public String getAfter() {
            return After;
        }

        public Tuple2<String, String> getLineNos() {
            return LineNos;
        }

        public String getCompilationUnit() {
            return CompilationUnit;
        }

        public Tuple2<String, String> getNames() {
            return Names;
        }

        public String getOriginalCompleteBefore() {
            return OriginalCompleteBefore;
        }

        public String getOriginalCompleteAfter() {
            return OriginalCompleteAfter;
        }

        public Map<String, String> getTemplateVariableToCodeBefore() {
            return TemplateVariableToCodeBefore;
        }

        public Map<String, String> getTemplateVariableToCodeAfter() {
            return TemplateVariableToCodeAfter;
        }


        public boolean isRelevant() {
            return isRelevant;
        }

        public List<String> getRelevantImports() {
            return RelevantImports;
        }
    }
}
