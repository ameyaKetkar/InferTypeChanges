package type.change;


import io.vavr.control.Try;
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.lib.Repository;
//import org.refactoringminer.api.GitHistoryRefactoringMiner;
//import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static com.t2r.common.utilities.FileUtils.materializeAtBase;
import static com.t2r.common.utilities.GitUtil.*;

public class Learner {

    public static Properties prop;
    public static Function<String, Path> projectPath;
    public static Path pathToCorpus;
    public static Path pathToTemp;
    public static String mavenHome;


    static {
        try {
            prop = new Properties();
            InputStream input = new FileInputStream("paths.properties");
            prop.load(input);
            pathToCorpus = Path.of(prop.getProperty("PathToCorpus"));
            pathToTemp = Path.of(prop.getProperty("PathToTemp"));
            projectPath = p -> pathToCorpus.resolve("Project_" + p).resolve(p);
            mavenHome = prop.getProperty("mavenHome");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] a) throws Exception {
//        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
//        for (var e : Files.readAllLines(Path.of("Corpus.csv"))) {
//            System.out.println(e);
//            var projectName = e.split(",")[0].trim();
//            var commitID = e.split(",")[1].trim();
//
////            if (!commitID.equals("19cf690b5a27798774aaf33c4c2af0d9072873fc"))
////                continue;
//
//            Try<Git> g = tryToClone("", projectPath.apply(projectName));
//
//            if(g.isFailure()) continue;
//
//            Repository repo = g.get().getRepository();
//            var commit = findCommit(commitID, repo);
//
//            if(commit.isEmpty()) continue;
//
//            //var jarArtifactInfoSet = getDependenciesFromEffectivePom(commitID, projectName, repo, mavenHome);
//
//            var files = getFilesAddedRemovedRenamedModified(repo,
//                    commit.get(),filePathDiffAtCommit(g.get(), commitID));

//            Path pAfter = pathToTemp.resolve(commitID);
//            materializeAtBase(pAfter, files._4());
//            materializeAtBase(pAfter, files._3());
//
//            Path pBefore = pathToTemp.resolve(commitID + "-Parent");
//            materializeAtBase(pBefore, files._2());
//            materializeAtBase(pBefore, files._1());
//            var typeChangeMiner = new TypeChangeMiner(pAfter, pBefore, jarArtifactInfoSet);
//            miner.detectAtCommit(repo, commitID, typeChangeMiner);
//        }
    }
}
