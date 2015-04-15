package gfgparser;

import shared.Nonterminal;

import java.util.ArrayList;
import java.util.List;

public class StartGFGNode implements GFGNode {

    private Nonterminal nonterminal;
    private ArrayList<InnerGFGNode> nextNodes;

    public StartGFGNode(Nonterminal n) {
        nonterminal = n;
        nextNodes = new ArrayList<>();
    }

    public Nonterminal getNonterminal() {
        return nonterminal;
    }

    public List<InnerGFGNode> getNextNodes() {
        return nextNodes;
    }

    public void addNextNode(InnerGFGNode nextNode) {
        nextNodes.add(nextNode);
    }

    public String toString() {
        return "StartGFGNode: ." + nonterminal;
    }

    // Even though we hash these objects, we do not override hashCode
    // (and equals) here, as we ensure that each unique node is only
    // ever created once, and so using a hash code based on the
    // identity is appropriate.
}
