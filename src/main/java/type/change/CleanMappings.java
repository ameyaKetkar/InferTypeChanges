package type.change;

import Utilities.CombyUtils;
import Utilities.InferredMappings;
import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import type.change.comby.CombyMatch;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static Utilities.RWUtils.outputFolder;
import static java.util.stream.Collectors.*;

public class CleanMappings {

    public static void main(String[] args) throws IOException {
        List<InferredMappings> cleanMappings = new ArrayList<>();

        Map<Tuple2<String, String>, InferredMappings> collectedMappings = Files.readAllLines(outputFolder.resolve("FileToPath.jsonl"))
                .stream().map(x -> new Gson().fromJson(x, InferredMappings.class))
                .peek(InferredMappings::isUsageMapping)
                .collect(toMap(x -> Tuple.of(x.getMatch(null), x.getReplace()), x -> x));



        Map<Tuple2<String, String>, List<Tuple3<String, String, List<InferredMappings.Instance>>>> explains = new HashMap<>();

        for(var c1: collectedMappings.entrySet()){
               for(var c2: collectedMappings.entrySet()){
                   if(!c1.getKey().equals(c2.getKey())){

                   }
               }
        }

        // I can figure out if something is a usage or not!
        // IF SOMETHING CONTAINS NO TEMPLATE VARIABLES AND OCCURS ONLY ONCE, IT COULD BE REMOVED (NOT ::)
        //



        /**
         * Can be usages or not
         *  If usages, the appropriate tv in the match template shud be constrained with a regex \bvarName\b
         *  If not usage,
         *      - Something that contains a usage, and could not be inferred
         *          Infer the usage and discard this
         *      - initailizer
         *
         *
         */
//        List<Tuple2<InferredMappings, Optional<String>>> mapping_usagesTV = collectedMappings.values().stream().map(x -> Tuple.of(x, isUsageMapping(x)))
//                .collect(toList());

//
//        var areUsages = collectedMappings.values().stream()
//                .collect(groupingBy(CleanMappings::isUsageMapping, collectingAndThen(toList(),
//                                ls -> ls.stream().map(x -> Tuple.of(x.getMatch(), x.getReplace())).collect(toList()))));
//
//        for(var usage: areUsages.get(true)){
//            for(var notUsage: areUsages.get(false)){
//                if()
//            }
//        }
//

        System.out.println();
    }

//    public static String getUsageTV(InferredMappings i){
//        Instance instance = i.getInstances().get(0);
//        CombyMatch cm_m = CombyUtils.getMatch(i.getMatch(), instance.getBefore()).get();
//        cm_m.getMatches().get(0).getEnvironment().stream()
//                .filter(x -> x.getValue().equals(instance.getNames()._1()));
//    }




    public static boolean explains(InferredMappings explainer, InferredMappings explainee){
        // explainer found in explainee
        if(explainee.getCapturesUsage() != null || explainer.getCapturesUsage() == null) return false;

        for(var i: explainee.getInstances()){

        }

        Optional<CombyMatch> cm_m = CombyUtils.getMatch(explainer.getMatch(), explainee.getMatch(), null);
        if(cm_m.isPresent()){
            Optional<CombyMatch> cm_r = CombyUtils.getMatch(explainer.getReplace(), explainee.getReplace(), null);
            if(cm_r.isPresent()){

            }
        }
        return false;
    }
}