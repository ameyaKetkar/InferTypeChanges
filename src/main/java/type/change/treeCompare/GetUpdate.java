package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import Utilities.comby.Environment;
import org.refactoringminer.RMinerUtils;
import org.refactoringminer.RMinerUtils.TypeChange;

import java.util.*;
import java.util.stream.Stream;

import static Utilities.ASTUtils.getChildren;
import static Utilities.ASTUtils.getCoveringNode;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.*;

public class GetUpdate {


    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesB4;
    private Map<Tuple2<Integer, Integer>, Optional<PerfectMatch>> matchesAfter;
    private final CodeMapping codeMapping;
    private final TypeChange typeChange;

    public GetUpdate(CodeMapping codeMapping, TypeChange typeChange) {
        this.codeMapping = codeMapping;
        this.typeChange = typeChange;
        matchesB4 = new HashMap<>();
        matchesAfter = new HashMap<>();

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

