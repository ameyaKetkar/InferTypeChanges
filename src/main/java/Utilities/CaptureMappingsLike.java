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
            return (((VariableDeclarationFragment) n).getInitializer() == null)? "": ((VariableDeclarationFragment) n).getInitializer().toString();
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
                            ":[exc~([A-Z][a-z0-9]+)+].<:[ta]>:[[c]]" + "(" + generateArgs(mi.arguments().size()) + ")",
                            ":[r].:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")",
                            ":[r].<:[ta]>:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")");
                }
            }catch (Exception e){
                return new ArrayList<>();
            }
            return List.of(":[r].:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")",
                    ":[r].<:[ta]>:[[mc]]" + "(" + generateArgs(mi.arguments().size()) + ")");
        }
        else if(ast.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION){
            ClassInstanceCreation cic = (ClassInstanceCreation) ast;
            return List.of("new :[[c]]" + "(" + generateArgs(cic.arguments().size()) + ")",
                        "new :[[c]]<:[ta]>" + "(" + generateArgs(cic.arguments().size()) + ")");
        }
        else if(ast.getNodeType() == ASTNode.ASSIGNMENT) return List.of(":[nm]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]");
        else if(ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT)
            return List.of(":[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]",
                ":[mod:e] :[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]");
        else if(ast.getNodeType() == ASTNode.INFIX_EXPRESSION)
            return List.of(":[r]:[op~\\s*(==|!=|&&|\\+|\\-|\\*|\\|\\|)\\s*]:[a]");
        else if(ast.getNodeType() == ASTNode.PREFIX_EXPRESSION) return List.of(":[b~(\\+|\\-|!)+]:[r]");
        else if(ast.getNodeType() == ASTNode.POSTFIX_EXPRESSION) return List.of(":[r]:[b~(\\+|\\-|!)+]");
        else if(ast.getNodeType() == ASTNode.RETURN_STATEMENT) return List.of("return :[e]");
        else if(ast.getNodeType() == ASTNode.LAMBDA_EXPRESSION) {
            return List.of(generateArgs(((LambdaExpression)ast).parameters().size()) + ":[l~\\s*(->)\\s*]:[b]");
        }
        else if(ast.getNodeType() == ASTNode.CAST_EXPRESSION) return List.of("(:[t]):[ex]");
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

}
