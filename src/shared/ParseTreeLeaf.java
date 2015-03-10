package shared;

public class ParseTreeLeaf implements ParseTreeNode {

    private ParseTreeParent parent;
    private Token token;

    public ParseTreeLeaf(ParseTreeParent p, Token t) {
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
