package gfgparser;

import shared.GrammarRule;
import shared.Nonterminal;

import java.util.HashMap;

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

    public GFGNode getNextNodeForGrammarRule(GrammarRule rule) {
        return nextNodes.get(rule);
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
}
