package Utilities;

import com.google.gson.Gson;
import io.vavr.*;
import type.change.comby.CapturePatterns;
import type.change.comby.CombyMatch;
import type.change.comby.ExpressionPattern;
import type.change.treeCompare.Update;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.*;

public class RWUtils {
    public static Path outputFolder = Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output");
    public static CapturePatterns CAPTURE_PATTERNS = readPatterns(Paths.get("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/CapturePatternsNew.json"));




    public static Map<String, Class> kindMapping = new HashMap<>() {{
        put("InferredMappings", InferredMappings.class);
        put("CapturePatterns", CapturePatterns.class);
    }};

//    public static <T> T readJson(Path pathToJson, String kind) {
//        Gson gson = new Gson();
//        try (Reader reader = new FileReader(pathToJson.toAbsolutePath().toString())) {
//            Object result = gson.fromJson(reader, kindMapping.get(kind));
//            return (T) result;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public static <T> T toJson(String jsonData, String kind) {
        Gson gson = new Gson();
        Object result = gson.fromJson(jsonData, kindMapping.get(kind));
        return (T) result;
    }



    public static CapturePatterns readPatterns(Path pathToCapturePatterns) {
        CapturePatterns result;
        Gson gson = new Gson();
        try (Reader reader = new FileReader(pathToCapturePatterns.toAbsolutePath().toString())) {
            result = gson.fromJson(reader, CapturePatterns.class);
            assert result != null;
            if (result.getExpressionPatterns().stream().map(x -> x.getName()).count() < result.getExpressionPatterns().size())
                throw new RuntimeException("Each Capture pattern should have a distinct name!!!");

            String templates = result.getExpressionPatterns().stream()
                    .map(ExpressionPattern::getTemplate).collect(joining("\n"));

            Optional<CombyMatch> cm = CombyUtils.getMatch(":[:[var]]", templates, null);

            if (cm.isEmpty())
                throw new RuntimeException("not able to \"parse\" templates");

            Map<String, Long> tvs = cm.get().getMatches().stream()
                    .map(x -> x.getMatched())
                    .collect(groupingBy(x -> x, counting()));

            if (tvs.values().stream().anyMatch(x -> x > 1))
                throw new RuntimeException("Non distinct Template variables");
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static void writeAsJson(String fileNameTypechange, Map<Tuple2<String, String>, List<Update>> groupedByTemplates) {
        List<String> collect = groupedByTemplates.entrySet().stream()
                //midnight controller .filter(x -> x.getValue().size() > 1)
                .map(Tuple::fromEntry)
                .map(x -> x.map2(y -> y.stream().map(Update::getProject_commit_cu_los).collect(toList())))
                .map(x -> new InferredMappings(x._1()._1(), x._1()._2(), x._2()))
                .map(x -> new Gson().toJson(x))
                .collect(toList());
        try {
            Files.write(outputFolder.resolve(fileNameTypechange), String.join("\n", collect).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
