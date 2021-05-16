package Utilities;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.gen.jdt.JdtVisitor;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.common.collect.Streams;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.CodeMapping;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.*;
import Utilities.comby.Range__1;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.*;

public class ASTUtils {


    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    public static List<String> getAllTokens(String code){
        try {
            // Create the tokenizer to read from a file

            StringReader r = new StringReader(code);
            StreamTokenizer st = new StreamTokenizer(r);

            // Prepare the tokenizer for Java-style tokenizing rules
            st.parseNumbers();
            st.wordChars('_', '_');
            st.eolIsSignificant(true);

            // If whitespace is not to be discarded, make this call
            st.ordinaryChars(0, ' ');

            // These calls caused comments to be discarded
            st.slashSlashComments(true);
            st.slashStarComments(true);

            // Parse the file
            int token = st.nextToken();
            List<String> tokens = new ArrayList<>();
            while (token != StreamTokenizer.TT_EOF) {
                switch (token) {
                    case StreamTokenizer.TT_NUMBER:
                        // A number was found; the value is in nval
                        tokens.add(String.valueOf(st.nval));
                        token = st.nextToken();
                        break;
                    case StreamTokenizer.TT_WORD:
                    case '"':
                    case '\'':
                        // A single-quoted string was found; sval contains the contents
                        // A double-quoted string was found; sval contains the contents
                        // A word was found; the value is in sval
                        tokens.add(st.sval);
                        token = st.nextToken();
                        break;
                    case StreamTokenizer.TT_EOL:
                        // End of line character found
                        break;
                    case StreamTokenizer.TT_EOF:
                        // End of file has been reached
                        break;
                    default:
                        // A regular character was found; the value is the token itself
                        tokens.add(String.valueOf((char)st.ttype));
                        token = st.nextToken();
                        break;
                }
            }
        r.close();
        return tokens;
        } catch (IOException ignored) {
        }

        return new ArrayList<>();
    }
//
//    public static List<String> getAllTokens(String expr) {
//
//        List<String> tokens = tokenize(expr);
//        Scanner sc = new Scanner();
//        sc.setSource(expr.toCharArray());
//        while (!sc.atEnd()) {
//            try {
//                sc.getNextToken();
//                tokens.add(sc.getCurrentTokenString());
//            } catch (InvalidInputException e) {
//                LOGGER.warning("Could not tokenize the string : " + expr);
//                return new ArrayList<>();
//            }
//
//        }
//        return tokens;
//    }

//    public static int getTotal(String expr) {
//        List<String> tokens = new ArrayList<>();
//        Scanner sc = new Scanner();
//        sc.setSource(expr.toCharArray());
//        while (!sc.atEnd()) {
//            try {
//                sc.getNextToken();
//                tokens.add(sc.getCurrentTokenString());
//            } catch (InvalidInputException e) {
//                e.printStackTrace();
//                return new ArrayList<>();
//            }
//
//        }
//        return tokens;
//    }

    public static Optional<Statement> getStatement(String expr) {
        try {
            ASTParser parser = ASTParser.newParser(AST.JLS15);
            parser.setKind(ASTParser.K_STATEMENTS);
            Map<String, String> pOptions = JavaCore.getOptions();
            pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
            pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
            pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
            pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
            parser.setStatementsRecovery(true);
            parser.setCompilerOptions(pOptions);
            char[] source = expr.toCharArray();
            parser.setSource(source);
            IScanner scanner = ToolFactory.createScanner(false, false, false, false);
            scanner.setSource(source);
            ASTNode node = parser.createAST(null);
            return Optional.ofNullable((Statement) node);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Tuple2<MappingStore, List<Action>> getGumTreeEditScript(ASTNode before, ASTNode after) {
//        try {
//            TreeContext src = getGumTreeContextForASTNode(before).orElseThrow();
//            TreeContext dst = getGumTreeContextForASTNode(after).orElseThrow();
//            Matcher m = Matchers.getInstance().getMatcher("gumtree-simple");
//            EditScriptGenerator e = new SimplifiedChawatheScriptGenerator();
//            if (src == null || dst == null) return Tuple.of(null, new ArrayList<>());
//            MappingStore match = m.match(src.getRoot(), dst.getRoot());
//            EditScript editScript = e.computeActions(match);
//            return Tuple.of(match, editScript.asList());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return Tuple.of(null, new ArrayList<>());
    }


//    public static IUpdate getUpdateNew(ASTNode before, ASTNode after) {
//
//        ITree root1 = getGumTreeContextForASTNode(before).map(c -> c.getRoot()).orElse(null);
//        ITree root2 = getGumTreeContextForASTNode(after).map(c -> c.getRoot()).orElse(null);
//
//        if (root1 == null || root2 == null || root1.isIsomorphicTo(root2)) {
//            return new NoUpdate();
//        }
//
//        Update upd = new Update(root1, root2, before.toString(), after.toString());
//
//        if (before instanceof Expression && after instanceof Expression)
//            upd.setExplanation(getInstance(before.toString(), after.toString()));
//        if (root1.hasSameType(root2)) {
//            List<Tuple2<ASTNode, ASTNode>> ast_children = zip(getChildren(root1), getChildren(root2),
//                    (x, y) -> getCoveringNode(before, x).flatMap(t -> getCoveringNode(after, y).map(z -> Tuple.of(t, z))))
//                    .flatMap(Optional::stream)
//                    .collect(toList());
//            ast_children.forEach(t -> upd.addSubExplanation(getUpdate(t._1(), t._2())));
//        }
//        return upd;
//    }

//    public static boolean isIsomorphic(String s1, String s2, Statement ss1, Statement ss2){
//        ITree root1 = ASTUtils.getGumTreeContextForASTNode(ss1).map(c -> c.getRoot()).orElse(null);
//        ITree root2 = ASTUtils.getGumTreeContextForASTNode(ss2).map(c -> c.getRoot()).orElse(null);
//
//    }

    public static Stream<Tree> getChildren(Tree root1) {
        return root1.getChildren().stream()
                .flatMap(c -> c.getType().name.equals("METHOD_INVOCATION_ARGUMENTS") ? c.getChildren().stream() : Stream.of(c));
    }

    public static Optional<ASTNode> getCoveringNode(ASTNode node, Tree tree) {
        var n = NodeFinder.perform(node, tree.getPos(), tree.getLength());
        if (n == null) return Optional.empty();
        if (n.getStartPosition() != tree.getPos() || n.getLength() != tree.getLength()) return Optional.empty();
        return Optional.of(n);
    }

    public static Optional<ASTNode> getCoveringNode(ASTNode node, Range__1 r) {
        try {
            int length = r.getEnd().getOffset() - r.getStart().getOffset();
            int start = node.getStartPosition() + r.getStart().getOffset();
            var n = NodeFinder.perform(node, start, length);
            if (n == null) return Optional.empty();
            if (n.getStartPosition() != start || n.getLength() != length) return Optional.empty();
            return Optional.of(n);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean isSubsumedBy(Tree tree, Tree subsumedTree){
        return (subsumedTree.getPos() > tree.getPos() && subsumedTree.getEndPos() <= tree.getEndPos())
                || (subsumedTree.getPos() >= tree.getPos() && subsumedTree.getEndPos() < tree.getEndPos());
    }

    public static Optional<TreeContext> getGumTreeContextForASTNode(ASTNode ast) {
        try {
            IScanner scanner = ToolFactory.createScanner(false, false, false, false);
            scanner.setSource(ast.toString().toCharArray());
            AbstractJdtVisitor v = new JdtVisitor(scanner);
            ast.accept(v);
            return Optional.of(v.getTreeContext());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<CompilationUnit> getResolvedCompilationUnit(String cu_path, Path pathCommitFiles, Set<Tuple3<String, String, String>> jarArtifactInfoSet, String pathToThirdPartyLibs, String pathToJdk){
        Path cuPath = pathCommitFiles.resolve(cu_path);
        ASTParser parser = ASTParser.newParser(AST.JLS15);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        String unitName = cuPath.getFileName().toString();
        parser.setUnitName(unitName);
        try {

            String[] sources = Files.walk(pathCommitFiles)
                    .filter(Files::isDirectory)
                    .filter(x -> x.endsWith("java"))
                    .map(x -> x.toAbsolutePath().toString()).toArray(String[]::new);

            List<String> jarfiles = jarArtifactInfoSet.stream()
                    .map(x -> Path.of(pathToThirdPartyLibs)
                            .resolve(x._2() + "-" + x._3() + ".jar").toAbsolutePath().toString())
                    .collect(toList());

            String[] classpath = new String[jarfiles.size() + 1];
            classpath[0] = pathToJdk;
            int c = 1;
            for (var j : jarfiles){
                classpath[c] = j;
                c+=1;
            }

            String[] encodings = new String[sources.length];
            Arrays.fill(encodings, "UTF-8");
            parser.setEnvironment(classpath,sources, encodings, true);
            var content = Try.of(() -> Files.readString(cuPath)).getOrElse("");
            parser.setSource(content.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            if (cu.getAST().hasBindingsRecovery()) {
                System.out.println("Binding activated.");
                return Optional.of(cu);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static boolean isNotWorthLearning(CodeMapping cm){
        return cm.getIsSame() || cm.getReplcementInferredList().stream().allMatch(x -> x.getReplacementType().equals("VARIABLE_NAME")
                || x.getReplacementType().equals("STRING_LITERAL"));
    }

    public static <T> List<T>  mergeList (List<T> l1, List<T> l2){
            return Streams.concat(l1.stream(), l2.stream()).collect(toList());
    }



    public static String extractCommit(String url){
        Pattern pattern = Pattern.compile("(.*)(commit/)(\\w+)(.*)");
        Matcher m =pattern.matcher(url);
        return m.matches() ? m.group(3) : "";


    }

    public static String extractLineNumber(String url){
        Matcher m =Pattern.compile("(.*)([L-R])(\\d+)($)").matcher(url);
        if(m.matches())
            return m.group(3);
        return "";
    }

    public static String extractProject(String url){
        Pattern pattern = Pattern.compile("(.*)(github.com/)(.*)(/commit)(.*)");
        Matcher m = pattern.matcher(url);
        if(m.matches())
            return m.group(3);
        return "";
    }

//    public static void main(String a[]){
//        List<String> x = getAllTokens("additionalClasspathEntries=parser.acceptsAll(asList(\"cp\",\"classpath\",\"class-path\"),\"Provide additional classpath entries -- for example, for adding engines and their dependencies. \" + \"This option can be repeated.\").withRequiredArg().ofType(File.class).withValuesSeparatedBy(File.pathSeparatorChar).describedAs(\"path1\" + File.pathSeparator + \"path2\"+ File.pathSeparator+ \"...\")");
//        System.out.println(x);
//    }




//    public static List<Expression>  getExpressionOfType(ITypeBinding iTypeBinding, ASTNode astNode) {
//        var mv1 = new ExpressionOfType(iTypeBinding.getBinaryName());
//        astNode.accept(mv1);
//        return mv1.relevantExpressions;
//    }

}
