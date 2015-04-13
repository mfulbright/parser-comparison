package gfgparser;

import java.util.Objects;

public class RuleGFGNode implements GFGNode {

    private CursorGrammarRule cursorGrammarRule;
    private GFGNode nextNode;

    public RuleGFGNode(CursorGrammarRule cgr) {
        // leave nextNode null for now
        this(cgr, null);
    }

    public RuleGFGNode(CursorGrammarRule cgr, GFGNode next) {
        cursorGrammarRule = cgr;
        nextNode = next;
    }

    public GFGNode getNextNode() {
        return nextNode;
    }

    public CursorGrammarRule getCursorGrammarRule() {
        return cursorGrammarRule;
    }

    public void setNextNode(GFGNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public boolean isStartEndGFGNode() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof RuleGFGNode)) {
            return false;
        }
        RuleGFGNode otherNode = (RuleGFGNode) other;
        return cursorGrammarRule.equals(otherNode.cursorGrammarRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursorGrammarRule);
    }

    @Override
    public String toString() {
        return cursorGrammarRule.toString();
    }
}
