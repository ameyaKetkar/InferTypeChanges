package type.change.visitors;

import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class LowerCaseIdentifiers extends ASTVisitor {
    public Set<String> identifiers = new HashSet<>();
    public Set<String> methodNames = new HashSet<>();
    public Set<String> stringLiterals = new HashSet<>();
    public Set<String> numberLiterals = new HashSet<>();

    @Override
    public boolean visit(SimpleName node) {
        if(Character.isLowerCase(node.getIdentifier().charAt(0)))
            identifiers.add(node.getIdentifier());
        else if(node.getIdentifier().startsWith("STR")){
            identifiers.add(node.getIdentifier());
        }
        else if(Pattern.matches("[A-Z0-9]+(?:_[A-Z0-9]+)*]", node.getIdentifier())){
            identifiers.add(node.getIdentifier());
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NumberLiteral node) {
        String tokenValue = node.getToken();
        if (!tokenValue.equals("0") && !tokenValue.equals("0.0") && !tokenValue.equals("-1") ) {
            List<String> suffixes = List.of("f", "F", "l", "L");
            if (suffixes.stream().noneMatch(tokenValue::endsWith))
                numberLiterals.add(tokenValue);
            else
                numberLiterals.add(suffixes.stream().reduce(tokenValue, (a, b) -> a.replace(b, "")));
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(ExpressionMethodReference node) {
        methodNames.add(node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        methodNames.add(node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        methodNames.add(node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(CreationReference node) {
        methodNames.add(node.getType().toString());
        return super.visit(node);
    }
    @Override
    public boolean visit(ClassInstanceCreation node) {
        methodNames.add(node.getType().toString());
        return super.visit(node);
    }
    @Override
    public boolean visit(MethodInvocation node) {
        methodNames.add(node.getName().getIdentifier());
        return super.visit(node);
    }
}
