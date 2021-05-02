package type.change;

import Utilities.ASTUtils;
import com.github.gumtreediff.matchers.MappingStore;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.dom.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.zip;
import static gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType.VARIABLE_NAME;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


public class TypeChangeMiner extends RefactoringHandler {
    private final Path PathToCommitFilesAfter;
    private final Path PathToCommitFilesBefore;
    private final Set<Tuple3<String, String, String>> JarArtifactInfoSet;

    TypeChangeMiner(Path pathToCommitFilesAfter, Path pathToCommitFilesBefore,
                    Set<Tuple3<String, String, String>> jarArtifactInfoSet) {
        PathToCommitFilesAfter = pathToCommitFilesAfter;
        PathToCommitFilesBefore = pathToCommitFilesBefore;
        JarArtifactInfoSet = jarArtifactInfoSet;
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
            case CHANGE_ATTRIBUTE_TYPE:            return ((ChangeAttributeTypeRefactoring) r).getAttributeReferences();
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


    /**
     *
     * @param commitId
     * @param refactorings
     *
     *
     * If the commit is worth learning from :
     *
     *
     */

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        System.out.println("Refactorings at " + commitId);

        for (Refactoring refactoring : refactorings) {

            if(!isWorthLearning(refactoring)) continue;

            Optional<Tuple2<CompilationUnit, CompilationUnit>> cus = ASTUtils.getResolvedCompilationUnit(fst(refactoring.getInvolvedClassesBeforeRefactoring()).left, PathToCommitFilesBefore, JarArtifactInfoSet)
                    .flatMap(x -> ASTUtils.getResolvedCompilationUnit(fst(refactoring.getInvolvedClassesAfterRefactoring()).left, PathToCommitFilesAfter, JarArtifactInfoSet)
                            .map(y -> Tuple.of(x, y)));

            if (cus.isEmpty()) continue;

            Optional<Tuple2<Type, Type>> typeChange = getTypeChange(refactoring, cus.get()._1(), cus.get()._2());
            Optional<Tuple2<ITypeBinding, ITypeBinding>> typeChangeBindings = typeChange.map(tc -> tc.map(Type::resolveBinding, Type::resolveBinding));

            Optional<Tuple2<String, String>> names = getNames(refactoring);

            if(typeChangeBindings.isEmpty() || names.isEmpty()) continue;
            if(typeChangeBindings.get()._1().isFromSource() || typeChangeBindings.get()._2().isFromSource()) continue;

            List<Tuple2<ASTNode, ASTNode>> referringStatements = getReferences(refactoring)
                    .stream().map(x -> cus.get().map(b -> findLocation(b, x.getFragment1().getLocationInfo()),
                            a -> findLocation(a, x.getFragment2().getLocationInfo())))
                    .collect(toList());

            for(var rs : referringStatements){
//                System.out.println(rs);
//                Function<String, Optional<MappingStore>> fetchMapping = x -> Optional.empty();//Util.getMapping(rs._1(), rs._2(), x);
//
//                var mappings = // Stream.of("gumtree", "gumtree-simple", "change-distiller", "longestCommonSequence","default", "xy")
//                Stream.of("gumtree-simple","change-distiller")
////                        Stream.of("default")
//                        .map(fetchMapping).flatMap(Optional::stream).collect(toList());
//
//                for(var mapping : mappings){
//                    for(var x : mapping.asSet()){
//
//                        System.out.println(findLocation(cus.get()._1(), x.first.getPos(), x.first.getEndPos()).toString());
//                        System.out.println(findLocation(cus.get()._2(), x.second.getPos(), x.second.getEndPos()).toString());
//                        System.out.println("-----");
//                    }
//                }
//
//                // True -> TCI identifier
//                // False -> other expression of type T
//                var relevantExpressions = Util.getExpressionOfType(typeChangeBindings.get()._1(), rs._1())
//                        .stream().collect(groupingBy(x -> isTheTCIIdentifier(x, names.get()._1())));
//
//                // Get the mapping of expression of type T
//                var mappingForRelevantExprs =
//                      Stream.concat(Optional.ofNullable(relevantExpressions.get(false)).stream().flatMap(Collection::stream)
//                             .map(x -> Tuple.of(x, findInMappingStore(mappings, x, cus.get()))),
//                        Optional.ofNullable(relevantExpressions.get(true)).stream().flatMap(Collection::stream)
//                                .filter(x -> x.getParent() instanceof Expression)
//                            .map(x -> Tuple.of((Expression)x.getParent(), findInMappingStore(mappings, (Expression)x.getParent(), cus.get()))))
//                        .map(x -> Tuple.of(x, Tuple.of(getTypeName(x._1()),
//                                x._2().stream().flatMap(z -> getTypeName(z).stream()).collect(toList()))))
//                        .collect(toList());
//
//                for(Tuple2<Tuple2<Expression, List<Expression>>, Tuple2<Optional<String>, List<String>>> m : mappingForRelevantExprs){
//                    System.out.println(m._1._1().toString() + "-->" + m._1()._2().stream()
//                            .map(ASTNode::toString).collect(Collectors.joining("\t")));
//                    for(var expr : m._1()._2()){
//
//                        if(m._1()._1().toString().equals(expr.toString()))
//                            continue;
//
//                        Map<String, String> input = new HashMap<>(){{
//                            put("source", m._1()._1().toString());
//                            put("target", expr.toString());
//                        }};
//
////                        CombyResponse cr = combyRequest(input);
////
////                        if (cr!= null && cr.getResponse().size() > 0){
////                            System.out.println("Mapping found!!!!");
////                            for(BeforeAfter r: cr.getResponse()){
////                                System.out.println(r.getBefore() + " -> " + r.getAfter());
////                            }
////                        }
//                    }
//                    System.out.println(m._2._1() + "-->" + String.join("\t", m._2()._2()));
//
//                    System.out.println("----------");
//                }
//                System.out.println("---------------------------------------------");
            }

            System.out.println();
        }

        System.out.println();
    }

    private static Stream<Expression> getMetaPatternMatches(Expression expr){

        System.out.println();
        return Stream.empty();
    }


    private static Optional<String> getTypeName(Expression expression){
        if(expression instanceof MethodInvocation)
            return Optional.of((MethodInvocation) expression)
            .map(MethodInvocation::resolveMethodBinding)
            .map(IMethodBinding::getReturnType)
            .map(ITypeBinding::getQualifiedName);
        return Optional.ofNullable(expression.resolveTypeBinding()).map(o->o.getQualifiedName());
    }

    private static boolean isTheTCIIdentifier(Expression expr, String name){
        return expr instanceof Name && ((Name) expr).getFullyQualifiedName().equals(name);
    }

    private static List<Expression> findInMappingStore(List<MappingStore> stores, Expression ex, Tuple2<CompilationUnit, CompilationUnit> cus ){
        var mapping = stores.stream().flatMap(x -> x.asSet().stream())
                .filter(x -> x.first.getPos() == ex.getStartPosition() && x.first.getEndPos() == ex.getStartPosition() + ex.getLength())
                .map(x -> (Expression) findLocation(cus._2(), x.second.getPos(), x.second.getEndPos()))
                .distinct()
                .collect(toList());
        if(mapping.size() == 0){
            mapping = guessInMappingStore(stores, ex, cus);
        }
        return mapping;
    }
    /*
    In case the mapping store did not have the mappings for an sub-expression,
    we check if the sub-expression is child of some matched expressionsl
     */
    private static List<Expression> guessInMappingStore(List<MappingStore> stores, Expression ex, Tuple2<CompilationUnit, CompilationUnit> cus ){
        return stores.stream().flatMap(x -> x.asSet().stream())
                .filter(x -> x.first.getPos() <= ex.getStartPosition() && x.first.getEndPos() >= ex.getStartPosition() + ex.getLength())
                .flatMap(x -> zip(x.first.getChildren().stream(), x.second.getChildren().stream(), Tuple::of))
                .filter(x -> x._1().getPos() == ex.getStartPosition() && x._1().getEndPos() == ex.getStartPosition() + ex.getLength())
                .map(x -> (Expression) findLocation(cus._2(), x._2().getPos(), x._2().getEndPos()))
                .distinct()
                .collect(toList());
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
            default: return Optional.empty();

        }
    }
}







//    private static List<Tuple2<Tuple2<Expression, String>, Tuple2<Expression, String>>> realize(Tuple2<CompilationUnit, CompilationUnit> cus,
//                                                                                                MappingStore gumTreeEditScript) {
//        return gumTreeEditScript.asSet().stream()
//                .map(x -> Tuple.of(findLocation(cus._1(), x.first.getPos(), x.first.getEndPos())
//                        , findLocation(cus._2(), x.second.getPos(), x.second.getEndPos()), x))
//                .filter(m -> m._1() instanceof Expression && m._2() instanceof Expression)
//                .map(t -> t.map1(x -> (Expression) x).map2(x -> (Expression) x))
//                .map(t -> Tuple.of(Tuple.of(t._1(), t._1().resolveTypeBinding().getQualifiedName()),
//                        Tuple.of(t._2(), t._2().resolveTypeBinding().getQualifiedName())))
//                .collect(toList());
//    }

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

//    public static boolean isSubsumed(int a1, int a2, int b1, int b2){
//        return (a1 >= b1 && a2 < b2) || (a1 > b1 && a2 <= b2);
//    }
//
//
//
//    private boolean isRename(Tuple2<String, String> names, Action x) {
//        return !(x instanceof Update && ((Update) x).getValue().equals(names._2()) && x.getNode().getLabel().equals(names._1()));
//    }
