package type.change;

import Utilities.InferredMappings;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class CleanMappings {

    private static <U,V> LinkedHashMap<U,V> sort(Map<U,V> m, Comparator<? super Map.Entry<U, V>> comparator){
        return m.entrySet().stream().sorted(comparator)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public static void main(String[] args) throws IOException {
        Map<Tuple2<String, String>, Map<Tuple2<String, String>, List<InferredMappings>>> updatedOp = Streams.concat(
                Stream.concat(
                        Files.readAllLines(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/finalExperimentOp.jsonl")).stream(),
                        Files.readAllLines(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/finalExperimentOpMore.jsonl")).stream()
                        ),
                Files.readAllLines(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/finalExperimentOp2.jsonl"))
                        .stream())
                .map(x -> new Gson().fromJson(x, InferredMappings.class))
                .filter(x -> x != null && x.getInstance() != null)
                .peek(x -> x.getInstance().reComputeIsSafe())
                .collect(groupingBy(x -> Tuple.of(x.getBeforeTypeTemplate(), x.getAfterTypeTemplate()),
                        collectingAndThen(toList(),
                                ps -> sort(ps.stream().collect(groupingBy(y -> Tuple.of(y.getMatch(), y.getReplace()))),
                                        Comparator.comparingInt(z -> z.getValue().size())))));

        updatedOp = sort(updatedOp, Comparator.comparingInt(z -> z.getValue().size()));
        Map<Tuple2<String, String>, Map<Tuple2<String, String>, List<InferredMappings>>> finalOp = new HashMap<>();
        for(var tc_rules : updatedOp.entrySet()){
            var tc = tc_rules.getKey();
            Map<Tuple2<String, String>, List<InferredMappings>> rules = tc_rules.getValue();

            Map<Tuple2<String, String>, List<InferredMappings>> rls = rules.entrySet().stream()
                    .filter(x -> !rules.containsKey(x.getKey().map(z -> z.replace(":[TCIVar]=" ,""), z -> z.replace(":[TCIVar]=" ,"")))
                            || (rules.containsKey(x.getKey().map(z -> z.replace(":[TCIVar]=" ,""), z -> z.replace(":[TCIVar]=" ,"")))
                            &&  rules.get(x.getKey().map(z -> z.replace(":[TCIVar]=" ,""), z -> z.replace(":[TCIVar]=" ,""))).size()
                            <= x.getValue().size())
                    )
                    .collect(toMap(x -> x.getKey(), x -> x.getValue()));
            finalOp.put(tc, rls);

        }
        List<String> op = finalOp.values().stream()
                            .flatMap(x -> x.values().stream().flatMap(y -> y.stream()))
                .map(y -> new Gson().toJson(y, InferredMappings.class))
                .collect(toList());

        Files.write(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/finalExperimentOpUpdatedMore.jsonl"),
                op, StandardOpenOption.CREATE);



    }

}
