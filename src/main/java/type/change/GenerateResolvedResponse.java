package type.change;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.refactoringminer.RMinerUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static Utilities.ResolveTypeUtil.getResolvedTypeChangeTemplate;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class GenerateResolvedResponse {
    public static Path pathToAllCommits = Paths.get("/Users/ameya/Research/TypeChangeStudy/HttpServer/RMinerAllCommits");
    public static Path pathToResolvedCommits = Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/ResolvedResponses");

    public static void main(String[] a) throws IOException {
        Set<String> alreadyResolved = Files.list(pathToResolvedCommits).map(x -> x.getFileName().toString().replace(".json", "")).collect(Collectors.toSet());
        Files.walk(pathToAllCommits).parallel()
                .map(x -> Try.of(() -> Files.readString(x)).map(s -> new Gson().fromJson(s, ResolvedResponse.class))
                        .getOrElse(() -> {
                            System.out.println("Could not parse the file " + x);
                            return null;
                        }))
                .filter(Objects::nonNull)
                .filter(x -> x.commits != null && !x.commits.isEmpty() && x.commits.stream().anyMatch(c -> c.refactorings != null
                        && !c.refactorings.isEmpty()
                        && c.refactorings.stream().anyMatch(r -> r.getRefactoringKind().contains("TYPE"))))
                .filter(x -> !alreadyResolved.contains(x.commits.get(0).sha1))
                .peek(response -> {
                    var resolvedTypeChangeTemplate = response.commits.stream().flatMap(x -> x.refactorings.stream())
                            .filter(x -> x.getB4Type() != null)
                            .collect(groupingBy(r -> Tuple.of(r.getB4Type(), r.getAfterType())))
                            .entrySet().stream()
                            .flatMap(x -> getResolvedTypeChangeTemplate(x.getKey(), x.getValue()).stream().map(t -> Tuple.of(x.getKey(), t)))
                            .collect(Collectors.toList());
                    response.setResolvedTypeChanges(resolvedTypeChangeTemplate);
                })
                .forEach(x -> {
                    Path fileName = pathToResolvedCommits.resolve(x.commits.get(0).sha1 + ".json");
                    try {
                        Files.write(fileName, new Gson().toJson(x, ResolvedResponse.class).getBytes(), StandardOpenOption.CREATE_NEW);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
    }


    public static class ResolvedResponse{

        private List<Tuple2<Tuple2<String, String>, Tuple2<String, String>>> resolvedTypeChanges;

        public List<RMinerUtils.CommitData> commits;

        public List<Tuple2<Tuple2<String, String>, Tuple2<String, String>>> getResolvedTypeChanges() {
            return resolvedTypeChanges;
        }
        public void setResolvedTypeChanges(List<Tuple2<Tuple2<String, String>, Tuple2<String, String>>> resolvedTypeChanges) {
            this.resolvedTypeChanges = resolvedTypeChanges;
        }
    }



}
