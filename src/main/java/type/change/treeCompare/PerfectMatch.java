package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.RWUtils;
import Utilities.comby.CombyMatch;
import Utilities.comby.Match;
import Utilities.comby.Range__1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.dom.ASTNode;
import type.change.T2RLearnerException;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static Utilities.CaptureMappingsLike.getTemplatesFor;
import static Utilities.CaptureMappingsLike.getTheUnBoundedTemplateVar;
import static Utilities.CombyUtils.getPerfectMatch;
import static Utilities.CombyUtils.isPerfectMatch;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PerfectMatch {

    private final String Name;
    private String Template;
    private final Match Match;
    private final String CodeSnippet;

    public String getAssistance() {
        return assistance;
    }

    private final String assistance;

    public PerfectMatch(String name, String template, Match match, String assistance) {
        Name = name;
        Template = template;
        Match = match;
        CodeSnippet = match.getMatched().replace("\n","");
        this.assistance=assistance;
        Map<String, String> mergeSameValuedTemplateVars = getTemplateVariablesWithSameValue();
        if(!mergeSameValuedTemplateVars.isEmpty())
            rename(mergeSameValuedTemplateVars);
    }

    public Map<String, String> getTemplateVariablesWithSameValue() {
        Set<Set<String>> mergeSameValuedTemplateVars = new HashSet<>();
        for(var e1: Match.getTemplateVarSubstitutions().entrySet()){
            for(var e2: Match.getTemplateVarSubstitutions().entrySet()){
                if(e1.getKey().equals(e2.getKey())) continue;
                if(e1.getValue().equals("") || !e1.getValue().equals(e2.getValue())) continue;
                Optional<Set<String>> s = mergeSameValuedTemplateVars.stream().filter(x -> x.contains(e1.getKey()) || x.contains(e2.getKey()))
                        .findFirst();
                if(s.isPresent()){
                    mergeSameValuedTemplateVars.stream().filter(x -> x.contains(e1.getKey()) || x.contains(e2.getKey()))
                            .findFirst().get().add(e1.getKey());
                    mergeSameValuedTemplateVars.stream().filter(x -> x.contains(e1.getKey()) || x.contains(e2.getKey()))
                            .findFirst().get().add(e2.getKey());
                }else{
                    mergeSameValuedTemplateVars.add(new HashSet<>(){{add(e1.getKey());e2.getKey();}});
                }
            }
        }
        Map<String, String> renameSimilarValuedTVs = new HashMap<>();
        for(var s: mergeSameValuedTemplateVars){
            if(s.size() <= 1) continue;
            String fst = s.contains("TCIVar") ? "TCIVar" : s.iterator().next();
            s.stream().filter(x -> !x.equals(fst))
                        .forEach(r -> renameSimilarValuedTVs.put(r, fst));

        }


        return renameSimilarValuedTVs;
    }

    public PerfectMatch(String name, String template, CombyMatch cm, String r){
        this(name,template, cm.getMatches().get(0),r);
    }

    // Renames the after to before in the after template
    public static Tuple3<PerfectMatch, PerfectMatch, Map<String, List<String>>> generalize(PerfectMatch before, PerfectMatch after){
        Map<String, List<String>> intersection = getIntersection(before, after);
        Map<String, String> afterNameBeforeName = intersection.entrySet().stream().flatMap(x -> x.getValue().stream().map(y -> Tuple.of(y, x.getKey())))
                .collect(toMap(Tuple2::_1, Tuple2::_2, (a, b)->a));
        Tuple2<PerfectMatch, PerfectMatch> t1 = safeRename(before, after, afterNameBeforeName);
        return t1.concat(Tuple.of(getIntersection(t1._1(), t1._2())));
    }

    private static Tuple2<PerfectMatch, PerfectMatch> safeRename(PerfectMatch before, PerfectMatch after, Map<String, String> afterNameBeforeName) {
        Map<String, String> templateVariableMapping = after.getTemplateVariableMapping();
        // conflict check
        List<String> conflictingBeforeNames = Stream.concat(afterNameBeforeName.values().stream(),afterNameBeforeName.keySet().stream())
                .filter(templateVariableMapping::containsKey)
                .collect(toList());

        Function<String, String> addSuffix = s -> s.endsWith("c") ? s.replace("c", "zc") : s + "z";

        afterNameBeforeName = afterNameBeforeName.entrySet().stream()
                .collect(toMap(Entry::getKey, x -> conflictingBeforeNames
                        .contains(x.getValue()) ? addSuffix.apply(x.getValue()): x.getValue()));

        Map<String, String> beforeNameNewBeforeName = conflictingBeforeNames.stream()
                .collect(toMap(x -> x, addSuffix, (a,b) -> a));

        after.rename(afterNameBeforeName);

        if(!beforeNameNewBeforeName.isEmpty())
            before.rename(beforeNameNewBeforeName);

        return Tuple.of(before, after);
    }

    public void rename(Map<String, String> renames){
        for(var r: renames.entrySet()){
            Template = Template.replace("["+r.getKey()+"~","["+r.getValue()+"~" )
                    .replace("["+r.getKey()+"]","["+r.getValue()+"]" )
                    .replace("["+r.getKey()+":","["+r.getValue()+":" );
            Match.getEnvironment().stream().filter(x->x.getVariable().equals(r.getKey())).findFirst()
                    .ifPresent(x -> x.setVariable(r.getValue()));
        }
    }


    public static PerfectMatch renamedInstance(Map<String, String> renames, PerfectMatch p){
        String template = p.getTemplate();
        for(var r: renames.entrySet()){
            template = template.replace("[" + r.getKey() + "~", "[" + r.getValue() + "~")
                    .replace("[" + r.getKey() + "]", "[" + r.getValue() + "]")
                    .replace("[" + r.getKey() + ":", "[" + r.getValue() + ":");
        }
        Match match = Utilities.comby.Match.renamedInstance(p.getMatch(), renames);
        return new PerfectMatch(p.getName(), template, match, p.assistance);
    }


    public PerfectMatch updateTemplate(String newTemplate) throws T2RLearnerException {
        return getPerfectMatch(newTemplate, CodeSnippet, null)
                .or(() -> {
                    if(assistance!= null) {
                        var a = RWUtils.unEscapeMetaCharacters(assistance);
                        var r = Match.getTemplateVarSubstitutions().entrySet().stream()
                                .filter(x->x.getValue().equals(a))
                                .findFirst();
                        if(r.isPresent()) {
                            String nt = newTemplate.replace(":[" + r.get().getKey() + "]", ":[" + r.get().getKey() + "~" + assistance +"]");
                            return getPerfectMatch(nt, CodeSnippet, null);
                        }
                    }
                    return Optional.empty();
                })
                .or(() -> getPerfectMatch(newTemplate, CodeSnippet, ".xml"))
                .map(x -> new PerfectMatch(Name, newTemplate, x, null))
                .orElseThrow(() -> new T2RLearnerException("Could not Update template. " + newTemplate + " did not perfectly match" + CodeSnippet));
    }


    public String getName() {
        return Name;
    }

    public String getTemplate() {
        return Template;
    }

    public Match getMatch() {
        return Match;
    }

    public Map<String, String> getTemplateVariableMapping(){
        return Match.getTemplateVarSubstitutions();
    }

    public Map<String, List<Range__1>> getTemplateVariableMappingRange(){
        return Match.getTemplateVarSubstitutionsRange();
    }

    public String getCodeSnippet() {
        return CodeSnippet;
    }

    public static PerfectMatch completelyDecompose(PerfectMatch x){
        PerfectMatch m = x;
        for(var v: x.getTemplateVariableMapping().entrySet()){
            m = PerfectMatch.completelyDecompose(m, v.getKey());
        }
        return m;
    }

    private static PerfectMatch completelyDecompose(PerfectMatch m, String templateVariable){
        Queue<String> tvs = new ArrayDeque<>();
        tvs.add(templateVariable);
        int i = 4;
        while(!tvs.isEmpty() && i >= 0) {
            String tv = tvs.remove();
            String decomposeSnippet = m.Match.getTemplateVarSubstitutions().get(tv);
            Optional<PerfectMatch> decomposedTemplate = ASTUtils.getExpression(decomposeSnippet).flatMap(PerfectMatch::getMatch)
                    .filter(x -> !x.getName().equals(":[[id]]"));
            if (decomposedTemplate.isEmpty())
                return m;
            Map<String, String> renames = decomposedTemplate.get().getTemplateVariableMapping()
                    .keySet().stream().collect(toMap(x -> x, x -> tv + "x" + x));
            tvs.addAll(renames.values());
            String newTemplate = renamedInstance(renames, decomposedTemplate.get()).getTemplate();
            String tryTemplate = CombyUtils.substitute(m.Template, tv, newTemplate);
            var t = getPerfectMatch(tryTemplate,m.CodeSnippet, null);
            if(t.isPresent()){
                m = new PerfectMatch(m.Name + "-" + decomposedTemplate.get().getName(), tryTemplate, t.get(), null);
            }
            i-=1;
        }
        return m;
    }

    public Optional<PerfectMatch> decompose(String templateVariable){
        String decomposeSnippet = Match.getTemplateVarSubstitutions().get(templateVariable);
        Optional<PerfectMatch> decomposedTemplate = ASTUtils.getExpression(decomposeSnippet).flatMap(PerfectMatch::getMatch);
        if(decomposedTemplate.isEmpty())
            return Optional.empty();
        Map<String, String> renames = decomposedTemplate.get().getTemplateVariableMapping()
                .keySet().stream().collect(toMap(x -> x, x -> templateVariable + "x" + x));
        String newTemplate = renamedInstance(renames, decomposedTemplate.get()).getTemplate();
        String tryTemplate = CombyUtils.substitute(Template, templateVariable, newTemplate);
        return getPerfectMatch(tryTemplate,CodeSnippet,null)
                .map(x->new PerfectMatch(Name + "-" + decomposedTemplate.get().getName(), tryTemplate, x, null));
    }

    static Optional<PerfectMatch> getMatch(ASTNode astNode) {
        Set<String> templatesFor = getTemplatesFor(astNode);
        String source = astNode.toString();
        for(var template: templatesFor){
            Optional<CombyMatch> match;
            String r = "";
            if(template.contains(":[r]")){
                r = RWUtils.escapeMetaCharacters(getTheUnBoundedTemplateVar(astNode));
                String newTemplate = template.replace(":[r]", ":[r~" + r + "]");
                match = CombyUtils.getMatch(newTemplate, source, null);
            }
            else{
                match = CombyUtils.getMatch(template, source, null);
            }
            if (match.isPresent() && isPerfectMatch(source, match.get())) {
                return Optional.of(new PerfectMatch(template, template, match.get(), r));
            }
        }
        return Optional.empty();
    }

    public static Map<String, List<String>> getIntersection(PerfectMatch before, PerfectMatch after) {
        Map<String, List<String>> intersectingTemplateVars = new HashMap<>();

        for (var entry_b4 : before.getTemplateVariableMapping().entrySet()) {
            if (entry_b4.getKey().endsWith("c")) continue;
            List<String> matchedTemplateVars = new ArrayList<>();
            for (var entry_after : after.getTemplateVariableMapping().entrySet()) {
                if (entry_after.getKey().endsWith("c")) continue;
                if (entry_b4.getValue().replace("\\n", "")
                        .equals(entry_after.getValue().replace("\\n", ""))) {
                    matchedTemplateVars.add(entry_after.getKey());

                }
            }
            if (!matchedTemplateVars.isEmpty())
                intersectingTemplateVars.put(entry_b4.getKey(), matchedTemplateVars);
        }
        return intersectingTemplateVars;
    }

}
