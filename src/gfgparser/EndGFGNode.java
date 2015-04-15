package gfgparser;

import shared.Nonterminal;

import java.util.HashMap;

public class EndGFGNode implements GFGNode {

    private Nonterminal nonterminal;
    private HashMap<InnerGFGNode, InnerGFGNode> callNodesToReturnNodes;
    // This is used during the parsing phase, when we're working backwards
    // through the GFG path using the call stack
    private HashMap<InnerGFGNode, InnerGFGNode> returnNodesToCallNodes;

    public EndGFGNode(Nonterminal n) {
        nonterminal = n;
        callNodesToReturnNodes = new HashMap<>();
        returnNodesToCallNodes = new HashMap<>();
    }

    public Nonterminal getNonterminal() {
        return nonterminal;
    }

    public void mapNodes(InnerGFGNode callNode, InnerGFGNode returnNode) {
        callNodesToReturnNodes.put(callNode, returnNode);
        returnNodesToCallNodes.put(returnNode, callNode);
    }

    public InnerGFGNode getCallNode(InnerGFGNode returnNode) {
        return returnNodesToCallNodes.get(returnNode);
    }

    public InnerGFGNode getReturnNode(InnerGFGNode callNode) {
        return callNodesToReturnNodes.get(callNode);
    }

    public String toString() {
        return "EndGFGNode: " + nonterminal + ".";
    }

    // Even though we hash these objects, we do not override hashCode
    // (and equals) here, as we ensure that each unique node is only
    // ever created once, and so using a hash code based on the
    // identity is appropriate.
}
