package Utilities;

import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.ReplacementInferred;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;

public class RMinerUtils {

//    public static Path getProjectFolder(String project){
//        return Paths.get("/Users/ameya/Research/TypeChangeStudy/Corpus").resolve("Project_"+project).resolve(project);
//    }

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


    public static class Response {
        public List<CommitData> commits;
    }

    public static class CommitData {
        public String repository;
        public String sha1;
        public String url;
        public List<TypeChange> refactorings;

    }

    public static class TypeChange{
        private final String beforeName;
        private final String afterName;
        private ImmutablePair<String, String> beforeCu;
        private ImmutablePair<String, String> afterCu;
        private String b4Type;
        private String afterType;
        private LocationInfo locationInfoBefore;
        private LocationInfo locationInfoAfter;
        private List<String> addedImportStatements;
        private List<String> removedImportStatements;
        private List<String> unchangedImportStatements;
        private List<Statement_Mapping> references;

        public TypeChange(String beforeName, String afterName){
            this.beforeName = beforeName;
            this.afterName = afterName;
        }

        public TypeChange(String beforeName, String afterName, ImmutablePair<String, String> beforeCu, ImmutablePair<String, String> afterCu, String b4Type,
                          String afterType, LocationInfo locationInfoBefore, LocationInfo locationInfoAfter, Statement_Mapping varDeclLoc, List<Statement_Mapping> references,
                          List<String> addedImportStatements, List<String> removedImportStatements, List<String> unchangedImportStatements) {
            this.beforeName = beforeName;
            this.afterName = afterName;
            this.beforeCu = beforeCu;
            this.afterCu = afterCu;
            this.b4Type = b4Type;
            this.afterType = afterType;
            this.locationInfoBefore = locationInfoBefore;
            this.locationInfoAfter = locationInfoAfter;
            this.references = references;
            this.addedImportStatements = addedImportStatements;
            this.removedImportStatements = removedImportStatements;
            this.unchangedImportStatements = unchangedImportStatements;
            if(varDeclLoc != null){
                if(this.references.isEmpty()) {
                    this.references = List.of(varDeclLoc);
                }else{
                    this.references.add(varDeclLoc);
                }
            }
        }

        public String getBeforeName() {
            return beforeName;
        }

        public String getAfterName() {
            return afterName;
        }

        public ImmutablePair<String, String> getBeforeCu() {
            return beforeCu;
        }

        public ImmutablePair<String, String> getAfterCu() {
            return afterCu;
        }

        public String getB4Type() {
            return b4Type;
        }

        public String getAfterType() {
            return afterType;
        }


        public List<Statement_Mapping> getReferences() {
            return references;
        }

        public LocationInfo getLocationInfoBefore() {
            return locationInfoBefore;
        }

        public LocationInfo getLocationInfoAfter() {
            return locationInfoAfter;
        }

        public List<String> getAddedImportStatements() {
            return addedImportStatements;
        }


        public List<String> getRemovedImportStatements() {
            return removedImportStatements;
        }

        public List<String> getUnchangedImportStatements() {
            return unchangedImportStatements;
        }
    }

    public static class Statement_Mapping {
        private final String beforeStmt;
        private final String afterStmt;
        private final LocationInfo locationInfoBefore;
        private final LocationInfo locationInfoAfter;
        private final List<String> replacements;
        private final boolean isSimilar;

        public Statement_Mapping(String beforeStmt, String afterStmt, LocationInfo locationInfoBefore, LocationInfo locationInfoAfter, List<String> replacements, boolean isSimilar) {
            this.beforeStmt = beforeStmt;
            this.afterStmt = afterStmt;
            this.locationInfoBefore = locationInfoBefore;
            this.locationInfoAfter = locationInfoAfter;
            this.replacements = replacements;
            this.isSimilar = isSimilar;
        }

        public String getBeforeStmt() {
            return beforeStmt;
        }

        public String getAfterStmt() {
            return afterStmt;
        }

        public List<String> getReplacements() {
            return replacements;
        }

        public LocationInfo getLocationInfoBefore() {
            return locationInfoBefore;
        }

        public LocationInfo getLocationInfoAfter() {
            return locationInfoAfter;
        }

        public boolean isSimilar() {
            return isSimilar;
        }
    }

    public static List<Statement_Mapping> toStmtMapping(Collection<AbstractCodeMapping> acm){
        return acm.stream().map(x->toStmtMapping(x)).collect(Collectors.toList());
    }


    public static Statement_Mapping toStmtMapping(AbstractCodeMapping acm){
        return new Statement_Mapping(acm.getFragment1().getString(), acm.getFragment2().getString(), acm.getFragment1().getLocationInfo()
                ,acm.getFragment2().getLocationInfo(), acm.getReplacements().stream().map(x->x.getType().toString()).collect(Collectors.toList()), acm.isExact() || acm.isIdenticalWithExtractedVariable() || acm.isIdenticalWithInlinedVariable());
    }

//    public static String getJsonForRelevant(Refactoring r) {
//        String json = "";
//        if(r instanceof ChangeReturnTypeRefactoring){
//            ChangeReturnTypeRefactoring crt = (ChangeReturnTypeRefactoring) r;
//            var tc = new TypeChange(crt.getOperationBefore().getName(), crt.getOperationAfter().getName()
//                    ,crt.getInvolvedClassesBeforeRefactoring().iterator().next()
//                    ,crt.getInvolvedClassesAfterRefactoring().iterator().next()
//                    ,crt.getOriginalType().toQualifiedString(), crt.getChangedType().toQualifiedString()
//                    , crt.getOperationBefore().getLocationInfo(), crt.getOperationAfter().getLocationInfo(), null, toStmtMapping(crt.getReturnReferences()));
//            json = new Gson().toJson(tc, TypeChange.class);
//        }
//        if(r instanceof ChangeAttributeTypeRefactoring){
//            ChangeAttributeTypeRefactoring crt = (ChangeAttributeTypeRefactoring) r;
//            var tc = new TypeChange(crt.getOriginalAttribute().getName(), crt.getChangedTypeAttribute().getName()
//                    ,crt.getInvolvedClassesBeforeRefactoring().iterator().next()
//                    ,crt.getInvolvedClassesAfterRefactoring().iterator().next()
//                    ,crt.getOriginalAttribute().getType().toQualifiedString(), crt.getChangedTypeAttribute().getType().toQualifiedString()
//                    , crt.getOriginalAttribute().getLocationInfo(), crt.getChangedTypeAttribute().getLocationInfo(), null, toStmtMapping(crt.getAttributeReferences()));
//
//            json = new Gson().toJson(tc, TypeChange.class);
//        }
//        if(r instanceof ChangeVariableTypeRefactoring){
//            ChangeVariableTypeRefactoring crt = (ChangeVariableTypeRefactoring) r;
//            var tc = new TypeChange(crt.getOriginalVariable().getVariableName(), crt.getChangedTypeVariable().getVariableName()
//                    ,crt.getInvolvedClassesBeforeRefactoring().iterator().next()
//                    ,crt.getInvolvedClassesAfterRefactoring().iterator().next()
//                    ,crt.getOriginalVariable().getType().toQualifiedString(), crt.getChangedTypeVariable().getType().toQualifiedString()
//                    , crt.getOriginalVariable().getLocationInfo(), crt.getChangedTypeVariable().getLocationInfo(), null, toStmtMapping(crt.getVariableReferences()));
//
//            json = new Gson().toJson(tc, TypeChange.class);
//        }
//        if(r instanceof RenameAttributeRefactoring){
//            RenameAttributeRefactoring rar = (RenameAttributeRefactoring) r;
//            var re = new TypeChange(rar.getOriginalAttribute().getName(), rar.getRenamedAttribute().getName());
//            json = new Gson().toJson(re, TypeChange.class);
//        }
//        if(r instanceof RenameVariableRefactoring){
//            RenameVariableRefactoring rar = (RenameVariableRefactoring) r;
//            var re = new TypeChange(rar.getOriginalVariable().getVariableName(), rar.getRenamedVariable().getVariableName());
//            json = new Gson().toJson(re, TypeChange.class);
//        }
//        if(r instanceof RenameOperationRefactoring){
//            RenameOperationRefactoring rar = (RenameOperationRefactoring) r;
//            var re = new TypeChange(rar.getOriginalOperation().getName(), rar.getRenamedOperation().getName());
//            json = new Gson().toJson(re, TypeChange.class);
//        }
//        if(r instanceof RenameClassRefactoring){
//            RenameClassRefactoring rar = (RenameClassRefactoring) r;
//            var re = new TypeChange(rar.getOriginalClassName(),  rar.getRenamedClassName());
//            json = new Gson().toJson(re, TypeChange.class);
//        }
//        return json;
//    }

    public static String generateUrl(LocationInfo locationInfo, String cloneLink, String commit, String lOrR) {
        String url = cloneLink.replace(".git", "/commit/" + commit + "?diff=split#diff-");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(locationInfo.getFilePath().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            String val = sb.toString();
            return url + val + lOrR + locationInfo.getStartLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }

}



