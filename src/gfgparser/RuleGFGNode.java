package gfgparser;

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
}
