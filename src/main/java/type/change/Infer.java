package type.change;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import Utilities.InferredMappings;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import logging.MyLogger;
import type.change.treeCompare.*;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static Utilities.ASTUtils.*;
import static Utilities.RWUtils.*;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.*;


public class Infer {


    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static Map<Tuple2<String, String>, String> fileNameTypechange = new HashMap<>(){{

//        put(Tuple.of("java.io.File", "java.nio.file.Path"), "FileToPath.jsonl");
//        put(Tuple.of("java.lang.String", "java.util.UUID"), "StringToUUID.jsonl");
//        put(Tuple.of("java.lang.String", "java.net.URI"), "StringToURI.jsonl");
//        put(Tuple.of("java.lang.String", "java.util.regex.Pattern"), "StringToPattern.jsonl");
//        put(Tuple.of("java.lang.String", "java.utilSet<java.lang.String>"), "StringToSet.jsonl");
//        put(Tuple.of("java.lang.String", "java.util.File"), "StringToFile.jsonl");
//        put(Tuple.of("java.net.URL", "java.net.URI"), "URLToURI.jsonl");
//        put(Tuple.of("java.net.URI", "java.net.URL"), "URIToURL.jsonl");
//
//        put(Tuple.of("java.lang.String", "java.util.Optional<java.lang.String>"), "StringToOptional.jsonl");
//
//
//        put(Tuple.of("long", "java.time.Duration"), "longToDuration.jsonl");
//        put(Tuple.of("long", "java.time.Instant"), "longToInstant.jsonl");
//        put(Tuple.of("java.lang.Long", "java.time.Duration"), "longToDuration.jsonl");
//        put(Tuple.of("java.util.Date", "java.lang.Long"), "DateToLong.jsonl");
//        put(Tuple.of("java.util.Date", "java.time.LocalDate"), "DateToLocalDate.jsonl");
//
//
        put(Tuple.of("java.util.List", "java.util.Set"), "listToSet.jsonl");
//        put(Tuple.of("java.util.Set", "com.google.common.collect.ImmutableSet"), "SetToImmutable.jsonl"); // list
//        put(Tuple.of("java.util.Map", "com.google.common.collect.ImmutableMap"), "MapToImmutable.jsonl");
//        put(Tuple.of("java.util.Stack", "java.util.Deque"), "StackToDeque.jsonl"); //
//
//        put(Tuple.of("java.util.concurrent.Callable", "java.util.function.Supplier"), "CallableToSupplier.jsonl");
//        put(Tuple.of("java.util.function.Function", "java.util.function.ToDoubleFunction"), "FunctonToToDouble.jsonl");
//        put(Tuple.of("java.util.function.Function", "java.util.function.ToIntFunction"), "FunctonToToInt.jsonl");
//        put(Tuple.of("java.util.Optional", "java.util.OptionalInt"), "OptionalToOptionalInt.jsonl");
//
//
//        put(Tuple.of("long", "java.util.concurrent.atomic.AtomicLong"), "longToAtomicLong.jsonl"); // int boolean
//        put(Tuple.of("java.util.Map", "java.util.concurrent.ConcurrentHashMap"), "MapToConcurrent.jsonl");
//        put(Tuple.of("java.util.concurrent.BlockingQueue", "java.util.Queue"), "BlockingQueToQueue.jsonl");



//        put(Tuple.of("org.apache.hadoop.hbase.KeyValue", "org.apache.hadoop.hbase.Cell"), "keyValueToCell.jsonl");

//        put(Tuple.of("java.lang.String", "java.util.UUID"), "StringToUUID.jsonl");

    }};


    public static void main(String[] args) throws IOException {
        MyLogger.setup();
//      Short circuit for test
        var st1 = ASTUtils.getStatement("DefaultServer.setRootHandler(new CanonicalPathHandler().setNext(new PathHandler().addPrefixPath(\"/path\",new ResourceHandler().setResourceManager(new FileResourceManager(newSymlink,10485760,true,rootPath.getAbsolutePath().concat(\"/innerSymlink\"))).setDirectoryListingEnabled(false).addWelcomeFiles(\"page.html\"))));");
        var st2 = ASTUtils.getStatement("DefaultServer.setRootHandler(new CanonicalPathHandler().setNext(new PathHandler().addPrefixPath(\"/path\",new ResourceHandler(new PathResourceManager(newSymlink,10485760,true,rootPath.toAbsolutePath().toString().concat(\"/innerSymlink\"))).setDirectoryListingEnabled(false).addWelcomeFiles(\"page.html\"))));");
        GetIUpdate gu = new GetIUpdate();

        IUpdate upd1 = gu.getUpdate(st1.get(), st2.get());
        List<Update> updates1 = explainableUpdates((Update) upd1);

         var popularTypeChanges = ASTUtils.getPopularTypeChanges(fileNameTypechange);

        for(var tc: popularTypeChanges) {
            Tuple2<String, String> typeChange = Tuple.of(pretty(tc.getKey()._1()), pretty(tc.getKey()._2()));
            System.out.println(pretty(tc.getKey()._1()) + " -> " + pretty(tc.getKey()._2()));
            System.out.println("==============");
            List<Update> updates = tc.getValue().stream()
                    .flatMap(instance -> instance.getCodeMappingList().stream().filter(x -> !ASTUtils.isNotWorthLearning(x))
                            .flatMap(cm -> inferTransformation(cm, instance).stream()))
                    .collect(toList());
            Map<Tuple2<String, String>, List<Update>> groupedByTemplates = updates.stream()
                    .collect(groupingBy(x -> ((Explanation) x.getExplanation()).getMatchReplace()));
            writeAsJson(fileNameTypechange.get(typeChange), groupedByTemplates);
            System.out.println();
        }
        try {
            Files.write(outputFolder.resolve("PopularTypeChanges.json"),
                    new Gson().toJson(fileNameTypechange, Map.class).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }


    private static List<Update> inferTransformation(CodeMapping cm, TypeChangeInstance tci) {

        String nameB4 = tci.getNameB4(), nameAfter = tci.getNameAfter();

        List<Update> explainableUpdates = new ArrayList<>();
        String stmtB4 = cm.getB4();
        String stmtAftr = CombyUtils.performIdentifierRename(nameB4, nameAfter, cm.getAfter());

        var stmt_b = ASTUtils.getStatement(stmtB4.replace("\n",""));
        var stmt_a = ASTUtils.getStatement(stmtAftr.replace("\n",""));

        ASTUtils.printCodeMappingStuff(cm, nameB4, nameAfter, stmtB4, stmtAftr, stmt_b, stmt_a);

        // If the number of tokens are too large skip
        NodeCounter nc = new NodeCounter();
        stmt_b.get().accept(nc);
        if(nc.getCount() > 50){
            LOGGER.info("TOO LARGE!!!");
            return explainableUpdates;
        }

        GetIUpdate gu = new GetIUpdate();
        IUpdate upd = gu.getUpdate(stmt_b.get(), stmt_a.get());
        if (upd instanceof NoUpdate){
            LOGGER.info("NO UPDATE FOUND!!!");
            return explainableUpdates;
        }

        explainableUpdates = explainableUpdates((Update) upd);

        if (explainableUpdates.isEmpty())
            LOGGER.info("NO EXPLAINABLE UPDATE FOUND!!!");

        for(var e: explainableUpdates){
            e.setProject_commit_cu_los(new InferredMappings.Instance(cm.getB4(), cm.getAfter(), e.getBeforeStr(), e.getAfterStr(), extractProject(cm.getUrlbB4()), extractCommit(cm.getUrlbB4())
                    ,tci.getCompilationUnit(), Tuple.of(extractLineNumber(cm.getUrlbB4()), extractLineNumber(cm.getUrlAftr()))
            , Tuple.of(nameB4, nameAfter)));


            LOGGER.info((((Explanation)e.getExplanation()).getMatchReplace()).toString());
        }

        System.out.println("----------");

        return explainableUpdates;
    }


    public static List<Update> explainableUpdates(Update u){
        // Collect all updates!
        Collection<Update> allUpdates = Stream.concat(Stream.of(u), Update.getAllDescendants(u))
                .filter(i -> i.getExplanation() instanceof Explanation)
                .collect(toList());

        allUpdates = allUpdates.stream().collect(groupingBy(x -> Tuple.of(Tuple.of(x.getBefore().getPos(), x.getBefore().getEndPos()), Tuple.of(
                x.getAfter().getPos(), x.getAfter().getEndPos())), collectingAndThen(toList(), x -> x.stream().findFirst().get())))
                .values();

        if(allUpdates.size() == 1)
            return new ArrayList<>(allUpdates);

        // IF Descendants explain the change partially then remove descendants
        // IF Descendants explain the change incorrectly then remove descendants
        // IF descendants completely explain the change, then remove the ancestors

        var removeRedundantUpdates = new ArrayList<Update>();
        for(var i : allUpdates){
            List<Update> allDescendants = Update.getAllDescendants(i)
                    .filter(x -> x.getExplanation() instanceof Explanation)
                    .filter(allUpdates::contains)
                    .collect(toList());

            List<Update> simplestDescendants = removeSubsumedEdits(allDescendants);
            boolean applyDesc = Update.applyUpdatesAndMatch(simplestDescendants, i.getBeforeStr(), i.getAfterStr());
            if(applyDesc)
                removeRedundantUpdates.add(i);
            else removeRedundantUpdates.addAll(simplestDescendants);

        }

        allUpdates = allUpdates.stream().filter(x -> !removeRedundantUpdates.contains(x)).collect(toList());

        if(allUpdates.size() == 1)
            return new ArrayList<>(allUpdates);

        // Removed subsumed edits .... keep the simplest possible explanation
        return removeSubsumedEdits(allUpdates);

//                .filter(x -> x.getSubUpdates().size() ==0 ||  !x.getSubUpdates().stream().allMatch(y -> y.getExplanation() instanceof Explanation))
//                .collect(toList());
    }

    public static List<Update> removeSubsumedEdits(Collection<Update> allUpdates) {
        List<Update> subsumedEdits = new ArrayList<>();
        for(var i : allUpdates){
            for (var j : allUpdates){
                if(isSubsumedBy(i.getBefore(), j.getBefore()) || isSubsumedBy(i.getAfter(), j.getAfter()) ){
                    subsumedEdits.add(j);
                }
            }
        }
        return allUpdates.stream().filter(x -> !subsumedEdits.contains(x)).collect(toList());
    }
}
