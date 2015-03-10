package shared;

import java.util.ArrayList;

public class GrammarRule {

    private Nonterminal leftHandSide;
    private ArrayList<GrammarElement> rightHandSide;

    public GrammarRule(Nonterminal left) {
        this(left, new ArrayList<GrammarElement>());
    }

    public GrammarRule(Nonterminal left, ArrayList<GrammarElement> right) {
        leftHandSide = left;
        rightHandSide = right;
    }

    public Nonterminal getLeftHandSide() {
        return leftHandSide;
    }

    public ArrayList<GrammarElement> getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof GrammarRule)) {
            return false;
        }
        GrammarRule otherGrammarRule = (GrammarRule) other;
        return leftHandSide.equals(otherGrammarRule.leftHandSide) &&
                rightHandSide.equals(otherGrammarRule.rightHandSide);
    }

    @Override
    public int hashCode() {
        int hash = leftHandSide.hashCode();
        hash *= 17;
        hash += rightHandSide.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        String rhs = "";
        for(GrammarElement gE : rightHandSide) {
            rhs += gE + " ";
        }
        rhs = rhs.substring(0, rhs.length() - 1);
        return leftHandSide + " -> " + rhs;
    }
}
