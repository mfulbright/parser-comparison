package shared;

import java.util.Objects;

public class ParseTreeLeaf implements ParseTreeNode {

    private Token token;

    public ParseTreeLeaf(Token t) {
        token = t;
    }

    public Token getToken() {
        return token;
    }

    public Symbol getSymbol() {
        return token.getType();
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof ParseTreeLeaf)) {
            return false;
        }
        ParseTreeLeaf otherLeaf = (ParseTreeLeaf) other;
        return otherLeaf.token.equals(token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String toString() {
        return token.toString();
    }
}
