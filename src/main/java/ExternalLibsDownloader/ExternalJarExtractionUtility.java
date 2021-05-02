package ExternalLibsDownloader;

import com.t2r.common.utilities.FileUtils;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static com.t2r.common.utilities.FileUtils.*;
import static com.t2r.common.utilities.GitUtil.populateFileContents;
import static com.t2r.common.utilities.GitUtil.tryToClone;
import static java.util.stream.Collectors.toSet;
import static type.change.Learner.pathToCorpus;

/**
 * @author Diptopol
 * @since 12/27/2020 5:17 PM
 */
public class ExternalJarExtractionUtility {

    private static Logger logger = LoggerFactory.getLogger(ExternalJarExtractionUtility.class);

    public static Set<Tuple3<String, String, String>> getDependenciesFromEffectivePom(String commit,
                                                                                      String projectName,
                                                                                      String cloneLink,
                                                                                      String mavenHome,
                                                                                      Path pathToCorpus) {

        Set<String> deps = generateEffectivePom(commit, projectName, cloneLink, mavenHome, pathToCorpus)
                .map(Utility::listOfJavaProjectLibraryFromEffectivePom)
                .orElse(new HashSet<>());

        return deps.stream().map(x -> x.split(":"))
                .filter(x -> x.length == 3)
                .map(dep -> Tuple.of(dep[0], dep[1], dep[2]))
                .collect(toSet());
    }

    public static Set<Tuple3<String, String, String>> getDependenciesFromEffectivePom(String commit,
                                                                                      String projectName,
                                                                                      Repository repository,
                                                                                      String mavenHome) {
        Set<String> deps = generateEffectivePOM(commit, projectName, repository, mavenHome)
                .map(Utility::listOfJavaProjectLibraryFromEffectivePom)
                .orElse(new HashSet<>());

        return deps.stream().map(x -> x.split(":"))
                .filter(x -> x.length == 3)
                .map(dep -> Tuple.of(dep[0], dep[1], dep[2]))
                .collect(toSet());
    }

    private static Path pathToProjectFolder(String projectName, Path pathToCorpus) {
        return pathToCorpus.resolve("Project_" + projectName);
    }

    private static Optional<String> generateEffectivePom(String commitID, final String projectName,
                                                         String cloneLink, String mavenHome, Path pathToCorpus) {
        Path pathToProject = pathToProjectFolder(projectName, pathToCorpus);

        Try<Git> repo = tryToClone(cloneLink, pathToProject.resolve(projectName));

        if (repo.isFailure()) return Optional.empty();
        return generateEffectivePOM(commitID, projectName, repo.get().getRepository(), mavenHome);
    }

    private static Optional<String> generateEffectivePOM(String commitID, String projectName, Repository repo, String mavenHome) {
        Path pathToProject = pathToProjectFolder(projectName, pathToCorpus);

        createFolderIfAbsent(pathToProject);

        Map<Path, String> poms = populateFileContents(repo, commitID, x -> x.endsWith("pom.xml"));
        Path p = pathToProject.resolve("tmp").resolve(commitID);
        FileUtils.materializeAtBase(p, poms);
        Path effectivePomPath = p.resolve("effectivePom.xml");

        if (!effectivePomPath.toFile().exists()) {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(p.resolve("pom.xml").toAbsolutePath().toString()));
            request.setGoals(Arrays.asList("help:effective-pom", "-Doutput=" + effectivePomPath.toAbsolutePath().toString()));
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(mavenHome));
            try {
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    logger.info("Build Failed");
                    logger.info("Could not generate effective pom");
                    return Optional.empty();
                }
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        String effectivePomPathContent = readFile(effectivePomPath);
        deleteDirectory(p);

        return Optional.of(effectivePomPathContent);
    }

    public static JarInformation getJarInfo(String groupId, String artifactId, String version, String jarsPath) {
        JarInformation jarInformation;
        String url = "http://central.maven.org/maven2/" + groupId + "/" + artifactId + "/" + version + "/" + artifactId
                + "-" + version + ".jar";

        jarInformation = getAsJarInformation(url, groupId, artifactId, version, jarsPath);

        if (jarInformation == null) {
            url = "http://central.maven.org/maven2/org/" + groupId + "/" + artifactId + "/" + version + "/" + artifactId
                    + "-" + version + ".jar";

            jarInformation = getAsJarInformation(url, groupId, artifactId, version, jarsPath);
        }

        if (jarInformation == null) {
            url = "http://central.maven.org/maven2/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                    + "/" + artifactId + "-" + version + ".jar";

            jarInformation = getAsJarInformation(url, groupId, artifactId, version, jarsPath);
        }

        if (jarInformation == null) {
            url = "https://repo1.maven.org/maven2/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                    + "/" + artifactId + "-" + version + ".jar";

            jarInformation = getAsJarInformation(url, groupId, artifactId, version, jarsPath);
        }

        return jarInformation;
    }

    private static JarInformation getAsJarInformation(JarFile jarFile, String groupId, String artifactId, String version) {
        if (jarFile == null)
            return null;

        return new JarInformation(jarFile, groupId, artifactId, version);
    }

    private static JarInformation getAsJarInformation(String url, String groupId, String artifactId, String version, String jarsPath) {
        JarFile jarFile = DownloadJar(url, jarsPath);
        return getAsJarInformation(jarFile, groupId, artifactId, version);
    }

    private static JarFile DownloadJar(String jarUrl, String jarsPath) {
        String jarName = Utility.getJarName(jarUrl);
//        String jarsPath =

        String jarLocation = jarsPath + '/' + jarName;
        JarFile jarFile = null;
        File file = new File(jarLocation);
        if (file.exists()) {
            try {
                return new JarFile(new File(jarLocation));
            } catch (IOException e) {
                logger.error("Cannot open jar: " + jarLocation, e);
            }
        }
        try {
            Utility.downloadUsingStream(jarUrl, jarLocation);
        } catch (IOException e) {
            logger.error("Could not download jar: " + jarUrl, e);
        }

        try {
            jarFile = new JarFile(new File(jarLocation));
        } catch (IOException e) {
            logger.error("Cannot open jar: " + jarLocation, e);
        }

        return jarFile;
    }
}
