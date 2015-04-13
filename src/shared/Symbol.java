package shared;

import java.util.Objects;

public class Symbol {

    private String name;
    private String pattern;

    public Symbol(String n, String p) {
        name = n;
        pattern = p;
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof Symbol)) {
            return false;
        }
        Symbol otherSymbol = (Symbol) other;
        // TODO: should I check the pattern here too?
        return name.equals(otherSymbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pattern);
    }

    @Override
    public String toString() {
        return name;
    }
}
