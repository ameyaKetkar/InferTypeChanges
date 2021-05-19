package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.comby.Range__1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Map.Entry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class MatchReplace {

    private final PerfectMatch Match;
    private final PerfectMatch Replace;
    private final Map<String, String> TemplateVariableDeclarations;


    private final Map<String, String> UnMatchedBefore;
    private final Map<String, String> UnMatchedAfter;

    private final Map<String, Range__1> UnMatchedAfterRange;
    private final Map<String, Range__1> UnMatchedBeforeRange;

    // Renames the after to before in the after template
    public Tuple2<PerfectMatch, PerfectMatch> safeRename(PerfectMatch before, PerfectMatch after, Map<String, String> afterNameBeforeName){
        Map<String, String> templateVariableMapping = after.getTemplateVariableMapping();
        // conflict check
        List<String> conflictingBeforeNames = afterNameBeforeName.values().stream().filter(x -> templateVariableMapping.keySet().contains(x))
                .collect(toList());

        afterNameBeforeName = afterNameBeforeName.entrySet().stream()
                .collect(toMap(x -> x.getKey(), x -> conflictingBeforeNames.contains(x.getValue()) ? x.getValue() + "z": x.getValue()));

        Map<String, String> beforeNameNewBeforeName = conflictingBeforeNames.stream().collect(toMap(x -> x, x -> x + "z"));

        after = after.rename(afterNameBeforeName);

        if(!beforeNameNewBeforeName.isEmpty())
            before = before.rename(beforeNameNewBeforeName);

        return Tuple.of(before, after);
    }

    public MatchReplace(PerfectMatch before, PerfectMatch after){
        Map<String, String> intersectingTemplateVars = getIntersection(before, after);
        Tuple2<PerfectMatch, PerfectMatch> sf = safeRename(before, after, intersectingTemplateVars);
        before = sf._1; after = sf._2();
        intersectingTemplateVars = getIntersection(before, after);
    //        after = after.rename(intersectingTemplateVars);
        Optional<Either<String, String>> nxtDcmp = nextDecomposition(before, after,intersectingTemplateVars, null);
        int i = 6;
        while (nxtDcmp.isPresent() && i >= 0){
            if(nxtDcmp.get().isLeft())
                before = before.decompose(nxtDcmp.get().getLeft()).orElse(before);
            else
                after = after.decompose(nxtDcmp.get().get()).orElse(after);
            intersectingTemplateVars = getIntersection(before, after);
            sf = safeRename(before, after, intersectingTemplateVars);
    //        after = after.rename(intersectingTemplateVars);
            before = sf._1; after = sf._2();

            nxtDcmp = nextDecomposition(before, after, intersectingTemplateVars, nxtDcmp.get());
            i -= 1;

        }
        Collection<String> finalIntersectingTemplateVars = intersectingTemplateVars.values();
        PerfectMatch finalAfter = after;
        PerfectMatch finalBefore = before;
        this.TemplateVariableDeclarations = before.getTemplateVariableMapping().entrySet()
                    .stream().filter(x -> !x.getKey().endsWith("c") && finalIntersectingTemplateVars.contains(x.getKey()))
                    .collect(toMap(Entry::getKey, Entry::getValue));
        UnMatchedBefore = notInTemplateVariableDeclarations(before.getTemplateVariableMapping());
        UnMatchedBeforeRange = UnMatchedBefore.keySet().stream()
                .collect(toMap(x -> x, x -> finalBefore.getTemplateVariableMappingRange().get(x)));
        this.Match = before.substitute(UnMatchedBefore);
        UnMatchedAfter = notInTemplateVariableDeclarations(after.getTemplateVariableMapping());
        UnMatchedAfterRange = UnMatchedAfter.keySet().stream()
                .collect(toMap(x -> x, x -> finalAfter.getTemplateVariableMappingRange().get(x)));
        this.Replace = after.substitute(UnMatchedAfter);



//        System.out.println(this.getMatch().getTemplate() + " -> " + getReplace().getTemplate());
    }





    public Optional<Either<String, String>> nextDecomposition(PerfectMatch before, PerfectMatch after, Map<String, String> intersectingTemplateVars, Either<String, String> prev){
        for(var eb: before.getTemplateVariableMapping().entrySet()) {
            if (eb.getKey().endsWith("c") || intersectingTemplateVars.containsKey(eb.getKey()) || eb.getValue().isEmpty())
                continue;
            for (var ea : after.getTemplateVariableMapping().entrySet()) {
                if (ea.getKey().endsWith("c") || intersectingTemplateVars.containsKey(ea.getKey()) || ea.getValue().isEmpty())
                    continue;

                if(isContainedTokenize(eb.getValue(),ea.getValue())) {
                    Either<String, String> l = Either.left(eb.getKey());
                    if(!l.equals(prev))
                        return Optional.of(l);
                }
                if(isContainedTokenize(ea.getValue(),eb.getValue())) {
                    Either<String, String> r = Either.right(ea.getKey());
                    if(!r.equals(prev))
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isContainedTokenize(String largerSnippet, String shorterSnippet) {
        if(largerSnippet.equals(shorterSnippet)) return false;
        List<String> codeSnippetTokens = ASTUtils.getAllTokens(largerSnippet)
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        List<String> tvTokens = ASTUtils.getAllTokens(shorterSnippet)
                .stream().flatMap(x -> x.contains(".") ? Stream.of(x.split("\\.")) : Stream.of(x)).collect(toList());
        if (tvTokens.size() == 0 || codeSnippetTokens.size() == 0) {
            return largerSnippet.contains(shorterSnippet);
        }
        return Collections.indexOfSubList(codeSnippetTokens, tvTokens) != -1;
    }


    // Match template variables with the same value
    // Prefers matches with same key
    public Map<String, String> getIntersection(PerfectMatch before, PerfectMatch after) {
        Map<String, String> intersectingTemplateVars = new HashMap<>();

        for(var entry_b4: before.getTemplateVariableMapping().entrySet()){
            if(entry_b4.getKey().equals("c")) continue;
            String matchedTemplateVar = "";
            for(var entry_after: after.getTemplateVariableMapping().entrySet()){
                if(entry_after.getKey().equals("c")) continue;
                if(entry_b4.getValue().replace("\\n","")
                        .equals(entry_after.getValue().replace("\\n",""))){
                    if(matchedTemplateVar.isEmpty())
                        matchedTemplateVar = entry_after.getKey();
                    if(entry_b4.getKey().equals(entry_after.getKey())){
                        matchedTemplateVar = entry_after.getKey();
                        break;
                    }

                }
            }
            if(!matchedTemplateVar.isEmpty())
                intersectingTemplateVars.put(matchedTemplateVar, entry_b4.getKey());
        }
        return intersectingTemplateVars;
    }


    public PerfectMatch getMatch() {
        return Match;
    }

    public PerfectMatch getReplace() {
        return Replace;
    }

    public Map<String, String> getTemplateVariableDeclarations() {
        return TemplateVariableDeclarations;
    }


    public Map<String, String> notInTemplateVariableDeclarations(Map<String, String> tmap){
    return  tmap.entrySet().stream().filter(x -> !TemplateVariableDeclarations.containsKey(x.getKey())
                        && !TemplateVariableDeclarations.containsValue(x.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));

    }

    public Tuple2<String, String> getMatchReplace() {
        return Tuple.of(getMatch().getTemplate(), getReplace().getTemplate());
    }

//    public Map<String, String> getUnMatchedTemplateVarsBefore(){
//        return notInTemplateVariableDeclarations(Match.getTemplateVariableMapping());
//    }
//    public Map<String, String> getUnMatchedTemplateVarsAfter(){
//        return notInTemplateVariableDeclarations(Replace.getTemplateVariableMapping());
//    }

    public String getCodeSnippetB4() {
        return getMatch().getCodeSnippet();
    }

    public String getCodeSnippetAfter() {
        return getReplace().getCodeSnippet();
    }


    public Map<String, String> getUnMatchedBefore() {
        return UnMatchedBefore;
    }

    public Map<String, String> getUnMatchedAfter() {
        return UnMatchedAfter;
    }


    public Map<String, Range__1> getUnMatchedAfterRange() {
        return UnMatchedAfterRange;
    }


    public Map<String, Range__1> getUnMatchedBeforeRange() {
        return UnMatchedBeforeRange;
    }
}
