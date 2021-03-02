package type.change;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Compute connected components.
 *
 * @author SPG-04
 *
 */
public class ConnctedComponent {

    /**
     * Visited edit operations. Required to compute connected components (DFS
     * implementation).
     */
    private HashMap<Action, Integer> visited;

    /**
     * Edit operations graph.
     */
    private HashMap<Action, List<Action>> graph;

//    private ConnectionStrategy connectionComparer;

    /**
     * Compute connected components.
     */
    private List<List<Action>> computeConnectedComponents(final List<Action> editOperations) {
        visited = new HashMap<>();
        int i = 0;
        final HashMap<Integer, List<Action>> dic = new HashMap<>();
        for (final Action edit : editOperations) {
            final Action t = edit;
            if (!visited.containsKey(t)) {
                dic.put(i, new ArrayList<Action>());
                depthFirstSearch(edit, i++);
            }
        }
        for (final Action edit : editOperations) {
            final Action t = edit;
            final int cc = visited.get(t);
            dic.get(cc).add(edit);
        }
        return new ArrayList<>(dic.values());
    }

    /**
     * Depth first search.
     *
     * @param editOperation edit operation
     * @param i index of he edit operation
     */
    private void depthFirstSearch(final Action editOperation, final int i) {
        final Action t = editOperation;
        visited.put(t, i);
        for (final Action edit : graph.get(t)) {
            final Action te = edit;
            if (!visited.containsKey(te)) {
                depthFirstSearch(edit, i);
            }
        }
    }

    /**
     * Computes connected components.
     * @param editOperations edit operations.
     * @return connected components
     */
    public List<List<Action>> connectedComponents(final List<Action> editOperations) {
        buildDigraph(editOperations);
        return computeConnectedComponents(editOperations);
    }

    /**
     * Build a directed graph of the transformation. An edition i is connected to
     * edition j if edit j depends that edit i insert a node in the tree.
     *
     * @param script list of Actions
     */
    private void buildDigraph(final List<Action> script) {
        graph = new HashMap<>();
        for (final Action edit : script) {
            final Action t = edit;
            graph.put(t, new ArrayList<Action>());
        }
        for (int i = 0; i < script.size(); i++) {
            final Action editI = script.get(i);

            for (int j = 0; j < script.size(); j++) {
                if (i == j) {
                    continue;
                }
                final Action editJ = script.get(j);

                if (isConnected(script.get(i), script.get(j))) {
                    graph.get(editI).add(editJ);
                    graph.get(editJ).add(editI);
                }
            }
        }
    }

    public static boolean isConnected(final Action editi , final Action editj) {

        ITree parenti = null;
        ITree parentj = null;
        if (editi instanceof Insert) {
            final Insert insert = (Insert) editi;
            parenti = insert.getParent();
        }
        if (editi instanceof Move) {
            final Move move = (Move) editi;
            parenti = move.getParent();
            return false;
        }
        if (editi instanceof Delete) {
            final Delete delete = (Delete) editi;
            parenti = delete.getNode().getParent();
        }
        if (editi instanceof Update) {
            final Update update = (Update) editi;
            parenti = update.getNode().getParent();
        }

        if (editj instanceof Insert) {
            final Insert insert = (Insert) editj;
            parentj = insert.getParent();
        }
        if (editj instanceof Move) {
            final Move move = (Move) editj;
            parentj = move.getParent();
        }
        if (editj instanceof Delete) {
            final Delete delete = (Delete) editj;
            parentj = delete.getNode().getParent();
        }
        if (editj instanceof Update) {
            final Update update = (Update) editj;
            parentj = update.getNode().getParent();
        }
        if (parenti != null && parentj != null) {

            if (parenti.equals(parentj) && !parenti.getType().name.equals("COMPILATION_UNIT")) {
                return true;
            }
        }
        return parentj != null && parentj.equals(editi.getNode());
    }

}