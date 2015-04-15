package gfgparser;

import shared.Nonterminal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OldStartEndGFGNode implements OldGFGNode {

    private Nonterminal nonterminal;
    private boolean isStartNode;
    private HashMap<CursorGrammarRule, OldGFGNode> nextNodes;

    public OldStartEndGFGNode(Nonterminal n, boolean i) {
        nonterminal = n;
        isStartNode = i;
        nextNodes = new HashMap<>();
    }

    public void addNextNode(CursorGrammarRule rule, OldGFGNode node) {
        nextNodes.put(rule, node);
    }

    public OldGFGNode getNextNodeForGrammarRule(CursorGrammarRule rule) {
        return nextNodes.get(rule);
    }

    public Map<CursorGrammarRule, OldGFGNode> getNextNodes() {
        return nextNodes;
    }

    public Nonterminal getNonterminal() {
        return nonterminal;
    }

    public boolean isStartNode() {
        return isStartNode;
    }

    @Override
    public boolean isStartEndGFGNode() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof OldStartEndGFGNode)) {
            return false;
        }
        OldStartEndGFGNode otherNode = (OldStartEndGFGNode) other;
        return nonterminal.equals(otherNode.nonterminal) &&
                isStartNode == otherNode.isStartNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonterminal, isStartNode);
    }

    @Override
    public String toString() {
        if(isStartNode) {
            return "." + nonterminal.toString();
        } else {
            return nonterminal.toString() + ".";
        }
    }
}
