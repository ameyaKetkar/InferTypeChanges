package type.change.treeCompare;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.RWUtils;
import Utilities.comby.CombyMatch;
import Utilities.comby.Match;
import Utilities.comby.Range__1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.eclipse.jdt.core.dom.ASTNode;
import type.change.T2RLearnerException;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static Utilities.CaptureMappingsLike.*;
import static Utilities.CombyUtils.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PerfectMatch {

    private final String Name;
    private String Template;
    private final Match Match;
    private final String CodeSnippet;

    public PerfectMatch(String name, String template, Match match) {
        Name = name;
        Template = template;
        Match = match;
        CodeSnippet = match.getMatched();
    }

    public PerfectMatch(String name, String template, CombyMatch cm){
        this(name,template, cm.getMatches().get(0));
    }

    // Renames the after to before in the after template
    public static Tuple2<PerfectMatch, PerfectMatch> safeRename(PerfectMatch before, PerfectMatch after, Map<String, String> afterNameBeforeName){
        Map<String, String> templateVariableMapping = after.getTemplateVariableMapping();
        // conflict check
        List<String> conflictingBeforeNames = afterNameBeforeName.values().stream()
                .filter(templateVariableMapping::containsKey)
                .collect(toList());

        Function<String, String> addSuffix = s -> s.endsWith("c") ? s.replace("c", "zc") : s + "z";

        afterNameBeforeName = afterNameBeforeName.entrySet().stream()
                .collect(toMap(Entry::getKey, x -> conflictingBeforeNames
                        .contains(x.getValue()) ? addSuffix.apply(x.getValue()): x.getValue()));

        Map<String, String> beforeNameNewBeforeName = conflictingBeforeNames.stream()
                .collect(toMap(x -> x, addSuffix));

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
        return new PerfectMatch(p.getName(), template, match);
    }


    public PerfectMatch updateTemplate(String newTemplate) throws T2RLearnerException {
        return getPerfectMatch(newTemplate, CodeSnippet, null)
                .or(() -> getPerfectMatch(newTemplate, CodeSnippet, ".xml"))
                .map(x -> new PerfectMatch(Name, newTemplate, x))
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

    public Map<String, Range__1> getTemplateVariableMappingRange(){
        return Match.getTemplateVarSubstitutionsRange();
    }

    public String getCodeSnippet() {
        return CodeSnippet;
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
                .map(x->new PerfectMatch(Name + "-" + decomposedTemplate.get().getName(), tryTemplate, x));
    }

    static Optional<PerfectMatch> getMatch(ASTNode astNode) {
        List<String> templatesFor = getTemplatesFor(astNode);
        String source = astNode.toString();
        for(var template: templatesFor){
            Optional<CombyMatch> match;
            if(template.contains(":[r]")){
                String newTemplate = template.replace(":[r]", ":[r~" + RWUtils.escapeMetaCharacters(getTheUnBoundedTemplateVar(astNode)) + "]");
                match = CombyUtils.getMatch(newTemplate, source, null);
            }
            else{
                match = CombyUtils.getMatch(template, source, null);
            }
            if (match.isPresent() && isPerfectMatch(source, match.get()))
                return Optional.of(new PerfectMatch(template, template, match.get()));
        }
        return Optional.empty();
    }

}
