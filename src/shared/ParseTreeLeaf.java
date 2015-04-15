package shared;

public class ParseTreeLeaf implements ParseTreeNode {

    private ParseTreeParent parent;
    private Token token;

    public ParseTreeLeaf(ParseTreeParent p, Token t) {
        parent = p;
        token = t;
    }

    public ParseTreeParent getParent() {
        return parent;
    }

    public Token getToken() {
        return token;
    }

    public Symbol getSymbol() {
        return token.getType();
    }

    @Override
    public String toString() {
        return token.toString();
    }
}
