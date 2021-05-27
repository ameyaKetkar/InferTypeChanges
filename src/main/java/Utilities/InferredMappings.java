package Utilities;

import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import type.change.treeCompare.MatchReplace;
import type.change.treeCompare.Update;

import java.util.*;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static Utilities.ResolveTypeUtil.resolveTypeNames;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.refactoringminer.RMinerUtils.TypeChange;

public class InferredMappings {

    private final String BeforeTypeTemplate;
    private final String AfterTypeTemplate;
    private final String Match;
    private final String Replace;
    private final Instance Instance;
    private final boolean noTv;

    public InferredMappings(Tuple2<String, String> templateBeforeAfter, Update u) {
        BeforeTypeTemplate = templateBeforeAfter._1();
        AfterTypeTemplate = templateBeforeAfter._2();
        Match = u.getMatchReplace().get().getMatchReplace()._1();
        Replace = u.getMatchReplace().get().getMatchReplace()._2();
        Instance = u.getAsInstance();
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
        private Map<String, String> TemplateVariableToCodeBefore;
        private final List<String> RelevantImports;
        private Map<String, String> TemplateVariableToCodeAfter;
        private String isRelevant;

        public Instance(CodeMapping cm, Update upd, TypeChange tc){
            OriginalCompleteBefore = cm.getB4().replace("\n","");
            OriginalCompleteAfter = cm.getAfter().replace("\n","");
            Before = upd.getBeforeStr().replace("\n","");
            After = upd.getAfterStr().replace("\n","");
            Project =extractProject(cm.getUrlbB4()) ;
            Commit = extractCommit(cm.getUrlbB4());
            CompilationUnit = tc.getBeforeCu().right;
            LineNos = Tuple.of(extractLineNumber(cm.getUrlbB4()), extractLineNumber(cm.getUrlAftr()));
            Names = Tuple.of(tc.getBeforeName(), tc.getAfterName());
            TemplateVariableToCodeBefore = upd.getMatchReplace().map(e -> e.getMatch().getTemplateVariableMapping())
                    .orElseGet(HashMap::new);
            TemplateVariableToCodeAfter = upd.getMatchReplace().map(e -> e.getReplace().getTemplateVariableMapping())
                    .orElseGet(HashMap::new);
            isRelevant = isRelevant(upd);
            RelevantImports = upd.getMatchReplace().map(matchReplace -> relevantImports(tc, matchReplace)).orElseGet(ArrayList::new);

        }

        private String isRelevant(Update upd) {
            if(upd.getMatchReplace().isPresent() && ((TemplateVariableToCodeBefore.containsValue(Names._1())
                    && TemplateVariableToCodeAfter.containsValue(Names._1()))))
                return "Uses";
            if(varOnLHS(Names._1(), Before, OriginalCompleteBefore) &&
                    (varOnLHS(Names._1(), After, OriginalCompleteAfter) || varOnLHS(Names._2(), After, OriginalCompleteAfter)))
                return "AssignedTo";
            if(isReturnExpression(OriginalCompleteBefore, Before) && isReturnExpression(OriginalCompleteAfter, After))
                return "Returns";
            return "Not Relevant";
        }


        private List<String> relevantImports(TypeChange tc, MatchReplace expl) {
            Map<String, String> classNamesReferred = Stream.concat(expl.getUnMatchedAfter().entrySet().stream(),expl.getUnMatchedBefore().entrySet().stream())
                    .filter(x -> !expl.getGeneralizations().containsKey(x.getKey()))
                    .filter(x -> CombyUtils.getPerfectMatch(":[c~\\w+[?:\\.\\w+]+]", x.getValue(), null).isPresent())
                    .filter(x -> Character.isUpperCase(x.getValue().charAt(0)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            if(classNamesReferred.isEmpty()) return new ArrayList<>();

            Map<Boolean, List<String>> relevantImports = Stream.concat(Stream.concat(tc.getAddedImportStatements().stream(), tc.getRemovedImportStatements().stream()),
                    tc.getUnchangedImportStatements().stream())
                    .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

            Map<String, String> resolvedTypeNames = resolveTypeNames(classNamesReferred, relevantImports);
            return new ArrayList<>(resolvedTypeNames.values());

        }

        private boolean isReturnExpression(String originalComplete, String codeSnippet) {
            Optional<Utilities.comby.Match> cm = CombyUtils.getPerfectMatch("return :[r];", originalComplete, null)
                    .map(x -> x.getMatches().get(0));
            return cm.map(match -> match.getEnvironment().stream().anyMatch(x -> x.getVariable().equals("r")
                        && x.getValue().equals(codeSnippet)))
                    .orElse(false);
        }

        private boolean varOnLHS(String tciVarName, String codeSnippet, String source){
            Optional<Utilities.comby.Match> cm =
                    Stream.of(":[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r];",
                            ":[mod:e] :[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r];",
                            ":[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r];")
                    .flatMap(x -> CombyUtils.getPerfectMatch(x, source, null).stream())
                    .findFirst().map(x -> x.getMatches().get(0));

            if(cm.isPresent()){
                boolean tciVarOnLHs = cm.get().getEnvironment().stream().anyMatch(x -> x.getVariable().equals("nm")
                        && (x.getValue().equals(tciVarName) || x.getValue().endsWith("." + tciVarName)));
                boolean beforeOnRHS = cm.get().getEnvironment().stream().anyMatch(x -> x.getVariable().equals("r")
                        && x.getValue().replace("\\\"", "\"").equals(codeSnippet));
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
            return !isRelevant.equals("Not Relevant");
        }

        public List<String> getRelevantImports() {
            return RelevantImports;
        }

        public void updateExplanation(Update upd) {
            TemplateVariableToCodeBefore = upd.getMatchReplace().map(e -> e.getMatch().getTemplateVariableMapping())
                    .orElseGet(HashMap::new);
            TemplateVariableToCodeAfter = upd.getMatchReplace().map(e -> e.getReplace().getTemplateVariableMapping())
                    .orElseGet(HashMap::new);
            isRelevant = isRelevant(upd);
        }
    }
}
