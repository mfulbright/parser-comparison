package shared;

public class Terminal implements GrammarElement {

    private Symbol symbol;

    public Terminal(Symbol s) {
        symbol = s;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public boolean isNonterminal() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof Terminal)) {
            return false;
        }
        Terminal otherTerminal = (Terminal) other;
        return symbol == otherTerminal.symbol;
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public String toString() {
        return symbol.toString();
    }
}
