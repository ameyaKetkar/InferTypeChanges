package type.change.visitors;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class NodeCounter extends ASTVisitor {

    private int count = 0;

    @Override
    public void postVisit(ASTNode node) {
        count ++;
    }

    public int getCount() {
        return count;
    }
}
