package gfgparser;

import java.util.Objects;

public class OldRuleGFGNode implements OldGFGNode {

    private CursorGrammarRule cursorGrammarRule;
    private OldGFGNode nextNode;

    public OldRuleGFGNode(CursorGrammarRule cgr) {
        // leave nextNode null for now
        this(cgr, null);
    }

    public OldRuleGFGNode(CursorGrammarRule cgr, OldGFGNode next) {
        cursorGrammarRule = cgr;
        nextNode = next;
    }

    public OldGFGNode getNextNode() {
        return nextNode;
    }

    public CursorGrammarRule getCursorGrammarRule() {
        return cursorGrammarRule;
    }

    public void setNextNode(OldGFGNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public boolean isStartEndGFGNode() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof OldRuleGFGNode)) {
            return false;
        }
        OldRuleGFGNode otherNode = (OldRuleGFGNode) other;
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
