package Utilities;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import Utilities.comby.CombyMatch;
import org.checkerframework.common.value.qual.IntRange;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class CaptureMappingsLike {


    // "r" and "c" are special names
    // "r" should be the left-most temnplate variable if it represents ANY expression
    // template variables "c" are never considered for generalization
    public static <U,V> Function<U, Optional<V>> checkAndThen(Predicate<U> test, Function<U, V> f){
        return u -> test.test(u) ? Optional.ofNullable(f.apply(u)) : Optional.empty();
    }


    public static String getTheUnBoundedTemplateVar(ASTNode n){
        if(n.getNodeType() == ASTNode.ASSIGNMENT)
            return ((Assignment)n).getRightHandSide().toString();
        else if(n.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
            VariableDeclarationStatement n1 = (VariableDeclarationStatement) n;
            if(n1.fragments().size() > 0)
                return getTheUnBoundedTemplateVar((VariableDeclarationFragment)n1.fragments().get(0));
        }else if(n.getNodeType() == ASTNode.METHOD_INVOCATION && ((MethodInvocation)n).getExpression() != null)
            return ((MethodInvocation) n).getExpression().toString();
        else if(n.getNodeType() == ASTNode.INFIX_EXPRESSION)
            return ((InfixExpression) n).getLeftOperand().toString();
        else if(n.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT)
            return ((VariableDeclarationFragment) n).getInitializer().toString();
        else if(n.getNodeType() == ASTNode.POSTFIX_EXPRESSION)
            return ((PostfixExpression)n).getOperand().toString();
        else if(n.getNodeType() == ASTNode.PREFIX_EXPRESSION)
            return ((PrefixExpression)n).getOperand().toString();
        return "";
    }

    public static List<String> getTemplatesFor(ASTNode ast){
        if(ast.getNodeType() == ASTNode.METHOD_INVOCATION){
            MethodInvocation mi = (MethodInvocation) ast;
            try {
                if (mi.getExpression()!= null && ASTNode.SIMPLE_NAME == mi.getExpression().getNodeType() && Character.isUpperCase(mi.getExpression().toString().charAt(0))) {
                    return List.of(":[exc~([A-Z][a-z0-9]+)+].:[[c]]" + "(" + generateArgs(mi.arguments().size()) + ")",
                            ":[exc~([A-Z][a-z0-9]+)+].<:[ta]>:[[c]]" + "(" + generateArgs(mi.arguments().size()) + ")");
                }
            }catch (Exception e){
                return new ArrayList<>();
            }
            return List.of(":[r].:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")",
                    ":[r].<:ta>:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")");
        }
        else if(ast.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION){
            ClassInstanceCreation cic = (ClassInstanceCreation) ast;
            return List.of("new :[[c]]" + "(" + generateArgs(cic.arguments().size()) + ")",
                        "new:[[c]]<:ta>" + "(" + generateArgs(cic.arguments().size()) + ")");
        }
        else if(ast.getNodeType() == ASTNode.ASSIGNMENT) return List.of(":[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]");
        else if(ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT)
            return List.of(":[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]",
                ":[mod:e] :[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]");
        else if(ast.getNodeType() == ASTNode.INFIX_EXPRESSION)
            return List.of(":[r]:[op~\\s*(==|!=|&&|\\+|\\-|\\*|\\|\\|)\\s*]:[a:e]");
        else if(ast.getNodeType() == ASTNode.PREFIX_EXPRESSION) return List.of(":[b~(\\+|\\-|!)+]:[r]");
        else if(ast.getNodeType() == ASTNode.POSTFIX_EXPRESSION) return List.of(":[r]:[b~(\\+|\\-|!)+]");
        else if(ast.getNodeType() == ASTNode.RETURN_STATEMENT) return List.of("return :[e]");
        else if(ast.getNodeType() == ASTNode.LAMBDA_EXPRESSION) {
            return List.of(generateArgs(((LambdaExpression)ast).parameters().size()) + ":[l~\\s*(->)\\s*]:[b]");
        }
        else if(ast.getNodeType() == ASTNode.CAST_EXPRESSION) return List.of("(:[t:e]):[ex]");
        else if(ast.getNodeType() == ASTNode.STRING_LITERAL) return List.of(":[a~\\\".*\\\"]");
        else if(ast.getNodeType() == ASTNode.MEMBER_REF) return List.of(":[[ty]]:::[[c]]");
        else if(ast.getNodeType() == ASTNode.NUMBER_LITERAL) return List.of(":[n~[+-]?(\\d*\\.)?\\d+$]",
                ":[d~[+-]?(\\d*\\.)?\\d+]:[c~(L|l|f|F)]",
                ":[h~0[xX][0-9a-fA-F]+]",":[h~0[xX][0-9a-fA-F]+]:[c~(L|l|f|F)]");
        else if(ast.getNodeType() == ASTNode.BOOLEAN_LITERAL) return List.of(":[st~false]",":[st~true]");
        return List.of(":[[id]]");

    }

    public static String generateArgs(int i){
        return IntStream.range(0,i).mapToObj(x -> ":[a"+x+"]").collect(joining(","));
    }

    public static LinkedHashMap<String, Tuple2<String, Predicate<String>>> SYNTACTIC_TYPE_CHANGES = new LinkedHashMap<>() {
        {
            put("primInt", Tuple.of(":[[s~\bint\b]]", s -> s.contains("int")));
            put("primShort", Tuple.of(":[[s~\bshort\b]]", s -> s.contains("short")));
            put("primLong", Tuple.of(":[[s~\blong\b]]", s -> s.contains("long")));
            put("primFloat", Tuple.of(":[[s~\bfloat\b]]", s -> s.contains("float")));
            put("primDouble", Tuple.of(":[[s~\bdouble\b]]", s -> s.contains("double")));
            put("primByte", Tuple.of(":[[s~\bbyte\b]]", s -> s.contains("byte")));
            put("primBool", Tuple.of(":[[s~\bboolean\b]]", s -> s.contains("boolean")));
            put("SimpleType", Tuple.of(":[[s]]", s -> !s.contains(" ")));
            put("ParameterizedType3", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[t1r],:[t2r],:[t3r]>", s -> s.contains("<")));
            put("ParameterizedType2", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[tar],:[tbr]>", s -> s.contains("<")));
            put("ParameterizedType1", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[tar]>", s -> s.contains("<")));
            put("ArrayType", Tuple.of(":[c~\\w+[?:\\.\\w+]+][]", s -> s.contains("[")));
            put("ExtendsType", Tuple.of(":[x] extends :[r]", s -> s.contains("extends")));
            put("SuperType", Tuple.of(":[x] super :[r]", s -> s.contains("super")));
            put("QualifiedType", Tuple.of(":[c~\\w+[?:\\.\\w+]+]", s -> s.contains(".")));
        }
    };


//    public static LinkedHashMap<String, Tuple2<String, Predicate<String>>> PATTERNS_HEURISTICS = new LinkedHashMap<>() {
//        {
//            put("Nothing", Tuple.of("", s -> s.equals("")));
//            put("Cast", Tuple.of("(:[t:e]):[ex]", s -> s.startsWith("(")));
//            put("Negate", Tuple.of("!:[ex]", s -> s.startsWith("!")));
//            put("BinaryOp", Tuple.of(":[r]:[op~\\s*(==|!=|&&|\\+|\\-|\\*|\\|\\|)\\s*]:[a:e]", s -> Stream.of("+", "-", "==", "*", "&&", "!=").anyMatch(s::contains)));
//            put("TypeArgs", Tuple.of("<:[tas]>", s -> s.startsWith("<")));
//            put("ClassName", Tuple.of(":[c~([A-Z][a-z0-9]+)+]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(") && s.length() > 0 && Character.isUpperCase(s.charAt(0)) && Character.isAlphabetic(s.charAt(0))));
//            put("NumberLiteral1", Tuple.of(":[n~\\d]", s -> s.length() > 0 && Character.isDigit(s.charAt(0))));
//            put("NumberLiteral", Tuple.of(":[n~\\d]:[c~[L-l]]", s -> s.length() > 0 && Character.isDigit(s.charAt(0))));
//            put("Hexdecimal1", Tuple.of(":[h~0[xX][0-9a-fA-F]+]", s -> s.length() > 0 && Character.isDigit(s.charAt(0))));
//            put("Hexdecimal", Tuple.of(":[h~0[xX][0-9a-fA-F]+]:[c~[L-l]]", s -> s.length() > 0 && Character.isDigit(s.charAt(0))));
//            put("StringLiteral", Tuple.of(":[st]", s -> s.startsWith("\"")));
//            put("false", Tuple.of(":[st~false]", s -> s.equals("false")));
//            put("true", Tuple.of(":[st~true]", s -> s.equals("true")));
//            put("Identifier", Tuple.of(":[[id]]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(")));
//            put("Member-Reference", Tuple.of(":[[ty]]:::[[c]]", s -> s.contains("::")));
//            put("Expression", Tuple.of("<:[exp:e]>", s -> s.startsWith("<")));
//            put("Assignment1", Tuple.of(":[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
//            put("Assignment2", Tuple.of(":[mod:e] :[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
//            put("Assignment", Tuple.of(":[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
//            put("ReturnStmt", Tuple.of("return :[e]", s -> s.contains("return ")));
//            put("Arguments", Tuple.of(":[a],:[b]", s -> s.contains(",")));
//            put("LambdaExpression", Tuple.of(":[[a]]->:[b]", s -> s.contains(",")));
//
//            Predicate<String> methodInvocation = s -> s.contains(".") && s.contains("(") && s.contains(")");
//            Predicate<String> newInstanceCreation = s -> s.startsWith("new");
//            Function<Integer, Predicate<String>> commaCount = i -> s -> s.chars().filter(x -> x == ',').count() >= i;
//
//
//            put("ClassInstanceCreation51", Tuple.of("new :[[c]](:[a1],:[a2],:[a3],:[a4],:[a5])", newInstanceCreation.and(commaCount.apply(4))));
//            put("ClassInstanceCreation5", Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3],:[a4],:[a5])", newInstanceCreation.and(commaCount.apply(4))));
//            put("MethodInvocation51", Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3],:[a4],:[a5])", methodInvocation.and(commaCount.apply(4))));
//            put("MethodInvocation5", Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3],:[a4],:[a5])", methodInvocation.and(commaCount.apply(4))));
//
//            put("ClassInstanceCreation41", Tuple.of("new :[[c]](:[a1],:[a2],:[a3],:[a4])", newInstanceCreation.and(commaCount.apply(3))));
//            put("ClassInstanceCreation4", Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3],:[a4])", newInstanceCreation.and(commaCount.apply(3))));
//            put("MethodInvocation41", Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3],:[a4])", methodInvocation.and(commaCount.apply(3))));
//            put("MethodInvocation4", Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3],:[a4])", methodInvocation.and(commaCount.apply(3))));
//
//            put("ClassInstanceCreation31", Tuple.of("new :[[c]](:[a1],:[a2],:[a3])", newInstanceCreation.and(commaCount.apply(2))));
//            put("ClassInstanceCreation3", Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3])", newInstanceCreation.and(commaCount.apply(2))));
//            put("MethodInvocation31", Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3])", methodInvocation.and(commaCount.apply(2))));
//            put("MethodInvocation3", Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3])", methodInvocation.and(commaCount.apply(2))));
//
//            put("ClassInstanceCreation21", Tuple.of("new :[[c]](:[a1],:[a2])", newInstanceCreation.and(commaCount.apply(1))));
//            put("ClassInstanceCreation2", Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2])", newInstanceCreation.and(commaCount.apply(1))));
//            put("MethodInvocation21", Tuple.of(":[r].:[[c]](:[a1],:[a2])", methodInvocation.and(commaCount.apply(1))));
//            put("MethodInvocation2", Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2])", methodInvocation.and(commaCount.apply(1))));
//
//            put("ClassInstanceCreation01", Tuple.of("new :[[c]]()", newInstanceCreation));
//            put("ClassInstanceCreation0", Tuple.of("new :[[c]]<:[3]>()", newInstanceCreation));
//            put("MethodInvocation01", Tuple.of(":[r].:[[c]]()", methodInvocation));
//            put("MethodInvocation0", Tuple.of(":[r].:[[c]]<:[12]>()", methodInvocation));
////
////
////            put("ClassInstanceCreation11", Tuple.of("new :[[c]](:[a1])", newInstanceCreation));
////            put("ClassInstanceCreation1", Tuple.of("new :[[c]]<:[3]>(:[a1])", newInstanceCreation));
////            put("MethodInvocation11", Tuple.of(":[r].:[[c]](:[a1])", methodInvocation));
////            put("MethodInvocation1", Tuple.of(":[r].:[[c]]<:[12]>(:[a1])", methodInvocation));
//
////            put("Anything", Tuple.of(":[anything]", s -> true));
//
//        }
//    };



//    public static Map<String, Optional<CombyMatch>> STOCK_TVs = PATTERNS_HEURISTICS.entrySet().stream()
//            .collect(toMap(x -> x.getValue()._1(), x -> CombyUtils.getMatch(":[:[var]]", x.getValue()._1(), null)));
}
