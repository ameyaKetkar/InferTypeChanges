package Utilities;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import Utilities.comby.CombyMatch;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class CaptureMappingsLike {


    // "r" and "c" are special names
    // "r" should be the left-most temnplate variable if it represents ANY expression
    // template variables "c" are never considered for generalization
    public static LinkedHashMap<String, Tuple2<String, Predicate<String>>> PATTERNS_HEURISTICS = new LinkedHashMap<>(){
        {
            put("Nothing",Tuple.of("", s -> s.equals("")));
            put("Cast",Tuple.of("(:[t:e]):[ex]", s -> s.startsWith("(")));
            put("Negate",Tuple.of("!:[ex]", s -> s.startsWith("!")));
            put("BinaryOp",Tuple.of(":[r]:[op~\\s*(==|!=|&&|\\+|\\-|\\*|\\|\\|)\\s*]:[a:e]", s -> Stream.of("+","-","==","*","&&", "!=").anyMatch(s::contains)));
            put("TypeArgs",Tuple.of("<:[tas]>", s -> s.startsWith("<")));
            put("ClassName",Tuple.of(":[c~([A-Z][a-z0-9]+)+]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(") && s.length()>0 && Character.isUpperCase(s.charAt(0)) && Character.isAlphabetic(s.charAt(0))));
            put("NumberLiteral1",Tuple.of(":[n~\\d]", s -> s.length()>0 && Character.isDigit(s.charAt(0))));
            put("NumberLiteral",Tuple.of(":[n~\\d]:[c~[L-l]]", s -> s.length()>0 && Character.isDigit(s.charAt(0))));
            put("Hexdecimal1",Tuple.of(":[h~0[xX][0-9a-fA-F]+]", s -> s.length()>0 && Character.isDigit(s.charAt(0))));
            put("Hexdecimal",Tuple.of(":[h~0[xX][0-9a-fA-F]+]:[c~[L-l]]", s -> s.length()>0 && Character.isDigit(s.charAt(0))));
            put("StringLiteral",Tuple.of(":[st]", s->s.startsWith("\"")));
            put("false",Tuple.of(":[st~false]", s->s.equals("false")));
            put("true",Tuple.of(":[st~true]", s->s.equals("true")));
            put("Identifier",Tuple.of(":[[id]]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(")));
            put("Member-Reference", Tuple.of(":[[ty]]:::[[c]]", s -> s.contains("::")));
            put("Expression",Tuple.of("<:[exp:e]>", s -> s.startsWith("<")));
            put("Assignment1",Tuple.of(":[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
            put("Assignment2",Tuple.of(":[mod:e] :[ty:e] :[[nm]]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
            put("Assignment",Tuple.of(":[nm:e]:[nm:e]:[29~\\s*(\\+|\\-|\\*|\\&)*=\\s*]:[r]", s -> s.contains("=")));
            put("ReturnStmt",Tuple.of("return :[e]", s -> s.contains("return ")));

            put("Arguments",Tuple.of(":[a],:[b]", s -> s.contains(",")));
            put("LambdaExpression",Tuple.of(":[[a]]->:[b]", s -> s.contains(",")));

            Predicate<String> methodInvocation = s -> s.contains(".") && s.contains("(") && s.contains(")");
            Predicate<String> newInstanceCreation = s -> s.startsWith("new");
            Function<Integer, Predicate<String>> commaCount = i -> s -> s.chars().filter(x->x ==',').count() >=i;


            put("ClassInstanceCreation51",Tuple.of("new :[[c]](:[a1],:[a2],:[a3],:[a4],:[a5])", newInstanceCreation.and(commaCount.apply(4))));
            put("ClassInstanceCreation5",Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3],:[a4],:[a5])", newInstanceCreation.and(commaCount.apply(4))));
            put("MethodInvocation51",Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3],:[a4],:[a5])", methodInvocation.and(commaCount.apply(4))));
            put("MethodInvocation5",Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3],:[a4],:[a5])", methodInvocation.and(commaCount.apply(4))));

            put("ClassInstanceCreation41",Tuple.of("new :[[c]](:[a1],:[a2],:[a3],:[a4])", newInstanceCreation.and(commaCount.apply(3))));
            put("ClassInstanceCreation4",Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3],:[a4])", newInstanceCreation.and(commaCount.apply(3))));
            put("MethodInvocation41",Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3],:[a4])", methodInvocation.and(commaCount.apply(3))));
            put("MethodInvocation4",Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3],:[a4])", methodInvocation.and(commaCount.apply(3))));

            put("ClassInstanceCreation31",Tuple.of("new :[[c]](:[a1],:[a2],:[a3])", newInstanceCreation.and(commaCount.apply(2))));
            put("ClassInstanceCreation3",Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2],:[a3])", newInstanceCreation.and(commaCount.apply(2))));
            put("MethodInvocation31",Tuple.of(":[r].:[[c]](:[a1],:[a2],:[a3])", methodInvocation.and(commaCount.apply(2))));
            put("MethodInvocation3",Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2],:[a3])", methodInvocation.and(commaCount.apply(2))));

            put("ClassInstanceCreation21",Tuple.of("new :[[c]](:[a1],:[a2])", newInstanceCreation.and(commaCount.apply(1))));
            put("ClassInstanceCreation2",Tuple.of("new :[[c]]<:[3]>(:[a1],:[a2])", newInstanceCreation.and(commaCount.apply(1))));
            put("MethodInvocation21",Tuple.of(":[r].:[[c]](:[a1],:[a2])", methodInvocation.and(commaCount.apply(1))));
            put("MethodInvocation2",Tuple.of(":[r].:[[c]]<:[12]>(:[a1],:[a2])", methodInvocation.and(commaCount.apply(1))));

            put("ClassInstanceCreation01",Tuple.of("new :[[c]]()", newInstanceCreation));
            put("ClassInstanceCreation0",Tuple.of("new :[[c]]<:[3]>()", newInstanceCreation));
            put("MethodInvocation01",Tuple.of(":[r].:[[c]]()", methodInvocation));
            put("MethodInvocation0",Tuple.of(":[r].:[[c]]<:[12]>()", methodInvocation));


            put("ClassInstanceCreation11",Tuple.of("new :[[c]](:[a1])", newInstanceCreation));
            put("ClassInstanceCreation1",Tuple.of("new :[[c]]<:[3]>(:[a1])", newInstanceCreation));
            put("MethodInvocation11",Tuple.of(":[r].:[[c]](:[a1])", methodInvocation));
            put("MethodInvocation1",Tuple.of(":[r].:[[c]]<:[12]>(:[a1])", methodInvocation));
            
            put("Anything",Tuple.of(":[anything]", s -> true));

        }
    };

    public static LinkedHashMap<String, Tuple2<String, Predicate<String>>> SYNTACTIC_TYPE_CHANGES = new LinkedHashMap<>(){
        {
            put("primInt", Tuple.of(":[[s~\bint\b]]",s -> s.contains("int")));
            put("primShort", Tuple.of(":[[s~\bshort\b]]",s -> s.contains("short")));
            put("primLong", Tuple.of(":[[s~\blong\b]]",s -> s.contains("long")));
            put("primFloat", Tuple.of(":[[s~\bfloat\b]]",s -> s.contains("float")));
            put("primDouble", Tuple.of(":[[s~\bdouble\b]]",s -> s.contains("double")));
            put("primByte", Tuple.of(":[[s~\bbyte\b]]",s -> s.contains("byte")));
            put("primBool", Tuple.of(":[[s~\bboolean\b]]",s -> s.contains("boolean")));
            put("SimpleType", Tuple.of(":[[s]]",s -> !s.contains(" ")));
            put("ParameterizedType3", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[t1r],:[t2r],:[t3r]>",s -> s.contains("<")));
            put("ParameterizedType2", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[tar],:[tbr]>",s -> s.contains("<")));
            put("ParameterizedType1", Tuple.of(":[c~\\w+[?:\\.\\w+]+]<:[tar]>",s -> s.contains("<")));
            put("ArrayType", Tuple.of(":[c~\\w+[?:\\.\\w+]+][]",s -> s.contains("[")));
            put("ExtendsType", Tuple.of(":[x] extends :[r]",s -> s.contains("extends")));
            put("SuperType", Tuple.of(":[x] super :[r]",s -> s.contains("super")));
            put("QualifiedType", Tuple.of(":[c~\\w+[?:\\.\\w+]+]",s -> s.contains(".")));
        }
    };

    public static Map<String, Optional<CombyMatch>> STOCK_TVs = PATTERNS_HEURISTICS.entrySet().stream()
            .collect(toMap(x -> x.getValue()._1(), x -> CombyUtils.getMatch(":[:[var]]", x.getValue()._1(), null)));
}
