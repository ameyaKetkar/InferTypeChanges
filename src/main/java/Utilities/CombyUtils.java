package Utilities;

import Utilities.comby.CombyMatch;
import Utilities.comby.CombyRewrite;
import Utilities.comby.Environment;
import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import type.change.treeCompare.PerfectMatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.*;

public class CombyUtils {

    public static Optional<CombyMatch> getMatch(String template, String source, String language) {

        template = template.replace("\\\"", "\"");
        source = source.replace("\\\"", "\"");//;.replace("\\","\\\\");
//        source = RWUtils.escapeMetaCharacters(source);

        try {
            boolean flag = false;
//            System.out.println("MATCH        "  + template + "       " + source);
            String result = getMatchCmd(template, source, language, null);
            if(result == null || result.isEmpty()){
                result = getMatchCmd(template, source.replace("\\", "\\\\"), language, null);
                flag = true;
            }
            CombyMatch cm = new Gson().fromJson(result, CombyMatch.class);
            if(cm != null) {
                cm.getMatches().forEach(c -> c.setMatched(c.getMatched().replace("\\\"", "\"")));
                if (flag)
                    cm.getMatches().forEach(c -> c.setMatched(c.getMatched().replace("\\\\", "\\")));
            }

            return Optional.ofNullable(cm);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static String getMatchCmd(String template, String source, String language, String rule) throws IOException {
        String command;
        if(rule !=null){
            command = "echo '" + source + "' | comby '" + template + "' -rule " + "'" + rule + "'"
                    + " -stdin -json-lines -match-only -matcher "+ (language == null ? ".java" : language)+
                    " 'foo'";
        }else {
            command = "echo '" + source + "' | comby '" + template + "' -stdin -json-lines -match-only -matcher " +
                    (language == null ? ".java" : language) +
                    " 'foo'";
        }
        String[] cmd = {
                "/bin/sh",
                "-c",
                command
        };

        Process p = Runtime.getRuntime().exec(cmd);


        String result = new BufferedReader(new InputStreamReader(p.getInputStream()))
                .lines().collect(joining("\n"));
        return result;
    }


    public static Optional<CombyMatch> getPerfectMatch(Tuple2<String, Predicate<String>> template, String source, String language) {
        if (template._2().test(source))
            return getMatch(template._1(), source, language)
                .filter(cm -> isPerfectMatch(source, cm));
        return Optional.empty();
    }

    public static Optional<CombyMatch> getPerfectMatch(String template, String source, String language) {
        return getMatch(template, source, language)
                .filter(cm -> isPerfectMatch(source, cm));
    }

//    public static Optional<CombyMatch> removeRedundantTemplateVariables(PerfectMatch m){
//        Map<String, Long> valueToTVs = m.getTemplateVariableMapping().entrySet().stream().collect(groupingBy(x -> x.getValue(), counting()));
//        if (valueToTVs.values().stream().anyMatch(x->x>1)){
//            System.out.println();
//        }
//
//    }


    public static boolean isPerfectMatch(String source, CombyMatch cm) {
        return cm.getMatches().size() == 1
                && cm.getMatches().get(0).getMatched().replace("\\n","")
                .equals(source.replace("\\\"", "\"")
                        .replace("\\n",""));

    }

    public static Optional<CombyRewrite> rewrite(String matcher, String rewrite, String source) {
        try {
            matcher = matcher.replace("\\\"", "\"");
            rewrite = rewrite.replace("\\\"", "\"");
            String command = "echo '" + source + "' | comby '" + matcher + "'" + " '"+ rewrite +"' " + " -stdin -json-lines .java";
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    command
            };
            Process p = Runtime.getRuntime().exec(cmd);
            String result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(joining("\n"));
            CombyRewrite cr = new Gson().fromJson(result, CombyRewrite.class);
            return Optional.ofNullable(cr);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }



    public static String substitute(String template, String tv, String val) {
        return substitute(template, new HashMap<>(){{put(tv, val);}});
    }

    public static String substitute(String template, Map<String, String> substitutions) {
        try {
            String subs = substitutions.entrySet().stream().map(x -> new SubstitutionInput(x.getKey(), x.getValue()))
                    .map(x -> x.toString())
                    .collect(joining(",")).replace("\n","");
            String command = "comby '' '" + template + "'" + " -substitute '["+ subs +"]'";
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    command
            };
            Process p = Runtime.getRuntime().exec(cmd);
            String result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(joining("\n"));
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return template;
        }
    }

    public static String performIdentifierRename(String nameB4, String nameAfter, String stmtAftr) {
        // Perform rename variable for the TCI in the code mapping, to make it consistent
        if(!nameB4.equals(nameAfter)){
            Optional<CombyRewrite> cr = rewrite(":[1~\\b" + nameAfter + "\\b]", nameB4, stmtAftr);
            if(cr.isPresent()){
                stmtAftr = cr.get().getRewrittenSource();
            }
        }
        return stmtAftr.replace("\n","");
    }

    public static List<String> getAllTemplateVariableName(String template){

        return getMatch(":[:[var]]", template, null)
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
    
    public static class SubstitutionInput {
        public String variable;
        public String value;

        public SubstitutionInput(String variable, String value){
            this.variable = variable;
            this.value = value;
        }


        public void setVariable(String variable) {
            this.variable = variable;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getVariable() {
            return variable;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    public static String getAsCombyVariable(String x){
        return x.contains(":[") ? x : ":["+x+"]";
    }



}
