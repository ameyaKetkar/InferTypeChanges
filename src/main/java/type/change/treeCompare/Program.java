package type.change.treeCompare;

public class Program {

    private String Match;
    private String Replace;

    public Program(String match, String replace) {
        Match = match;
        Replace = replace;
    }

    public String getMatch() {
        return Match;
    }

    public String getReplace() {
        return Replace;
    }
}
