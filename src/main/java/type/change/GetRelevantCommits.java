package type.change;

import com.google.gson.Gson;
import com.t2r.common.models.refactorings.NameSpaceOuterClass;
import com.t2r.common.models.refactorings.ProjectOuterClass;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass;
import com.t2r.common.models.refactorings.TypeChangeCommitOuterClass;
import com.t2r.common.utilities.PrettyPrinter;
import com.t2r.common.utilities.ProtoUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.toList;

public class GetRelevantCommits {

    private static final Set<Tuple2<String, String>> queryTypeChanges = new HashSet<>() {{
        add(Tuple.of("java.io.File", "java.nio.file.Path"));
        add(Tuple.of("java.util.List", "java.util.Set"));
        add(Tuple.of("java.lang.String", "java.util.UUID"));
        add(Tuple.of("java.lang.String", "java.net.URI"));
        add(Tuple.of("java.lang.String", "java.util.regex.Pattern"));
        add(Tuple.of("java.lang.String", "java.utilSet<java.lang.String>"));
        add(Tuple.of("java.lang.String", "java.util.File"));
        add(Tuple.of("java.net.URL", "java.net.URI"));
        add(Tuple.of("java.net.URI", "java.net.URL"));
        add(Tuple.of("java.lang.String", "java.util.Optional<java.lang.String>"));
        add(Tuple.of("long", "java.time.Duration"));
        add(Tuple.of("long", "java.time.Instant"));
        add(Tuple.of("java.lang.Long", "java.time.Duration"));
        add(Tuple.of("java.util.Date", "java.lang.Long"));
        add(Tuple.of("java.util.Date", "java.time.LocalDate"));
        add(Tuple.of("java.util.List", "java.util.Set"));
        add(Tuple.of("java.util.Set", "com.google.common.collect.ImmutableSet")); // list
        add(Tuple.of("java.util.Map", "com.google.common.collect.ImmutableMap"));
        add(Tuple.of("java.util.Stack", "java.util.Deque")); //
        add(Tuple.of("java.util.concurrent.Callable", "java.util.function.Supplier"));
        add(Tuple.of("java.util.function.Function", "java.util.function.ToDoubleFunction"));
        add(Tuple.of("java.util.function.Function", "java.util.function.ToIntFunction"));
        add(Tuple.of("java.util.Optional", "java.util.OptionalInt"));
        add(Tuple.of("long", "java.util.concurrent.atomic.AtomicLong")); // int boolean
        add(Tuple.of("java.util.Map", "java.util.concurrent.ConcurrentHashMap"));
        add(Tuple.of("java.util.concurrent.BlockingQueue", "java.util.Queue"));
        add(Tuple.of("org.apache.hadoop.hbase.KeyValue", "org.apache.hadoop.hbase.Cell"));
        add(Tuple.of("java.lang.String", "java.util.UUID"));

    }};

    public static class Input{
        public List<RelevantCommits> inputTypeChangeCommits;
        public Input(Map<Tuple2<String, String>, Set<Tuple3<String, String, String>>> input_tcs){
            inputTypeChangeCommits = input_tcs.entrySet().stream()
                    .map(x -> new RelevantCommits(x.getKey(), x.getValue())).collect(toList());
        }
    }

    public static class RelevantCommits {
        public String typeB4;
        public List<Tuple3<String, String, String>> projectUrlSHA;
        public String typeAfter;
        public RelevantCommits(Tuple2<String, String> typeChange ,Set<Tuple3<String, String, String>> commits){
            typeB4 = typeChange._1();
            typeAfter = typeChange._2();
            projectUrlSHA = new ArrayList<>(commits);
        }
    }

    public static void main(String[] args) throws IOException {

        //String pathToTypeChangeMiner = "/Users/ameya/Research/TypeChangeStudy/TypeChangeMiner/";
        Path pathToTypeChangeMiner = Path.of(args[0]);
        String pathToTypeChangeMinerInput = pathToTypeChangeMiner.resolve("Input/ProtosOut").toAbsolutePath().toString();
        String pathToTypeChangeMinerOutput = pathToTypeChangeMiner.resolve("Output").toAbsolutePath().toString();
        Input relevantCommits = new Input(getCommitsFor(queryTypeChanges, pathToTypeChangeMinerInput, pathToTypeChangeMinerOutput));

        Files.write(Path.of("Input.json"), new Gson().toJson(relevantCommits, Input.class).getBytes(), StandardOpenOption.CREATE);

    }

    public static Map<Tuple2<String, String>, Set<Tuple3<String, String, String>>> getCommitsFor(Set<Tuple2<String, String>> queryTypeChanges, String pathToTypeChangeMinerInput, String pathToTypeChangeMinerOutput){
        ProtoUtil.ReadWriteAt rw_input = new ProtoUtil.ReadWriteAt(Path.of(pathToTypeChangeMinerInput));
        ProtoUtil.ReadWriteAt rw_output = new ProtoUtil.ReadWriteAt(Path.of(pathToTypeChangeMinerOutput));
        ArrayList<ProjectOuterClass.Project> projects = new ArrayList<>(rw_input.readAll("projects", "Project"));

        List<Tuple2<ProjectOuterClass.Project, List<TypeChangeCommitOuterClass.TypeChangeCommit>>> project_tcc = projects.stream()
                .map(z -> Tuple.of(z, rw_output.<TypeChangeCommitOuterClass.TypeChangeCommit>readAll("TypeChangeCommit_" + z.getName(), "TypeChangeCommit")))
                .collect(toList());

        Map<Tuple2<String, String>, Set<Tuple3<String, String, String>>> relevantCommits = new HashMap<>();

        for(var p : project_tcc){
            if(p._2().size() <= 1) continue;
            for (TypeChangeCommitOuterClass.TypeChangeCommit x : p._2()){
                for(TypeChangeAnalysisOuterClass.TypeChangeAnalysis anlys: x.getTypeChangesList()){
                    if(isNoisy(anlys)){
                        Tuple2<String, String> typeChange = Tuple.of(anlys.getB4(), anlys.getAftr()).map(PrettyPrinter::pretty, PrettyPrinter::pretty);
                        if(queryTypeChanges.contains(typeChange)) {
                            if(!relevantCommits.containsKey(typeChange))
                                relevantCommits.put(typeChange, new HashSet<>());
                            relevantCommits.get(typeChange).add(Tuple.of(p._1().getName(), p._1().getUrl(), x.getSha()));
                        }
                    }
                }
            }
        }


        return relevantCommits;
    }

    public static boolean isNoisy(TypeChangeAnalysisOuterClass.TypeChangeAnalysis anlys) {
        return !anlys.getB4().getRoot().getIsTypeVariable() && !anlys.getAftr().getRoot().getIsTypeVariable()
                && !anlys.getNameSpacesB4().equals(NameSpaceOuterClass.NameSpace.Internal) && !anlys.getNameSpaceAfter().equals(NameSpaceOuterClass.NameSpace.Internal);
    }

//    public static List<Map.Entry<Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph>, List<TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance>>> getPopularTypeChanges(Map<Tuple2<String, String>, String> fileNameTypechange){
//        ProtoUtil.ReadWriteAt rw_input = new ProtoUtil.ReadWriteAt(Path.of("/Users/ameya/Research/TypeChangeStudy/TypeChangeMiner/Input/ProtosOut/"));
//        ProtoUtil.ReadWriteAt rw_output = new ProtoUtil.ReadWriteAt(Path.of("/Users/ameya/Research/TypeChangeStudy/TypeChangeMiner/Output/"));
//        ArrayList<ProjectOuterClass.Project> projects = new ArrayList<>(rw_input.readAll("projects", "Project"));
//
//        List<Tuple2<ProjectOuterClass.Project, List<TypeChangeCommitOuterClass.TypeChangeCommit>>> project_tcc = projects.stream()
//                .map(z -> Tuple.of(z, rw_output.<TypeChangeCommitOuterClass.TypeChangeCommit>readAll("TypeChangeCommit_" + z.getName(), "TypeChangeCommit")))
//                .collect(toList());
//
//
//        Map<Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph>, List<TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance>> instancesGroupedByTypeChange = new HashMap<>();
//        Map<Tuple2<String, String>, List<String>> commitsGroupedByTypeChange = new HashMap<>();
//        Map<Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph>, List<String>> projectsGroupedByTypeChange = new HashMap<>();
//
//        List<TypeChangeAnalysisOuterClass.TypeChangeAnalysis> typeChangeAnalyses = new ArrayList<>();
//        for(var p : project_tcc){
//            if(p._2().size()<=1) continue;
//            for (TypeChangeCommitOuterClass.TypeChangeCommit x : p._2()){
//
//
//                for(TypeChangeAnalysisOuterClass.TypeChangeAnalysis anlys: x.getTypeChangesList()){
//                    if(isNoisy(anlys)){
//                        Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph> typeChange = Tuple.of(anlys.getB4(), anlys.getAftr());
//
//                        instancesGroupedByTypeChange.merge(typeChange,
//                                anlys.getTypeChangeInstancesList().stream().filter(z -> z.getCodeMappingList().stream().anyMatch(g -> !isNotWorthLearning(g))).collect(toList()), ASTUtils::mergeList);
//                        commitsGroupedByTypeChange.merge(typeChange.map(PrettyPrinter::pretty, PrettyPrinter::pretty), List.of(x.getSha()), ASTUtils::mergeList);
//                        projectsGroupedByTypeChange.merge(typeChange, List.of(p._1.getName()), ASTUtils::mergeList);
//                    }
//                }
//            }
//        }
//
//
//
//
//        List<Map.Entry<Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph>, List<TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance>>> groupedTci = instancesGroupedByTypeChange
//                .entrySet().stream()
//                .filter(x -> x.getValue().size() > 0)
//                .sorted(Comparator.comparingInt((Map.Entry<Tuple2<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph>, List<TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance>> x) -> projectsGroupedByTypeChange.get(x.getKey()).size())
//                        .thenComparingInt(x -> commitsGroupedByTypeChange.get(x.getKey()).size()))
//                .collect(toList());
//
//        Collections.reverse(groupedTci);
//
////        for(var e : groupedTci){
////            System.out.println(e.getKey().map(PrettyPrinter::pretty, PrettyPrinter::pretty) + " " + projectsGroupedByTypeChange.get(e.getKey()).size());
////
////        }
//        // https://github.com/undertow-io/undertow/commit/4428e96024ae631b8c299dc7fcda657b35e7e7a8?diff=split#diff-85de71c3800f1cbd223b8cb766dfb9f8L101
//
//        System.out.println(groupedTci.size());
//
//        return groupedTci.stream()
//                .filter(x -> fileNameTypechange.keySet().stream().anyMatch(v -> v._1().equals(pretty(x.getKey()._1()))
//                        && v._2().equals(pretty(x.getKey()._2()))))
//                .collect(toList());
//    }


}
