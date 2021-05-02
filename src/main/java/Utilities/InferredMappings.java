package Utilities;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import type.change.comby.CombyMatch;
import type.change.comby.Environment;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class InferredMappings {

    private final String Match;
    private final String Replace;
    private final List<Instance> Instances;
    private String capturesUsage;
    private boolean noTv;

    public InferredMappings(String match, String replace, List<Instance> instances) {
        Match = match;
        Replace = replace;
        Instances = instances;
        noTv = hasNoTV();
    }

    public boolean hasNoTV() {
        return CombyUtils.getMatch(":[:[var]]", Match, null).isEmpty();
    }

    public String getMatch() {
        return Match;
    }

    public String getMatch(String name) {
        if (name == null) return Match;
        if (capturesUsage != null)
            return Match.replace(capturesUsage, capturesUsage + "~\\b" + name + "\\b");
        return Match;
    }

    public String getReplace() {
        return Replace;
    }

    public List<Instance> getInstances() {
        return Instances;
    }

    public void isUsageMapping() {
        List<Tuple2<Instance, String>> xs = getInstances().stream().flatMap(x -> isInstanceAnUsage(x).map(u -> Tuple.of(x, u)).stream())
                .collect(toList());
        if ((double) xs.size() / getInstances().size() >= 0.5) {
            capturesUsage = (xs.get(0)._2());
        }
    }

    public Optional<String> isInstanceAnUsage(Instance i) {
        String matcher = getMatch().replace("\\\"", "\"");
        if (matcher.equals(i.getBefore())) return Optional.empty();
        CombyMatch cm_m = CombyUtils.getMatch(matcher, i.getBefore(), null).orElseThrow(() -> new RuntimeException(i.getBefore() + "     " + matcher));
        Optional<Environment> tv_b4 = cm_m.getMatches().get(0).getEnvironment().stream().filter(e -> e.getValue().equals(i.getNames()._1()))
                .findFirst();
        if (tv_b4.isPresent()) {
            CombyMatch cm_r = CombyUtils.getMatch(getReplace(), i.getAfter(), null).get();
            Optional<Environment> tv_After = cm_r.getMatches().get(0).getEnvironment().stream().filter(e -> e.getValue().equals(i.getNames()._1())
                    || e.getValue().equals(i.getNames()._2())).findFirst();
            if (tv_After.isPresent() && tv_b4.get().getVariable().equals(tv_After.get().getVariable())) {
                return Optional.ofNullable(tv_b4.get().getVariable());
            }
        }
        return Optional.empty();
    }


    public String getCapturesUsage() {
        return capturesUsage;
    }

    public boolean isNoTv() {
        return noTv;
    }


    public static class Instance {
        private final String OriginalCompleteBefore;
        private final String OriginalCompleteAfter;
        private final String Before;
        private final String After;
        private final String Project;
        private final String Commit;
        private final String CompilationUnit;
        private final Tuple2<String, String> LineNos;
        private final Tuple2<String, String> Names;

        public Instance(String originalCompleteBefore, String originalCompleteAfter, String before, String after, String project, String commit, String cu, Tuple2<String, String> lineNos, Tuple2<String, String> names){
            OriginalCompleteBefore = originalCompleteBefore;
            OriginalCompleteAfter = originalCompleteAfter;
            Before = before;
            After = after;
            Project = project;
            Commit = commit;
            CompilationUnit = cu;
            LineNos = lineNos;
            Names = names;
        }

        public String getBefore() {
            return Before;
        }

        public String getProject() {
            return Project;
        }

        public String getCommit() {
            return Commit;
        }

        public String getAfter() {
            return After;
        }

        public Tuple2<String, String> getLineNos() {
            return LineNos;
        }

        public String getCompilationUnit() {
            return CompilationUnit;
        }

        public Tuple2<String, String> getNames() {
            return Names;
        }

        public String getOriginalCompleteBefore() {
            return OriginalCompleteBefore;
        }

        public String getOriginalCompleteAfter() {
            return OriginalCompleteAfter;
        }
    }
}
