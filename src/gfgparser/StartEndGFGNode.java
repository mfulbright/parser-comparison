package gfgparser;

import shared.Nonterminal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StartEndGFGNode implements GFGNode {

    private Nonterminal nonterminal;
    private boolean isStartNode;
    private HashMap<CursorGrammarRule, GFGNode> nextNodes;

    public StartEndGFGNode(Nonterminal n, boolean i) {
        nonterminal = n;
        isStartNode = i;
        nextNodes = new HashMap<>();
    }

    public void addNextNode(CursorGrammarRule rule, GFGNode node) {
        nextNodes.put(rule, node);
    }

    public GFGNode getNextNodeForGrammarRule(CursorGrammarRule rule) {
        return nextNodes.get(rule);
    }

    public Map<CursorGrammarRule, GFGNode> getNextNodes() {
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
        if(! (other instanceof StartEndGFGNode)) {
            return false;
        }
        StartEndGFGNode otherNode = (StartEndGFGNode) other;
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
