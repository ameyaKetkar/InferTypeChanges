package type.change.treeCompare;

import Utilities.CombyUtils;
import Utilities.Util;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import type.change.comby.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static type.change.comby.ExpressionPattern.getInstanceFrom;

abstract class AbstractExplanation {

    static CapturePatterns CAPTURE_PATTERNS = Util.readPatterns(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/CapturePatternsNew.json"));

    static String getPatternFor(String name) {
        return CAPTURE_PATTERNS.getExpressionPatterns()
                .stream().filter(x -> x.getName().equals(name)).findFirst().map(x -> x.getTemplate()).get();
    }

    static AbstractExplanation getInstance(String before, String after) {

        Optional<Tuple3<String, String, Match>> explanationBefore = getMatch(before, "ALL");

        if (explanationBefore.isEmpty())  return new NoExplanation();

        var explanationAfter = getMatch(after, explanationBefore.get()._1())
                .or(() -> getMatch(after, "ALL"));

        if (explanationAfter.isEmpty() || explanationBefore.get()._1().equals(explanationAfter.get()._1())
                && explanationAfter.get()._1().equals("Identifier"))  return new NoExplanation();

        return new Explanation(explanationBefore.get(), explanationAfter.get());
    }



    static Optional<Tuple3<String, String, Match>> getMatch(String source, String templateName) {

        Tuple2<ExpressionPattern, CombyMatch> basicMatch = CAPTURE_PATTERNS.getExpressionPatterns().stream()
                .filter(x -> templateName.equals("ALL") || x.getName().equals(templateName))
                .flatMap(x -> CombyUtils.getMatch(x.getTemplate(), source).stream().map(y -> Tuple.of(x, y)))
                .findFirst().orElse(null);

        if(basicMatch == null) return Optional.empty();

        if(!isPerfectMatch(source, basicMatch._2())){
            List<SubPattern> subPatterns = basicMatch._1().getSubPatterns();
            for(var var_values : subPatterns){
                Optional<CombyMatch> potentialMatch = Optional.empty();
                for(var val : var_values.getValues()){
                    String tryTemplate = CombyUtils.substitute(basicMatch._1().getTemplate(),
                            var_values.getVariable(), val);
                    potentialMatch = CombyUtils.getCompleteMatch(tryTemplate, source);
                    if(potentialMatch.isPresent()){
                        basicMatch = Tuple.of(getInstanceFrom(basicMatch._1(),tryTemplate), potentialMatch.get());
                        break;
                    }
                }
                if(potentialMatch.isPresent()) break;
            }
        }

        if (!isPerfectMatch(source, basicMatch._2)) {
            return Optional.empty();
        }


        Tuple2<ExpressionPattern, Match> refinedTemplate = basicMatch.map2(x -> x.getMatches().get(0));

        for (var template_var : refinedTemplate._2().getEnvironment()) {
            var sp = refinedTemplate._1().getSubPatternFor(template_var.getVariable());
            for (var sub_template : sp) {
                String tryTemplate = CombyUtils.substitute(refinedTemplate._1().getTemplate(), template_var.getVariable(), sub_template );
                Optional<CombyMatch> tryMatch = CombyUtils.getCompleteMatch(tryTemplate, source);
                if(tryMatch.isPresent() && tryMatch.get().getMatches().size() == 1){
                    refinedTemplate = Tuple.of(getInstanceFrom(basicMatch._1(),tryTemplate), tryMatch.get().getMatches().get(0));
                    break;
                }
            }
        }
        return Optional.of(Tuple.of(refinedTemplate._1().getName(), refinedTemplate._1().getTemplate(), refinedTemplate._2()));

    }

    private static boolean isPerfectMatch(String source, CombyMatch cm) {
        return cm.getMatches().size() == 1 && cm.getMatches().get(0).getMatched().equals(source);
    }
}
