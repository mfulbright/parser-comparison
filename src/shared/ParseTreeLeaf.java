package shared;

public class ParseTreeLeaf implements ParseTreeNode {

    // temp
    private static int idcount = 0;
    private String id;

    private ParseTreeParent parent;
    private Token token;

    public ParseTreeLeaf(ParseTreeParent p, Token t) {
        id = "L" + idcount++;
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
        return token.toString() + " (" + id + ")";
    }
}
