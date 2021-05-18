package type.change.treeCompare;

import Utilities.ASTUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class MatchReplace {

    private final PerfectMatch Match;
    private final PerfectMatch Replace;
    private final Map<String, String> TemplateVariableDeclarations;


    public MatchReplace(PerfectMatch before, PerfectMatch after){
        Map<String, String> intersectingTemplateVars = getIntersection(before, after);
        after = after.rename(intersectingTemplateVars);
        Optional<Either<String, String>> nxtDcmp = nextDecomposition(before, after,intersectingTemplateVars);
        while (nxtDcmp.isPresent()){
            if(nxtDcmp.get().isLeft())
                before = before.decompose(nxtDcmp.get().getLeft());
            else
                after = after.decompose(nxtDcmp.get().get());
            intersectingTemplateVars = getIntersection(before, after);
            after = after.rename(intersectingTemplateVars);
            nxtDcmp = nextDecomposition(before, after, intersectingTemplateVars);

        }
        Collection<String> finalIntersectingTemplateVars = intersectingTemplateVars.values();
        this.TemplateVariableDeclarations = before.getTemplateVariableMapping().entrySet()
                    .stream().filter(x -> !x.getKey().endsWith("c") && finalIntersectingTemplateVars.contains(x.getKey()))
                    .collect(toMap(x -> x.getKey(), x -> x.getValue()));
//                intersectingTemplateVars.entrySet().stream()
//                                    .collect(toMap(x -> x.getValue(), x -> ));
        Map<String, String> unMatchedBefore = notInTemplateVariableDeclarations(before.getTemplateVariableMapping());
        this.Match = before.substitute(unMatchedBefore);
        Map<String, String> unMatchedAfter = notInTemplateVariableDeclarations(after.getTemplateVariableMapping());
        this.Replace = after.substitute(unMatchedAfter);
        System.out.println(this.getMatch().getTemplate() + " -> " + getReplace().getTemplate());
    }



    public Optional<Either<String, String>> nextDecomposition(PerfectMatch before, PerfectMatch after, Map<String, String> intersectingTemplateVars){
        for(var eb: before.getTemplateVariableMapping().entrySet()) {
            if (eb.getKey().endsWith("c") || intersectingTemplateVars.containsKey(eb.getKey()) || eb.getValue().isEmpty())
                continue;
            for (var ea : after.getTemplateVariableMapping().entrySet()) {
                if (ea.getKey().endsWith("c") || intersectingTemplateVars.containsKey(ea.getKey()) || ea.getValue().isEmpty())
                    continue;
                if(isContainedTokenize(eb.getValue(),ea.getValue()))
                    return Optional.of(Either.left(eb.getKey()));
                if(isContainedTokenize(ea.getValue(),eb.getValue()))
                    return Optional.of(Either.right(ea.getKey()));
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
                if(entry_b4.getValue().equals(entry_after.getValue())){
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
    return  tmap.entrySet().stream().filter(x -> !TemplateVariableDeclarations.containsKey(x.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));

    }

    public Tuple2<String, String> getMatchReplace() {
        return Tuple.of(getMatch().getTemplate(), getReplace().getTemplate());
    }

    public Map<String, String> getUnMatchedTemplateVarsBefore(){
        return notInTemplateVariableDeclarations(Match.getTemplateVariableMapping());
    }
    public Map<String, String> getUnMatchedTemplateVarsAfter(){
        return notInTemplateVariableDeclarations(Replace.getTemplateVariableMapping());
    }

    public String getCodeSnippetB4() {
        return getMatch().getCodeSnippet();
    }

    public String getCodeSnippetAfter() {
        return getReplace().getCodeSnippet();
    }
}
