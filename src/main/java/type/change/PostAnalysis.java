package type.change;

import Utilities.RWUtils;
import Utilities.InferredMappings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PostAnalysis {

    public static void main(String[] a) throws IOException {
        List<InferredMappings> inferredMappings = Files.readAllLines(Path.of(".").toAbsolutePath().resolve("Output/FileToPath.jsonl"))
                .stream().map(x -> RWUtils.<InferredMappings >toJson(x, "InferredMappings")).collect(Collectors.toList());


        System.out.println();


    }

}
