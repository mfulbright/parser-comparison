package shared;

public class Nonterminal implements GrammarElement {

    private String name;

    public Nonterminal(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public boolean isNonterminal() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof Nonterminal)) {
            return false;
        }
        Nonterminal otherNonterminal = (Nonterminal) other;
        return name.equals(otherNonterminal.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
