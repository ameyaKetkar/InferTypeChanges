package type.change;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.refactoringminer.RMinerUtils;
import type.change.GenerateResolvedResponse.ResolvedResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utilities.ASTUtils.isNotWorthLearning;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toSet;
import static type.change.GenerateResolvedResponse.pathToResolvedCommits;
import static type.change.CommitMode.getAsCodeMapping;

public class TheExperiment {
    
    
    public static void main(String a[]) throws IOException {




        List<String> testProjects = Files.readAllLines(Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/testProjects.txt"))
                .stream().map(x->x.split(",")[0]).collect(toList());

        List<ResolvedResponse> allResolvedCommits = Files.list(pathToResolvedCommits).parallel()
                .filter(x->x.getFileName().toString().contains(".json"))
                .flatMap(x -> Try.of(() -> new Gson().fromJson(Files.readString(x), ResolvedResponse.class))
                    .onFailure(Throwable::printStackTrace)
                    .toJavaOptional().stream())
                .collect(Collectors.toList());

        Function<? super ResolvedResponse, Boolean> isInTest = r -> {
            String url = r.commits.get(0).repository;
            String prName = url.split("/")[url.split("/").length - 1].replace(".git", "");
            return testProjects.contains(prName);
        };

        Map<Boolean, List<ResolvedResponse>> test_training = allResolvedCommits.stream().collect(groupingBy(isInTest));
        List<ResolvedResponse> testData = test_training.get(true);
        List<ResolvedResponse> trainingData = test_training.get(false);

        var typeChanges_testData = testData.stream().flatMap(x -> x.getResolvedTypeChanges().stream())
                .collect(toSet());

        var typeChanges_trainingData = trainingData.stream().flatMap(x -> x.getResolvedTypeChanges().stream())
                .collect(toSet());


        Map<Tuple2<String, String>, List<Tuple2<String, String>>> candidateTypeChanges = Sets.intersection(typeChanges_testData, typeChanges_trainingData).stream()
                .filter(x -> !x._1()._1().equals("var") && !x._1()._1().equals("val") && !x._1()._2().equals("var")
                        && !x._1()._2().equals("val")
                        && x._1()._1().length()>1 && x._1()._2().length()>1)
                .collect(groupingBy(Tuple2::_1, collectingAndThen(toList(), ls -> ls.stream().map(x->x._2()).collect(toList()))));


        Set<ResolvedResponse> testDataFinal = testData.stream().filter(x -> analyzeResponse(x, candidateTypeChanges.keySet())).collect(toSet());
        Set<ResolvedResponse> trainingDataFinal = trainingData.stream().filter(x -> analyzeResponse(x, candidateTypeChanges.keySet())).collect(toSet());

        var typeChanges_testData_final = testDataFinal.stream().flatMap(x -> x.getResolvedTypeChanges().stream())
                .map(x->x._2())
                .collect(toSet());

        var typeChanges_trainingDatafinal = trainingDataFinal.stream()
                .flatMap(x -> x.getResolvedTypeChanges().stream())
                .filter(x-> x._1()._1().length()>1 && x._1()._2().length()>1)
                .filter(x -> !x._1()._1().equals("void") && !x._1()._2().equals("void"))
                .filter(x->!x._1()._1().equals("var") && !x._1()._1().equals("val") && !x._1()._2().equals("var") && !x._1()._2().equals("val"))
                .filter(x -> !x._1()._1().contains("?") && !x._1()._2().contains("?"))
                .map(Tuple2::_2)
                .collect(toSet());


        ImmutableSet<Tuple2<String, String>> final_intersection = Sets.intersection
                (typeChanges_testData_final, typeChanges_trainingDatafinal).immutableCopy();
        final_intersection.forEach(x -> System.out.println(x));
        System.out.println();
        
    }




    public static boolean analyzeResponse(ResolvedResponse rr, Set<Tuple2<String, String>> intersectingTCs){
//        Map<Tuple2<String, String>, Tuple2<String, String>> typeChange_template = rr.getResolvedTypeChanges().stream()
//                .collect(toMap(Tuple2::_1, Tuple2::_2));

        List<RMinerUtils.TypeChange> allRefactorings = rr.commits.stream().flatMap(x -> x.refactorings.stream()).filter(Objects::nonNull).collect(toList());
        if (allRefactorings.isEmpty()) {
            System.out.println("No Refactorings found!");
            return false;
        }

        return allRefactorings.stream()
                .filter(x -> x.getRefactoringKind().contains("TYPE"))
//                .filter(typeChange -> typeChange_template.containsKey(Tuple.of(typeChange.getB4Type(), typeChange.getAfterType())))
                .flatMap(typeChange -> {
                    if(!intersectingTCs.contains(Tuple.of(typeChange.getB4Type(), typeChange.getAfterType())))
                        return Stream.empty();
                    return getAsCodeMapping(typeChange)
                                .stream().filter(x -> !isNotWorthLearning(x));
                        })
                .findFirst().isPresent();

    }
    
    
}
