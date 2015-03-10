package shared;

import java.util.ArrayList;

public class ParseTreeParent implements ParseTreeNode {

    private ParseTreeParent parent;
    private GrammarRule grammarRule;
    private ArrayList<ParseTreeNode> children;

    public ParseTreeParent(ParseTreeParent p, GrammarRule g) {
        parent = p;
        grammarRule = g;
        children = new ArrayList<ParseTreeNode>();
    }

    public boolean isLeafNode() {
        return false;
    }

    public ParseTreeParent getParent() {
        return parent;
    }

    public ArrayList<ParseTreeNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return grammarRule.toString();
    }
}
