package type.change;

import logging.MyLogger;

import java.io.IOException;

import static org.refactoringminer.RMinerUtils.toStmtMapping;


public class Infer {

    public static void main(String[] args) throws IOException {
        MyLogger.setup();
        if ("-c".equals(args[0])) {
            CommitMode.commitMode(args);
        }
        if ("-s".equals(args[0])) {
            SnippetMode.snippetMode(args);
        }
    }


}
