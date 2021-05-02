package Utilities;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import type.change.comby.CombyMatch;
import type.change.comby.CombyRewrite;
import type.change.comby.Environment;
import type.change.comby.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class CombyUtils {

    public static Optional<CombyMatch> getMatch(String template, String source, String language) {

        template = template.replace("\\\"", "\"");
        source = source.replace("\\\"", "\"");

        try {
            String command = "echo '" + source + "' | comby '" + template + "' -stdin -json-lines -match-only -matcher "+
                    (language == null ? ".java" : "")+
                    " 'foo'";
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    command
            };
            Process p = Runtime.getRuntime().exec(cmd);

            String result = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().collect(joining("\n"));
            CombyMatch cm = new Gson().fromJson(result, CombyMatch.class);

            if(cm != null) {
                cm.getMatches().forEach(c -> c.setMatched(c.getMatched().replace("\\\"", "\"")));
            }

            return Optional.ofNullable(cm);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
    public static Optional<CombyMatch> getPerfectMatch(String template, String source) {
        return getMatch(template, source, null)
                .filter(cm -> isPerfectMatch(source, cm));
//                .filter(cm -> cm.getMatches().size() == 1)
//                .filter(cm -> cm.getMatches().get(0).getMatched().equals(source.replace("\\\"", "\"")));

    }

    public static boolean isPerfectMatch(String source, CombyMatch cm) {
        return cm.getMatches().size() == 1
                && cm.getMatches().get(0).getMatched().equals(source.replace("\\\"", "\""));
    }


    public static List<String> getAllTemplateVariableNames(String template){
        List<String> allMatches = CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()))
                .map(x -> {
                    if (x.getValue().startsWith("[")) return x.getValue().substring(1, x.getValue().length() - 1);
                    else if (x.getValue().contains("~")) return x.getValue().substring(0, x.getValue().indexOf("~"));
                    else return x.getValue();
                }).collect(toList());

        return allMatches;

    }


    public static String  renameTemplateVariable(String template, Map<String, String> renames){
        List<Tuple2<String, Environment>> allMatches = CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched(), z))))
                .collect(toList());
        int n = allMatches.size();

        for(int i = 0; i < n; i++){
            Tuple2<String, Environment> t = allMatches.get(i);
            Environment x = t._2();
            String value;
            if (x.getValue().startsWith("[")) value = x.getValue().substring(1, x.getValue().length() - 1);
            else if(x.getValue().contains("~")) value = x.getValue().substring(0, x.getValue().indexOf("~"));
            else value = x.getValue();
            if(renames.containsKey(value)){
                String renamedTemplateVar = t._1().replace(value, renames.get(value));
                template = template.replace(t._1(), renamedTemplateVar);
            }
            allMatches = CombyUtils.getMatch(":[:[var]]", template, null)
                    .stream().flatMap(z -> z.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                            .map(v -> Tuple.of(y.getMatched(), v))))
                    .collect(toList());
        }
        return template;

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



    public static String substitute(String template, String s1, String s2) {
        return substitute(template, new HashMap<>(){{put(s1, s2);}});
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
//            CombyRewrite cr = new Gson().subsfromJson(result, CombyRewrite.class);
            if (result == null){
                System.out.println("No substitution found");
                result = template;
            }
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
        return stmtAftr;
    }

    public static boolean isDecomposable(Tuple3<String, String, Match> x) {
        return getAllTemplateVariableNames(x._2()).stream().anyMatch(s -> s.endsWith("r"));
    }


    public static class SubstitutionInput {
        public String variable;
        public String value;

        public SubstitutionInput(String variable, String value){
            this.variable = variable;//hackVarName(variable);
            this.value = value;
        }

        //TODO: Don't need this hack any more? then remove!
        public String hackVarName(String variable){
            var renames = new HashMap<String, String>() {{
                put("9x1c", "~([A-Z][a-z0-9]+)+");
                put("43c", "~[A-Z]\\w*");
                put("46c", "~[A-Z]\\w*");
                put("33c", "~\\s*");
                put("29c","~[\\+\\-\\*\\&]");
                put("50","~\\d");
                put("51","~\\d");
                put("52","~[L-l]");
                put("55","~[L-l]");
                put("53","~0[xX][0-9a-fA-F]+");
                put("54","~0[xX][0-9a-fA-F]+");

            }};
            return renames.containsKey(variable) ? variable + renames.get(variable) : variable;
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


    public static void main(String[] args){

        String renamedTemplate = renameTemplateVariable(":[9x1c~([A-Z][a-z0-9]+)+].:[[10c]]:[12](:[11])", new HashMap<>() {{
            put("9x1c", "29x1c");
            put("10c", "2x10c");
            put("12", "2x12");
            put("11", "2x11");
        }});

        System.out.println(renamedTemplate);
        String res = substitute(":[1] :[2]", new HashMap<>() {{
            put("1", "Ameya");
            put("2", "Ketkar");
        }});
        System.out.println(res);

    }

    public static String getAsCombyVariable(String x){
        return x.contains(":[") ? x : ":["+x+"]";
    }
}
