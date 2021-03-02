package type.change;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.gen.jdt.JdtVisitor;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static com.t2r.common.utilities.FileUtils.materializeAtBase;
import static com.t2r.common.utilities.GitUtil.*;
import static gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType.VARIABLE_NAME;
import static java.util.stream.Collectors.toList;

public class Learner {

    public static Properties prop;
    public static Function<String, Path> projectPath;
    public static Path pathToCorpus;
    public static Path pathToTemp;

    static {
        try {
            prop = new Properties();
            InputStream input = new FileInputStream("paths.properties");
            prop.load(input);
            pathToCorpus = Path.of(prop.getProperty("PathToCorpus"));
            pathToTemp = Path.of(prop.getProperty("PathToTemp"));
            projectPath = p -> pathToCorpus.resolve("Project_" + p).resolve(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] a) throws Exception {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        for (var e : Files.readAllLines(Path.of("Corpus.csv"))) {
            var projectName = e.split(",")[0].trim();
            var commitID = e.split(",")[1].trim();

            if (!commitID.equals("7485c34dead228336791067e3a6b03cbb3dcaaa2"))
                continue;

            Try<Git> g = tryToClone("", projectPath.apply(projectName));

            if(g.isFailure()) continue;

            Repository repo = g.get().getRepository();
            var commit = findCommit(commitID, repo);

            if(commit.isEmpty()) continue;

            var files = getFilesAddedRemovedRenamedModified(repo,
                    commit.get(),filePathDiffAtCommit(g.get(), commitID));

            Path pAfter = pathToTemp.resolve(commitID);
            materializeAtBase(pAfter, files._4());
            materializeAtBase(pAfter, files._3());

            Path pBefore = pathToTemp.resolve(commitID + "-Parent");
            materializeAtBase(pBefore, files._2());
            materializeAtBase(pBefore, files._1());
            var typeChangeMiner = new TypeChangeMiner(pAfter, pBefore);
            miner.detectAtCommit(repo, commitID, typeChangeMiner);
        }
    }

    static class TypeChangeMiner extends RefactoringHandler{
        private final Path PathToCommitFilesAfter;
        private final Path PathToCommitFilesBefore;

        TypeChangeMiner(Path pathToCommitFilesAfter, Path pathToCommitFilesBefore) {
            PathToCommitFilesAfter = pathToCommitFilesAfter;
            PathToCommitFilesBefore = pathToCommitFilesBefore;
        }

        private static boolean isWorthLearning(Refactoring r){
            Set<AbstractCodeMapping> references = getReferences(r);
            return references.size() > 0 && references.stream().anyMatch(ref -> !ref.isExact() && ref.getReplacements()
                            .stream().anyMatch(x -> !x.getType().equals(VARIABLE_NAME)));
        }

        private static Set<AbstractCodeMapping> getReferences(Refactoring r){
            switch (r.getRefactoringType()){
                case CHANGE_VARIABLE_TYPE:
                case CHANGE_PARAMETER_TYPE: return ((ChangeVariableTypeRefactoring) r).getVariableReferences();
                case CHANGE_RETURN_TYPE: return ((ChangeReturnTypeRefactoring) r).getReturnReferences();
                case CHANGE_ATTRIBUTE_TYPE: return ((ChangeAttributeTypeRefactoring) r).getAttributeReferences();
                default: return new HashSet<>();
            }
        }

        private static <T> Optional<T> getParentOfKind(ASTNode ast, int nodeType){
            if (ast.getNodeType() == nodeType) return Optional.of((T) ast);
            if(ast.getParent() == null) return Optional.empty();
            return getParentOfKind(ast.getParent(), nodeType);
        }

        private static Optional<Tuple2<Type, Type>> getTypeChange(Refactoring r, CompilationUnit cuB4, CompilationUnit cuAfter){
            switch (r.getRefactoringType()){
                case CHANGE_VARIABLE_TYPE:
                    ChangeVariableTypeRefactoring r0 = (ChangeVariableTypeRefactoring) r;
                    return TypeChangeMiner.<VariableDeclarationStatement>getParentOfKind(findLocation(cuB4, r0.getOriginalVariable().getLocationInfo()), ASTNode.VARIABLE_DECLARATION_STATEMENT)
                            .flatMap(x -> TypeChangeMiner.<VariableDeclarationStatement>getParentOfKind(findLocation(cuAfter, r0.getChangedTypeVariable().getLocationInfo()), ASTNode.VARIABLE_DECLARATION_STATEMENT)
                                    .map(y -> Tuple.of(x.getType(), y.getType())));
                case CHANGE_PARAMETER_TYPE:
                    ChangeVariableTypeRefactoring r1 = (ChangeVariableTypeRefactoring) r;
                    return TypeChangeMiner.<SingleVariableDeclaration>getParentOfKind(findLocation(cuB4, r1.getOriginalVariable().getLocationInfo()), ASTNode.SINGLE_VARIABLE_DECLARATION)
                            .flatMap(x -> TypeChangeMiner.<SingleVariableDeclaration>getParentOfKind(findLocation(cuAfter, r1.getChangedTypeVariable().getLocationInfo()), ASTNode.VARIABLE_DECLARATION_STATEMENT)
                                    .map(y -> Tuple.of(x.getType(), y.getType())));
                case CHANGE_RETURN_TYPE:
                    ChangeReturnTypeRefactoring r2 = (ChangeReturnTypeRefactoring) r;
                    return TypeChangeMiner.<MethodDeclaration>getParentOfKind(findLocation(cuB4, r2.getOperationBefore().getLocationInfo()), ASTNode.METHOD_DECLARATION)
                        .flatMap(x -> TypeChangeMiner.<MethodDeclaration>getParentOfKind(findLocation(cuAfter, r2.getOperationAfter().getLocationInfo()), ASTNode.METHOD_DECLARATION)
                                .map(y -> Tuple.of(x.getReturnType2(), y.getReturnType2())));
                case CHANGE_ATTRIBUTE_TYPE:
                    ChangeAttributeTypeRefactoring r3 = (ChangeAttributeTypeRefactoring) r;
                    return TypeChangeMiner.<FieldDeclaration>getParentOfKind(findLocation(cuB4, r3.getOriginalAttribute().getLocationInfo()), ASTNode.FIELD_DECLARATION)
                        .flatMap(x -> TypeChangeMiner.<FieldDeclaration>getParentOfKind(findLocation(cuAfter, r3.getChangedTypeAttribute().getLocationInfo()), ASTNode.FIELD_DECLARATION)
                                .map(y -> Tuple.of(x.getType(), y.getType())));
                default: return Optional.empty();

            }
        }


        private static <T> T fst(Collection<T> c){
            return new ArrayList<>(c).get(0);
        }

        private static ASTNode findLocation(CompilationUnit cu, LocationInfo locationInfo){
            return NodeFinder.perform(cu, locationInfo.getStartOffset(), locationInfo.getLength());
        }


        private static ASTNode findLocation(CompilationUnit cu, int start, int end){
            return NodeFinder.perform(cu, start, end - start);
        }


        static class ExpressionOfType extends ASTVisitor{

            private final String typeName;

            private final List<Expression> relevantExpressions;

            public ExpressionOfType(String typeName){
                this.relevantExpressions = new ArrayList<>();
                this.typeName = typeName;
            }
            @Override
            public void postVisit(ASTNode node) {
                if(node instanceof Expression){
                    Expression ex = (Expression) node;
                    ITypeBinding t = ex.resolveTypeBinding();
                    if(t != null && typeName.equals(t.getBinaryName())){
                        relevantExpressions.add(ex);
                    }
                }//7485c34dead228336791067e3a6b03cbb3dcaaa2
                super.postVisit(node);
            }
        }

        @Override
        public void handle(String commitId, List<Refactoring> refactorings) {
            System.out.println("Refactorings at " + commitId);


            for (Refactoring refactoring : refactorings) {

                if(!isWorthLearning(refactoring)) continue;

                Optional<Tuple2<CompilationUnit, CompilationUnit>> cus = getResolvedCompilationUnit(fst(refactoring.getInvolvedClassesBeforeRefactoring()).left, PathToCommitFilesBefore)
                        .flatMap(x -> getResolvedCompilationUnit(fst(refactoring.getInvolvedClassesAfterRefactoring()).left, PathToCommitFilesAfter)
                                .map(y -> Tuple.of(x, y)));

                if (cus.isEmpty()) continue;

                Optional<Tuple2<Type, Type>> typeChange = getTypeChange(refactoring, cus.get()._1(), cus.get()._2());
                Optional<Tuple2<ITypeBinding, ITypeBinding>> typeChangeBindings = typeChange.map(tc -> tc.map(Type::resolveBinding, Type::resolveBinding));

                Optional<Tuple2<String, String>> names = getNames(refactoring);

                if(typeChangeBindings.isEmpty() || names.isEmpty()) continue;
                List<Tuple2<ASTNode, ASTNode>> referringStatements = getReferences(refactoring)
                        .stream().map(x -> cus.get().map(b -> findLocation(b, x.getFragment1().getLocationInfo()), a -> findLocation(a, x.getFragment2().getLocationInfo())))
                        .collect(toList());

                for(var rs : referringStatements){
                    Tuple2<MappingStore, List<Action>> gumTreeEditScript = getGumTreeEditScript(rs._1(), rs._2());
                    var mappings = Arrays.asList(getMapping(rs._1(), rs._2(), "gumtree"), getMapping(rs._1(), rs._2(), "gumtree-simple")
                            , getMapping(rs._1(), rs._2(), "change-distiller"), getMapping(rs._1(), rs._2(), "longestCommonSequence"),
                            getMapping(rs._1(), rs._2(), "xy"))
                            .stream().flatMap(Optional::stream).collect(toList());


                    List<Expression> relevantExpressions = getExpressionOfType(typeChangeBindings.get()._1(), rs._1());
                    List<Tuple2<Tuple2<Expression, List<Expression>>, Tuple2<ITypeBinding, List<ITypeBinding>>>> mappingForRelevantExprs = relevantExpressions.stream()
                            .map(x -> Tuple.of(x, findInMappingStore(mappings, x, cus.get())))
                            .map(x -> Tuple.of(x, Tuple.of(x._1().resolveTypeBinding(),
                                    x._2().stream().map(z -> z.resolveTypeBinding()).collect(toList()))))
                            .collect(toList());
                    System.out.println();
                }

                System.out.println();
            }
            System.out.println();
        }

        private static List<Expression> findInMappingStore(List<MappingStore> stores, Expression ex, Tuple2<CompilationUnit, CompilationUnit> cus ){
            return stores.stream().flatMap(x -> x.asSet().stream())
                    .filter(x -> x.first.getPos() == ex.getStartPosition() && x.first.getEndPos() == ex.getStartPosition() + ex.getLength())
                    .map(x -> (Expression) findLocation(cus._2(), x.second.getPos(), x.second.getEndPos()))
                    .distinct()
                   .collect(toList());

        }

        private static List<Tuple2<Tuple2<Expression, String>, Tuple2<Expression, String>>> realize(Tuple2<CompilationUnit, CompilationUnit> cus,
                                                                                                       MappingStore gumTreeEditScript) {
            return gumTreeEditScript.asSet().stream()
                    .map(x -> Tuple.of(findLocation(cus._1(), x.first.getPos(), x.first.getEndPos())
                            , findLocation(cus._2(), x.second.getPos(), x.second.getEndPos()), x))
                    .filter(m -> m._1() instanceof Expression && m._2() instanceof Expression)
                    .map(t -> t.map1(x -> (Expression) x).map2(x -> (Expression) x))
                    .map(t -> Tuple.of(Tuple.of(t._1(), t._1().resolveTypeBinding().getQualifiedName()),
                            Tuple.of(t._2(), t._2().resolveTypeBinding().getQualifiedName())))
                    .collect(toList());
        }

//        public static Tuple2<ITree, ITree> getExpressionMappingFor(List<Action> actions, MappingStore mappingStore){
//            ITree parent = null;
//            for(var axn : actions){
//                ITree curr_parent = axn.getNode().getParent();
//                if(parent == null) {
//                    parent = curr_parent;
//                }
//                else if(isSubsumed(parent.getPos(), parent.getEndPos(), curr_parent.getPos(), curr_parent.getEndPos())){
//                    parent = curr_parent;
//                }
//            }
//            ITree dest = mappingStore.getSrcForDst(parent);
//            return Tuple.of(parent, dest);
//        }

        public static boolean isSubsumed(int a1, int a2, int b1, int b2){
            return (a1 >= b1 && a2 < b2) || (a1 > b1 && a2 <= b2);
        }



        private boolean isRename(Tuple2<String, String> names, Action x) {
            return !(x instanceof Update && ((Update) x).getValue().equals(names._2()) && x.getNode().getLabel().equals(names._1()));
        }

        private List<Expression> getExpressionOfType(ITypeBinding iTypeBinding, ASTNode astNode) {
            var mv1 = new ExpressionOfType(iTypeBinding.getBinaryName());
            astNode.accept(mv1);
            return mv1.relevantExpressions;
        }

        private Optional<Tuple2<String, String>> getNames(Refactoring r) {

            switch (r.getRefactoringType()){
                case CHANGE_VARIABLE_TYPE:
                case CHANGE_PARAMETER_TYPE:
                ChangeVariableTypeRefactoring r0 = (ChangeVariableTypeRefactoring) r;
                return Optional.of(Tuple.of(r0.getOriginalVariable().getVariableName(), r0.getChangedTypeVariable().getVariableName()));
                case CHANGE_RETURN_TYPE:
                    ChangeReturnTypeRefactoring r2 = (ChangeReturnTypeRefactoring) r;
                    return Optional.of(Tuple.of(r2.getOperationBefore().getName(), r2.getOperationBefore().getName()));
                case CHANGE_ATTRIBUTE_TYPE:
                    ChangeAttributeTypeRefactoring r3 = (ChangeAttributeTypeRefactoring) r;
                    return Optional.of(Tuple.of(r3.getOriginalAttribute().getName(), r3.getChangedTypeAttribute().getName()));
                default:{
                    return Optional.empty();
                }
            }
        }
    }


    static Optional<CompilationUnit> getResolvedCompilationUnit(String cu_path, Path pathCommitFiles){
        Path cuPath = pathCommitFiles.resolve(cu_path);
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        String unitName = cuPath.getFileName().toString();
        parser.setUnitName(unitName);
        try {

            String[] sources = Files.walk(pathCommitFiles)
                    .filter(Files::isDirectory)
                    .filter(x -> x.endsWith("java"))
                    .map(x -> x.toAbsolutePath().toString()).toArray(String[]::new);

            String[] classpath = {"/Library/Java/JavaVirtualMachines/jdk-11.0.6.jdk/Contents/Home/"};
            String[] encodings = new String[sources.length];
            Arrays.fill(encodings, "UTF-8");
            parser.setEnvironment(classpath,sources, encodings, true);
            var content = Try.of(() -> Files.readString(cuPath)).getOrElse("");
            parser.setSource(content.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            if (cu.getAST().hasBindingsRecovery()) {
                System.out.println("Binding activated.");
                return Optional.of(cu);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


    public static Optional<MappingStore> getMapping(ASTNode before, ASTNode after, String algo) {
        Optional<MappingStore> result = Optional.empty();
        try {
            TreeContext src = getContextForASTNode(before);
            TreeContext dst = getContextForASTNode(after);

            Matcher m = algo.equals("default") ? Matchers.getInstance().getMatcher() : Matchers.getInstance().getMatcher(algo);
            if (src != null && dst != null) {
                result = Optional.ofNullable(m.match(src.getRoot(), dst.getRoot()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Tuple2<MappingStore, List<Action>> getGumTreeEditScript(ASTNode before, ASTNode after) {
        try {
            TreeContext src = getContextForASTNode(before);
            TreeContext dst = getContextForASTNode(after);
            Matcher m = Matchers.getInstance().getMatcher("gumtree-simple");
            EditScriptGenerator e = new SimplifiedChawatheScriptGenerator();
            if (src == null || dst == null) return Tuple.of(null, new ArrayList<>());
            MappingStore match = m.match(src.getRoot(), dst.getRoot());
            EditScript editScript = e.computeActions(match);
            return Tuple.of(match, editScript.asList());
        } catch (Exception e) {
            e.printStackTrace();
        }
         return Tuple.of(null, new ArrayList<>());
    }

    public static TreeContext getContextForASTNode(ASTNode ast){
        IScanner scanner = ToolFactory.createScanner(false, false, false, false);
        scanner.setSource(ast.toString().toCharArray());
        AbstractJdtVisitor v = new JdtVisitor(scanner);
        ast.accept(v);
        return v.getTreeContext();
    }



}
