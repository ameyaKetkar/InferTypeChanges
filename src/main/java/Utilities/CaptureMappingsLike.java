package Utilities;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class CaptureMappingsLike {

    public static Map<String, String> PATTERNS = new HashMap<>(){
        {
            put("Nothing","");
            put("Assignment",":[27r]:[28~\\s*]:[29~[\\+\\-\\*\\&]*]=:[30~\\s*]:[31]");
            put("VarDecl",":[ty:e] :[[nm]]");
            put("VarDecl1",":[mod:e] :[ty:e] :[[nm]]");
            put("Cast","(:[25:e]):[26]");
            put("ClassInstanceCreation","new :[[1c]]:[3](:[2r])");
            put("MethodInvocation",":[9r].:[[10c]]:[12r](:[11r])");
            put("Member-Reference",":[[22]]:::[[23]]");
            put("NumberLiteral",":[50~\\d]:[51r]");
            put("longLit",":[52x1~[L-l]]");
            put("Hexdecimal",":[54~0[xX][0-9a-fA-F]+]:[52]");
            put("ClassName",":[51c~([A-Z][a-z0-9]+)+]");
            put("Arguments5",":[a1],:[a2],:[a3],:[a4],:[a5]");
            put("Arguments4",":[a1],:[a2],:[a3],:[a4]");
            put("Arguments3",":[a1],:[a2],:[a3]");
            put("Arguments2",":[a1],:[a2]");
            put("TypeArgs","<:[tas]>");
            put("Identifier",":[[35]]");
            put("Expression","<:[exp:e]>");
            put("Anything",":[anything]");


        }
    };

    public static Map<String, Tuple2<String, Predicate<String>>> PATTERNS_HEURISTICS = new HashMap<>(){
        {
            put("Nothing",Tuple.of("", s -> s.equals("")));
            put("Assignment",Tuple.of(":[27r]:[28~\\s*]:[29~[\\+\\-\\*\\&]*]=:[30~\\s*]:[31]", s -> s.contains("=")));
            put("VarDecl",Tuple.of(":[ty:e] :[[nm]]", s -> s.contains(" ")));
            put("VarDecl1",Tuple.of(":[mod:e] :[ty:e] :[[nm]]", s -> s.chars().filter(x->x ==' ').count() >=2));
            put("Cast",Tuple.of("(:[25:e]):[26]", s -> s.startsWith("(")));
            put("ClassInstanceCreation",Tuple.of("new :[[1c]]:[3](:[2r])", s -> s.startsWith("new")));
            put("MethodInvocation",Tuple.of(":[9r].:[[10c]]:[12r](:[11r])", s -> s.contains(".") && s.contains("(") && s.contains(")")));
            put("Member-Reference", Tuple.of(":[[22]]:::[[23]]", s -> s.contains("::")));
            put("NumberLiteral",Tuple.of(":[50~\\d]:[51r]", s -> Character.isDigit(s.charAt(0))));
            put("longLit",Tuple.of(":[52x1~[L-l]]",s -> Character.isDigit(s.charAt(0))));
            put("Hexdecimal",Tuple.of(":[54~0[xX][0-9a-fA-F]+]:[52]", s -> Character.isDigit(s.charAt(0))));
            put("ClassName",Tuple.of(":[51c~([A-Z][a-z0-9]+)+]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(")));
            put("Arguments5",Tuple.of(":[a1],:[a2],:[a3],:[a4],:[a5]", s -> s.chars().filter(x->x ==',').count() >=4));
            put("Arguments4",Tuple.of(":[a1],:[a2],:[a3],:[a4]", s -> s.chars().filter(x->x ==',').count() >=3));
            put("Arguments3",Tuple.of(":[a1],:[a2],:[a3]", s -> s.chars().filter(x->x ==',').count() >=2));
            put("Arguments2",Tuple.of(":[a1],:[a2]", s -> s.chars().filter(x->x ==',').count() >=1));
            put("TypeArgs",Tuple.of("<:[tas]>", s -> s.startsWith("<")));
            put("Identifier",Tuple.of(":[[35]]", s -> !s.contains(" ") && !s.contains(",") && !s.contains("(")));
            put("Expression",Tuple.of("<:[exp:e]>", s -> true));
            put("Anything",Tuple.of(":[anything]", s -> true));


        }
    };

}
