package Utilities;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import Utilities.comby.CombyMatch;
import Utilities.comby.CombyRewrite;
import Utilities.comby.Environment;
import Utilities.comby.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static Utilities.CaptureMappingsLike.STOCK_TVs;
import static java.util.stream.Collectors.*;

public class CombyUtils {

    public static Optional<CombyMatch> getMatch(String template, String source, String language) {

        template = template.replace("\\\"", "\"");
        source = source.replace("\\\"", "\"");

        try {
//            System.out.println("MATCH        "  + template + "       " + source);
            String result = getMatchCmd(template, source, language);
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

    public static String getMatchCmd(String template, String source, String language) throws IOException {
        String command = "echo '" + source + "' | comby '" + template + "' -stdin -json-lines -match-only -matcher "+
                (language == null ? ".java" : language)+
                " 'foo'";
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
        return template._2().test(source) ? getMatch(template._1(), source, language)
                .filter(cm -> isPerfectMatch(source, cm)) : Optional.empty();
    }

//    public static Optional<CombyMatch> getPerfectMatch(String template, String source, String language) {
//        return getMatch(template, source, language)
//                .filter(cm -> isPerfectMatch(source, cm));
//    }

    public static boolean isPerfectMatch(String source, CombyMatch cm) {
        return cm.getMatches().size() == 1
                && cm.getMatches().get(0).getMatched().equals(source.replace("\\\"", "\""));
    }


    public static boolean isPerfectMatch(String source, Match cm) {
        return cm.getMatched().equals(source.replace("\\\"", "\""));
    }
    //./RefactoringMiner -c /Users/ameya/Research/TypeChangeStudy/Corpus/Project_neo4j/neo4j 77a5e62f9d5a56a48f82b6bdd8519b18275bef1d -json /Users/ameya/Research/TypeChangeStudy/VanillaRMiner/output.json


    public static Tuple2<String, Map<String, String>>  renameTemplateVariable(String template, Function<String, String> rename){
        List<Tuple2<String, Environment>> allMatches = matchTemplateVariables(template)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched(), z))))
                .collect(toList());
        int n = allMatches.size();
        Map<String, String> renames = new HashMap<>();
        for(int i = 0; i < n; i++){
            Tuple2<String, Environment> t = allMatches.get(i);
            Environment env = t._2();

            String value;
            if (env.getValue().startsWith("[")) value = env.getValue().substring(1, env.getValue().length() - 1);
            else if(env.getValue().contains("~")) value = env.getValue().substring(0, env.getValue().indexOf("~"));
            else value = env.getValue();

            renames.put(value, rename.apply(value));
            String renamedTemplateVar = t._1().replace(value, renames.get(value));
            template = template.replace(t._1(), renamedTemplateVar);
            allMatches = matchTemplateVariables(template)
                    .stream().flatMap(z -> z.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                            .map(v -> Tuple.of(y.getMatched(), v))))
                    .collect(toList());
        }
        return Tuple.of(template, renames);

    }





    public static String  renameTemplateVariable(String template, Map<String, String> renames){

        if(renames.size() == 0)
            return template;

        renames = renames.entrySet().stream().filter(x->!x.getKey().equals(x.getValue()))
                .collect(toMap(x->x.getKey(), x->x.getValue()));

        List<Tuple2<String, Environment>> allMatches = CombyUtils.getMatch(":[:[var]]", template, null)
                .stream().flatMap(x -> x.getMatches().stream().flatMap(y -> y.getEnvironment().stream()
                        .map(z -> Tuple.of(y.getMatched().replace("\\\\","\\"), z))))
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
                            .map(v -> Tuple.of(y.getMatched().replace("\\\\","\\"), v))))
                    .collect(toList());
        }
        return template;

    }

    public static Optional<CombyMatch> matchTemplateVariables(String template) {
        return STOCK_TVs.containsKey(template) ? STOCK_TVs.get(template) :
                CombyUtils.getMatch(":[:[var]]", template, null);
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
//            System.out.println("REWRITE        "  + matcher + "       " + rewrite + "          " + source);
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
//            System.out.println("SUBSTITUTE        "  + template + substitutions.entrySet().stream().map(Tuple::fromEntry)
//                    .map(Tuple2::toString)
//                    .collect(joining(",")));

            Process p = Runtime.getRuntime().exec(cmd);
            String result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(joining("\n"));
//            CombyRewrite cr = new Gson().subsfromJson(result, CombyRewrite.class);
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

//    public static boolean isDecomposable(String template) {
//        return getAllTemplateVariableNames(template).stream().anyMatch(s -> s.endsWith("r"));
//    }


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
