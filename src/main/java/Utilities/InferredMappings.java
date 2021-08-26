package Utilities;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import type.change.treeCompare.MatchReplace;
import type.change.treeCompare.Update;
import type.change.visitors.LowerCaseIdentifiers;

import java.util.*;
import java.util.stream.Stream;

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


    public InferredMappings(String beforeTypeTemplate, String afterTypeTemplate, String match, String replace, InferredMappings.Instance instance, boolean noTv){
        BeforeTypeTemplate = beforeTypeTemplate;
        AfterTypeTemplate = afterTypeTemplate;
        Match = match;
        Replace = replace;
        Instance = instance;
        this.noTv = noTv;
    }

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

    public Instance getInstance() {
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
        private final Tuple2<Integer,Integer> LineNos;
        private final Tuple2<String, String> Names;
        private Map<String, String> TemplateVariableToCodeBefore;
        private final List<String> RelevantImports;
        private Map<String, String> TemplateVariableToCodeAfter;
        private String isRelevant;
        private boolean isSafe;

        public Instance(CodeMapping cm, Update upd, TypeChange tc, String commit, String repoName, boolean isSafe){
            this.OriginalCompleteBefore = cm.getB4().replace("\n","");
            this.OriginalCompleteAfter = cm.getAfter().replace("\n","");
            this.Before = upd.getBeforeStr().replace("\n","");
            this.After = upd.getAfterStr().replace("\n","");
            this.Project = repoName;
            this.Commit = commit;
            this.CompilationUnit = tc.getBeforeCu().right;
            this.isSafe = isSafe;
            this.LineNos = tc.getLocationInfoBefore() == null ? Tuple.of(0,0)
                    : Tuple.of(tc.getLocationInfoBefore().getStartLine(),tc.getLocationInfoAfter().getStartLine());
            this.Names = Tuple.of(tc.getBeforeName(), tc.getAfterName());
            this.TemplateVariableToCodeBefore = upd.getMatchReplace()
                    .map(e -> e.getMatch().getTemplateVariableMapping()).orElseGet(HashMap::new);
            this.TemplateVariableToCodeAfter = upd.getMatchReplace()
                    .map(e -> e.getReplace().getTemplateVariableMapping()).orElseGet(HashMap::new);
            this.isRelevant = isRelevant(upd);
            this.RelevantImports = upd.getMatchReplace()
                    .map(matchReplace -> relevantImports(tc, matchReplace)).orElseGet(ArrayList::new);

        }

        private boolean isSafe(ASTNode before, ASTNode after){
            LowerCaseIdentifiers v1= new LowerCaseIdentifiers(), v2 = new LowerCaseIdentifiers();
            before.accept(v1);
            after.accept(v2);
            ImmutableSet<String> varIdentifiers1 = Sets.difference(v1.identifiers, v1.methodNames).immutableCopy(),
                    varIdentifiers2 = Sets.difference(v2.identifiers, v2.methodNames).immutableCopy();
            return Sets.difference(varIdentifiers2, varIdentifiers1).isEmpty()
                    && Sets.difference(varIdentifiers1, varIdentifiers2).isEmpty()
                    && Sets.difference(v2.stringLiterals, v1.stringLiterals).isEmpty()
                    && Sets.difference(v2.numberLiterals, v1.numberLiterals).isEmpty();
        }

        public void reComputeIsSafe(){

            if (getBefore().contains("noPendingMoveIteration")){
                System.out.println();
            }
            Optional<Expression> b4 = ASTUtils.getExpression(this.getBefore()), aftr = ASTUtils.getExpression(this.getAfter());
            if (b4.isPresent() && aftr.isPresent()){
                this.isSafe = isSafe(b4.get() , aftr.get());
            }else {
                Optional<Statement> b4Stmt  = ASTUtils.getStatement(this.getBefore()), aftrStmt = ASTUtils.getStatement(this.getBefore());
                if (b4Stmt.isPresent() && aftrStmt.isPresent()){
                    this.isSafe = isSafe(b4Stmt.get() , aftrStmt.get());
                }
            }
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
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a));
            if(classNamesReferred.isEmpty()) return new ArrayList<>();
            Map<Boolean, List<String>> relevantImports = Stream.concat(Stream.concat(tc.getAddedImportStatements().stream(),
                                        tc.getRemovedImportStatements().stream()), tc.getUnchangedImportStatements().stream())
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
                            ":[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r];",
                            ":[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]")
                    .flatMap(x -> CombyUtils.getPerfectMatch(x, source, null).stream())
                    .findFirst().map(x -> x.getMatches().get(0));

            if(cm.isPresent()){
                boolean tciVarOnLHs = cm.get().getEnvironment().stream().anyMatch(x -> x.getVariable().startsWith("nm")
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

        public Tuple2<Integer, Integer> getLineNos() {
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
            return true || !isRelevant.equals("Not Relevant");
//            return !isRelevant.equals("Not Relevant");
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

        public boolean isSafe() {
            return isSafe;
        }
    }
}
