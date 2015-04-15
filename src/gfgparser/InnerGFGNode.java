package gfgparser;

import shared.Terminal;

public class InnerGFGNode implements GFGNode {

    // Perhaps dangerously, we will use null to represent an epsilon transition
    private Terminal transition;
    private GFGNode nextNode;

    public InnerGFGNode() {
        // When we make an InnerGFGNode, we haven't created the next node, or
        // considered what the transition will be. So we have an empty
        // constructor.
    }

    public void setTransition(Terminal tran) {
        transition = tran;
    }

    public Terminal getTransition() {
        return transition;
    }

    public void setNextNode(GFGNode next) {
        nextNode = next;
    }

    public GFGNode getNextNode() {
        return nextNode;
    }

    public String toString() {
        return "InnerGFGNode: " + (transition == null ? "\u03B5" : transition);
    }

    // Even though we hash these objects, we do not override hashCode
    // (and equals) here, as we ensure that each unique node is only
    // ever created once, and so using a hash code based on the
    // identity is appropriate.
}
