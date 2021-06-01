package type.change;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import type.change.GenerateResolvedResponse.ResolvedResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class AnalyzeResolvedResponse {

    public static void main(String[] a) throws IOException {
        List<ResolvedResponse> commits = Files.walk(GenerateResolvedResponse.pathToResolvedCommits)
                .filter(Files::isRegularFile)
                .map(x -> Try.of(() -> Files.readString(x))
                        .map(s -> new Gson().fromJson(s, ResolvedResponse.class))
                      .onFailure(Throwable::printStackTrace)
                        .getOrElse(() -> {
                            System.out.println("Could not parse the file " + x);
                            return null;
                        })).filter(Objects::nonNull)
                .collect(Collectors.toList());


        List<Map.Entry<Tuple2<String, String>, Set<Tuple2<String,String>>>> typeChanges = commits.stream()
                .flatMap(x -> x.getResolvedTypeChanges().stream().map(y -> Tuple.of(y._2(), x.commits.get(0).sha1, x.commits.get(0).repository)))
                .collect(groupingBy(Tuple3::_1, collectingAndThen(toList(), ys -> ys.stream().map(x -> Tuple.of(x._2(), x._3())).collect(toSet()))))
                .entrySet().stream().sorted(Comparator.comparingInt(x -> x.getValue().size()))
                .collect(toList());
        Collections.reverse(typeChanges);



        System.out.println();
    }

}
