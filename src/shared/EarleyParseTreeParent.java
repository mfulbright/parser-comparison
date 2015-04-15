package shared;

import java.util.ArrayList;

public class EarleyParseTreeParent implements EarleyParseTreeNode {

    private EarleyParseTreeParent parent;
    private GrammarRule grammarRule;
    private ArrayList<EarleyParseTreeNode> children;

    public EarleyParseTreeParent(EarleyParseTreeParent p, GrammarRule g) {
        parent = p;
        grammarRule = g;
        children = new ArrayList<EarleyParseTreeNode>();
    }

    public boolean isLeafNode() {
        return false;
    }

    public EarleyParseTreeParent getParent() {
        return parent;
    }

    public ArrayList<EarleyParseTreeNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return grammarRule.toString();
    }
}
