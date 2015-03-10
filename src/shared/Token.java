package shared;

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
    public String toString() {
        return "<" + type + ": " + text + ">";
    }

    // TODO: Should probably override equals and hashCode here
}
