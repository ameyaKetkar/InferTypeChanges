package type.change;

import com.google.gson.Gson;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.refactoringminer.RMinerUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
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
        Path fileName_noTC = pathToResolvedCommits.getParent().resolve("NoTypeChanges.txt");
        Set<String> noTypeChanges = new HashSet<>(Files.readAllLines(fileName_noTC));
//        List<String> noTypeChanges = (Files.readAllLines(fileName_noTC));
        Set<String> alreadyResolved = Files.list(pathToResolvedCommits)
                .map(x -> x.getFileName().toString().replace(".json", "")).collect(Collectors.toSet());
        Files.walk(pathToAllCommits)
                .filter(x -> !noTypeChanges.contains(x.getFileName().toString().replace(".json", "")))
                .parallel()
                .map(x -> Try.of(() -> {
                    ResolvedResponse gson = new Gson().fromJson(Files.readString(x), ResolvedResponse.class);
                    if (gson.commits != null && !gson.commits.isEmpty()) {
                        writeToNoTypeChangeFile(fileName_noTC, x.getFileName().toString().replace(".json", ""));
                    }
                    return gson; })
                        .getOrElse(() -> {
                            System.out.println("Could not parse the file " + x);
                            writeToNoTypeChangeFile(fileName_noTC, x.getFileName().toString().replace(".json", ""));
                            return null;
                        }))
                .filter(Objects::nonNull)
                //4428e96024ae631b8c299dc7fcda657b35e7e7a8
                .filter(x -> x.commits != null && !x.commits.isEmpty())
                .peek(x -> {
                    if (x.commits.stream().noneMatch(c -> c.refactorings != null
                            && !c.refactorings.isEmpty()
                            && c.refactorings.stream().anyMatch(r -> r.getRefactoringKind().contains("TYPE")))) {
                        writeToNoTypeChangeFile(fileName_noTC, x.commits.get(0).sha1);
                    }
                })
                .filter(x -> x.commits != null && !x.commits.isEmpty() && x.commits.stream().anyMatch(c -> c.refactorings != null
                        && !c.refactorings.isEmpty()
                        && c.refactorings.stream().anyMatch(r -> r.getRefactoringKind().contains("TYPE"))))
                .filter(x -> !alreadyResolved.contains(x.commits.get(0).sha1))
//                .filter(x -> x.commits.get(0).sha1.equals("793b15c1422b10f196ab3344e71d778439f5e8f6"))
                .peek(response -> {
                    if (response.commits.stream().flatMap(x -> x.refactorings.stream())
                            .allMatch(x -> x.getB4Type() == null)) {
                        writeToNoTypeChangeFile(fileName_noTC, response.commits.get(0).sha1);
                    }
                    var resolvedTypeChangeTemplate = response.commits.stream().flatMap(x -> x.refactorings.stream())
                            .filter(x -> x.getB4Type() != null)
//                            .filter(x -> x.getB4Type().equals("MapPartitionOperator<WindowedValue<InputT>,WindowedValue<RawUnionValue>>") && x.getAfterType().equals("DataSet<WindowedValue<RawUnionValue>>"))
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void writeToNoTypeChangeFile(Path fileName_noTC, String s) {
        try {
            Files.write(fileName_noTC, ("\n" + s).getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class ResolvedResponse {

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
