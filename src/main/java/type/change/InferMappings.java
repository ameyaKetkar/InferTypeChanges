package type.change;

import Utilities.ASTUtils;
import Utilities.CombyUtils;
import com.google.gson.Gson;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import type.change.comby.CombyRewrite;
import type.change.treeCompare.Explanation;
import type.change.treeCompare.IUpdate;
import type.change.treeCompare.NoUpdate;
import type.change.treeCompare.Update;
import type.change.visitors.NodeCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.*;


public class InferMappings {


    public static Stream<Update> getAllDescendants(Update u){
        return u.getSubUpdates().stream().flatMap(x -> Stream.concat(Stream.of(x), getAllDescendants(x)));

    }

//
//    public static Stream<Update> getAllDescendants(Update u){
//        return u.getSubUpdates().stream().flatMap(x -> Stream.concat(Stream.of(x), getAllDescendants(x)));
//
//    }



    public static List<Update> explainableUpdates(Update u){
        List<Update> allUpdates = Stream.concat(Stream.of(u), getAllDescendants(u))
                .filter(i -> i.getExplanation() instanceof Explanation)
                .collect(toList());
        if(allUpdates.size() == 1)
            return allUpdates;

        var removeRedundantUpdates = new ArrayList<Update>();
        for(var i : allUpdates){
            List<Update> allDescendants = getAllDescendants(i)
                    .filter(x -> x.getExplanation() instanceof Explanation).collect(toList());
            // Removes the descendant programs, where not all sub-updates of an update are explainable
            if(i.getSubUpdates().size() > 0 && allDescendants.size() < i.getSubUpdates().size()){
                removeRedundantUpdates.addAll(allDescendants);
                continue;
            }
            // Removes the ancestor programs, when all its sub-updates are explainable
            if(allUpdates.containsAll(allDescendants))
                removeRedundantUpdates.add(i);
        }

        return allUpdates.stream().filter(x -> !removeRedundantUpdates.contains(x)).collect(toList());
//                .filter(x -> x.getSubUpdates().size() ==0 ||  !x.getSubUpdates().stream().allMatch(y -> y.getExplanation() instanceof Explanation))
//                .collect(toList());
    }



    public static void main(String[] args){

        Path outputFolder = Path.of("/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output");
        Map<Tuple2<String, String >, String> fileNameTypechange = new HashMap<>(){{
//            put(Tuple.of("java.util.List", "java.util.Set"), "listToSet.jsonl");
            put(Tuple.of("java.io.File", "java.nio.file.Path"), "FileToPath.jsonl");
//            put(Tuple.of("java.util.concurrent.Callable", "java.util.function.Supplier"), "CallableToSupplier.jsonl");
//            put(Tuple.of("long", "java.util.concurrent.atomic.AtomicLong"), "longToAtomicLong.jsonl");
        }};



//      Short circuit for test
        var st1 = ASTUtils.getStatement("final FileResourceManager resourceManager=new FileResourceManager(tmpDir,10485760);");
        var st2 = ASTUtils.getStatement("final PathResourceManager resourceManager=new PathResourceManager(tmpDir,10485760);");
        IUpdate upd1 = ASTUtils.getUpdate(st1.get(), st2.get());
        List<Update> updates1 = explainableUpdates((Update) upd1);
        var popularTypeChanges = ASTUtils.getPopularTypeChanges(fileNameTypechange);


        for(var tc: popularTypeChanges) {
            Tuple2<String, String> typeChange = Tuple.of(pretty(tc.getKey()._1()), pretty(tc.getKey()._2()));
            System.out.println(pretty(tc.getKey()._1()) + " -> " + pretty(tc.getKey()._2()));
            System.out.println("==============");

            List<Update> updates = tc.getValue().stream()
                    .flatMap(instance -> instance.getCodeMappingList().stream().filter(x -> !ASTUtils.isNotWorthLearning(x))
                            .flatMap(cm -> inferTransformation(cm, instance.getNameB4(), instance.getNameAfter()).stream()))
                    .collect(toList());

            Map<Tuple2<String, String>, List<Update>> groupedByTemplates = updates.stream()
                    .collect(groupingBy(x -> ((Explanation) x.getExplanation()).getMatchReplace()));

            List<String> collect = groupedByTemplates.entrySet().stream().filter(x -> x.getValue().size() > 1)
                    .map(Tuple::fromEntry)
                    .map(x -> x.map2(y -> y.stream().map(z -> Tuple.of(z.getBeforeStr(), z.getAfterStr())).collect(toList())))
                    .map(InferredMappings::new)
                    .map(x -> new Gson().toJson(x))
                    .collect(toList());
            try {
                Files.write(outputFolder.resolve(fileNameTypechange.get(typeChange)), String.join("\n", collect).getBytes(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println();

        }
        try {
            Gson gson = new Gson();
            String str = gson.toJson(fileNameTypechange, Map.class);
            Files.write(outputFolder.resolve("PopularTypeChanges.json"), str.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
    }


    public static class InferredMappings{
        private final String Match;
        private final String Replace;
        private final List<Instance> Instances;

        public InferredMappings(Tuple2<Tuple2<String, String>, List<Tuple2<String, String>>> x){
            Match = x._1()._1();
            Replace = x._1()._2();
            Instances = x._2().stream().map(Instance::new).collect(toList());
        }


    }

    public static class Instance {
        private final String Before;

        public String getAfter() {
            return After;
        }

        private final String After;

        public Instance(Tuple2<String, String> x) {
            Before = x._1();
            After = x._2();
        }
    }

    private static List<Update> inferTransformation(CodeMapping cm, String nameB4, String nameAfter) {
        List<Update> explainableUpdates = new ArrayList<>();
        String stmtB4 = cm.getB4();
        String stmtAftr = cm.getAfter();
        // Perform rename variable for the TCI in the code mapping, to make it consistent
        if(!nameB4.equals(nameAfter)){
            Optional<CombyRewrite> cr = CombyUtils.rewrite(":[1~\\b" + nameAfter + "\\b]", nameB4, stmtAftr);
            if(cr.isPresent()){
                stmtAftr = cr.get().getRewrittenSource();
            }
        }

        var stmt_b = ASTUtils.getStatement(stmtB4.replace("\n",""));
        var stmt_a = ASTUtils.getStatement(stmtAftr.replace("\n",""));

        ASTUtils.printCodeMappingStuff(cm, nameB4, nameAfter, stmtB4, stmtAftr, stmt_b, stmt_a);

        // If the number of tokens are too large skip
        NodeCounter nc = new NodeCounter();
        stmt_b.get().accept(nc);
        if(nc.getCount() > 100){
            System.out.println("TOO LARGE!!!");
            return explainableUpdates;
        }

        IUpdate upd = ASTUtils.getUpdate(stmt_b.get(), stmt_a.get());
        if (upd instanceof NoUpdate){
            System.out.println("NO UPDATE FOUND!!!");
            return explainableUpdates;
        }

        explainableUpdates = explainableUpdates((Update) upd);
        if (explainableUpdates.isEmpty())
            System.out.println("NO EXPLAINABLE UPDATE FOUND!!!");

        for(var e: explainableUpdates){
            System.out.println(((Explanation)e.getExplanation()).getMatchReplace());
        }

        System.out.println("----------");

        return explainableUpdates;
    }


}
