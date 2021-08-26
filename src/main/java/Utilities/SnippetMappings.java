package Utilities;

import java.util.List;
import java.util.Set;

public class SnippetMappings {

    public List<SnippetMapping> sns;

    public static class SnippetMapping {
        public String tc_;
        public List<ChangesForCommit> commits;

    }
    public static class ChangesForCommit {
        public String commit;
        public List<BeforeAfter> b4Aftrs;
    }

    public static class BeforeAfter {
        private List<Mpng> mappings;

        public List<Mpng> getMappings(){
            if(mappings.size()>3){
                return mappings.subList(0,3);
            }
            return mappings;
        }
    }

    public static class Mpng {
        public String before;
        public String after;
        public String element;
    }
}
