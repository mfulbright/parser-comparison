package shared;

public class EarleyParseTreeLeaf implements EarleyParseTreeNode {

    private EarleyParseTreeParent parent;
    private Token token;

    public EarleyParseTreeLeaf(EarleyParseTreeParent p, Token t) {
        parent = p;
        token = t;
    }

    public boolean isLeafNode() {
        return true;
    }

    @Override
    public String toString() {
        return token.toString();
    }
}
