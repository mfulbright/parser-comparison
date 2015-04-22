package shared;

import java.util.Objects;

public class Token {

    private String text;
    private Symbol type;

    public Token(String te, Symbol ty) {
        text = te;
        type = ty;
    }

    public String getText() {
        return text;
    }

    public Symbol getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof Token)) {
            return false;
        }
        Token otherToken = (Token) other;
        return otherToken.text.equals(text) &&
                otherToken.type.equals(type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, type);
    }

    @Override
    public String toString() {
        return "<" + type + ": " + text + ">";
    }
}
