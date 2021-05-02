package type.change.visitors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.List;

public class ExpressionOfType extends ASTVisitor {

    private final String typeName;

    public final List<Expression> relevantExpressions;

    public ExpressionOfType(String typeName){
        this.relevantExpressions = new ArrayList<>();
        this.typeName = typeName;
    }
    @Override
    public void postVisit(ASTNode node) {
        if(node instanceof Expression){
            Expression ex = (Expression) node;
            ITypeBinding t = ex.resolveTypeBinding();
            if(t != null && typeName.equals(t.getBinaryName())){
                relevantExpressions.add(ex);
            }
        }
        super.postVisit(node);
    }
}
