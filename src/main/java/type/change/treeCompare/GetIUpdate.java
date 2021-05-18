package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.HttpUtils;
import Utilities.RMinerUtils.TypeChange;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import Utilities.comby.Environment;
import Utilities.comby.Match;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static Utilities.ASTUtils.getChildren;
import static Utilities.ASTUtils.getCoveringNode;

import static Utilities.CaptureMappingsLike.SYNTACTIC_TYPE_CHANGES;
import static Utilities.CombyUtils.*;
import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.*;

public class GetIUpdate {


    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesB4;
    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;

    public GetIUpdate(CodeMapping codeMapping, TypeChange typeChange) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();

    }

    public static Optional<Tuple2<String, String>> getResolvedTypeChangeTemplate(Tuple2<String, String> reportedTypeChange, List<TypeChange> typeChanges) {

        Function<String, Optional<PerfectMatch>> matchType = t -> SYNTACTIC_TYPE_CHANGES.entrySet().stream()
                        .flatMap(x -> getPerfectMatch(x.getValue(), t, ".xml")
                                .map(y -> new PerfectMatch(x.getKey(), x.getValue()._1(), y.getMatches().get(0)))
                                .stream())
                        .findFirst();

        var matchedTypeSyntax = reportedTypeChange.map(matchType, matchType);

        if(matchedTypeSyntax._1().isEmpty() || matchedTypeSyntax._2().isEmpty())
            return Optional.empty();

        MatchReplace expl = new MatchReplace(matchedTypeSyntax._1().get(), matchedTypeSyntax._2().get());
        Tuple2<String, String> enrichedMatchReplace = tryToresolveTypes(expl, typeChanges);

        if(enrichedMatchReplace._1().contains(":[") && !enrichedMatchReplace._2().contains(":[") )
            System.out.println();

        if(!enrichedMatchReplace._1().contains(":[") && enrichedMatchReplace._2().contains(":[") )
            System.out.println();

        System.out.println(reportedTypeChange + " -> " + enrichedMatchReplace);
        return Optional.ofNullable(enrichedMatchReplace);
    }

    private static Tuple2<String, String> tryToresolveTypes(MatchReplace expl, List<TypeChange> typeChanges) {
        Tuple2<String, String> matchReplace = expl.getMatchReplace();
        Map<String, String> tvMapB4 = expl.getMatch().getTemplateVariableMapping()
                .entrySet().stream().filter(x -> !expl.getTemplateVariableDeclarations().containsKey(x.getKey()))
                .filter(x -> matchReplace._1().contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s->true), x.getValue(), null).isPresent())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> tvMapAfter = expl.getReplace().getTemplateVariableMapping()
                .entrySet().stream().filter(x -> !expl.getTemplateVariableDeclarations().containsValue(x.getKey()))
                .filter(x -> matchReplace._2().contains(x.getValue()))
                .filter(x -> CombyUtils.getPerfectMatch(Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s->true), x.getValue(), null).isPresent())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<Boolean, List<String>> relevantImportsB4 = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getRemovedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

        Map<String, String> c1 = tvMapB4.entrySet().stream()
                .map(x -> Tuple.of(x.getValue(), resolveType(x, relevantImportsB4)))
                .filter(x -> x._2().isPresent())
                .map(x -> x.map2(y -> y.get()))
                .collect(toMap(x -> x._1(), x -> x._2(), (a,b) -> a));

        Map<Boolean, List<String>> relevantImportsAfter = typeChanges.stream()
                .flatMap(x -> Stream.concat(x.getAddedImportStatements().stream(), x.getUnchangedImportStatements().stream()))
                .collect(groupingBy(x -> Character.isUpperCase(x.split("\\.")[x.split("\\.").length - 1].charAt(0))));

        Map<String, String> c2 = tvMapAfter.entrySet().stream()
                .map(x -> Tuple.of(x.getValue(), resolveType(x, relevantImportsAfter)))
                .filter(x -> x._2().isPresent())
                .map(x -> x.map2(y -> y.get()))
                .collect(toMap(x -> x._1(), x -> x._2(),(a,b) -> a));

        Tuple2<String, String> s = expl.getMatchReplace().map(x -> performRenameIdentifier(x, c1), x -> performRenameIdentifier(x, c2));

        return s;

    }


    public static String performRenameIdentifier(String source, Map<String, String> renameMapping){
        String curr = source;
        for(var e: renameMapping.entrySet()){
            curr = performIdentifierRename(e.getValue(), e.getKey(), source);
        }
        return curr;
    }


    public static Optional<String> resolveType(Map.Entry<String, String> b, Map<Boolean, List<String>> relevantImports) {
        return isPrimitive(b).or(() -> relevantImports.getOrDefault(true, new ArrayList<>()).stream()
                                .filter(x -> x.endsWith("." + b.getValue())).findFirst())
                .or(() -> findInBuiltInJava(relevantImports.getOrDefault(false, new ArrayList<>()), b));
    }

    public static class ResolveResponse {
        public String QualifiedName;
    }

    private static Optional<String> findInBuiltInJava(List<String> packages, Map.Entry<String, String> b) {
        Optional<String> response = HttpUtils.makeHttpRequest(Map.of("purpose", "Resolve", "lookup", b.getValue(), "packages",
                String.join(",", packages)))
                .map(x -> new Gson().fromJson(x, ResolveResponse.class).QualifiedName)
                .filter(x -> !x.isEmpty());
        return response;
    }

    private static Optional<String> isPrimitive(Map.Entry<String, String> b) {
        return SYNTACTIC_TYPE_CHANGES.entrySet().stream().filter(x -> x.getKey().startsWith("prim"))
                .flatMap(x -> getPerfectMatch(x.getValue(), b.getValue(), null)
                        .map(y -> b.getValue()).stream()).findFirst();
    }

    public Optional<Update> getUpdate(ASTNode before, ASTNode after, Tree root1, Tree root2) {

        if (root1 == null || root2 == null) return Optional.empty();

        if (!root1.hasSameType(root2))
            System.out.println();

        Optional<MatchReplace> explanation = before instanceof Expression && after instanceof Expression ?
                getInstance(before.toString(), after.toString(), Tuple.of(root1.getPos(), root1.getEndPos())
                            , Tuple.of(root2.getPos(), root2.getEndPos()))
                : Optional.empty();

        Update upd = new Update(root1, root2, before.toString(), after.toString(),explanation, codeMapping, typeChange);

        if (root1.hasSameType(root2))
            zip(getChildren(root1), getChildren(root2), Tuple::of).forEach(t -> {
                if (t._1().isIsomorphicTo(t._2()))
                    upd.addSubExplanation(Optional.empty());
                else
                    getCoveringNode(before, t._1()).flatMap(x -> getCoveringNode(after, t._2()).map(y -> Tuple.of(x, y)))
                            .ifPresent(x -> upd.addSubExplanation(getUpdate(x._1(), x._2(), t._1(), t._2())));
            });
        else{
            if(upd.getExplanation().isPresent()){
                MatchReplace expl = upd.getExplanation().get();
                Map<String, String> unMappedTVB4 = expl.getUnMatchedTemplateVarsBefore();
                Map<String, String> unMappedTVAfter = expl.getUnMatchedTemplateVarsAfter();
                if(unMappedTVB4.size()==unMappedTVAfter.size() && unMappedTVB4.size() == 1){

                    Optional<ASTNode> n1 = getCoveringNode(before, expl.getMatch().getTemplateVariableMappingRange()
                            .get(unMappedTVB4.entrySet().iterator().next().getKey()));
                    Optional<ASTNode> n2 = getCoveringNode(after, expl.getReplace().getTemplateVariableMappingRange()
                            .get(unMappedTVAfter.entrySet().iterator().next().getKey()));
                    if(n1.isPresent() && n2.isPresent()){
                        if(!n1.get().toString().equals(before.toString()) && !n2.get().toString().equals(after.toString()))
                            upd.setSubUpdates(getUpdate(n1.get(), n2.get()).stream().collect(toList()));
                    }
                    else{
                        System.out.println("Could not find expr");
                        System.out.println(before.toString());
                        System.out.println(after.toString());
                    }
                }else{
                    if (unMappedTVB4.size() != unMappedTVAfter.size() || unMappedTVB4.size() != 0) {
                        System.out.println("Ow! Too many unmatched vars");
                        System.out.println(before.toString());
                        System.out.println(after.toString());
                    }
                }
            }
        }
        return Optional.of(upd);
    }

    public Optional<Update> getUpdate(ASTNode before, ASTNode after) {
        Tree root1 = ASTUtils.getGumTreeContextForASTNode(before).map(TreeContext::getRoot).orElse(null);
        Tree root2 = ASTUtils.getGumTreeContextForASTNode(after).map(TreeContext::getRoot).orElse(null);
        return getUpdate(before, after, root1, root2);
    }

    public Optional<MatchReplace> getInstance(String before, String after, Tuple2<Integer, Integer> loc_b4,
                                           Tuple2<Integer, Integer> loc_aftr) {

        Optional<PerfectMatch> explanationBefore = matchesB4.containsKey(loc_b4) ? matchesB4.get(loc_b4)
                : PerfectMatch.getMatch(before);
        matchesB4.put(loc_b4, explanationBefore);
        if (explanationBefore.isEmpty()) return Optional.empty();

        Optional<PerfectMatch> explanationAfter = matchesAfter.containsKey(loc_aftr) ? matchesAfter.get(loc_aftr)
                : PerfectMatch.getMatch(after);
        matchesAfter.put(loc_aftr, explanationAfter);

        if (explanationAfter.isEmpty() || (explanationBefore.get().getName().equals(explanationAfter.get().getName())
                && Stream.of("Identifier", "ClassName","StringLiteral").anyMatch(x -> explanationAfter.get().getName().equals(x))))
            return Optional.empty();

        return Optional.of(new MatchReplace(explanationBefore.get(), explanationAfter.get()));
    }



    /*
    pattern: 1= pattern name, 2= pattern with holes
     */


    public static List<String> getAllTemplateVariableName(String template){

        return CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched().replace("\\\\", "\\"), z))))
                .map(t -> {
                    Environment x = t._2();
                    if (x.getValue().startsWith("[")) return x.getValue().substring(1, x.getValue().length() - 1);
                    else if (x.getValue().contains("~")) return x.getValue().substring(0, x.getValue().indexOf("~"));
                    else return x.getValue();
                })
                .collect(toList());

    }

}

