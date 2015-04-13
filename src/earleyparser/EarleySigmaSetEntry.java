package earleyparser;

import shared.GrammarElement;
import shared.GrammarRule;

import java.util.ArrayList;
import java.util.Objects;

public class EarleySigmaSetEntry {

    private GrammarRule rule;
    // this will be in the range [0, rule.getRightHandSide().size()]
    private int cursorIndex;
    private int tag;

    public EarleySigmaSetEntry(GrammarRule r, int cI, int t) {
        rule = r;
        cursorIndex = cI;
        tag = t;
    }

    public GrammarRule getGrammarRule() {
        return rule;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public int getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof EarleySigmaSetEntry)) {
            return false;
        }
        EarleySigmaSetEntry otherSigmaSetEntry = (EarleySigmaSetEntry) other;
        return rule.equals(otherSigmaSetEntry.rule) &&
                cursorIndex == otherSigmaSetEntry.cursorIndex &&
                tag == otherSigmaSetEntry.tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, cursorIndex, tag);
    }

    @Override
    public String toString() {
        String rhs = "";
        ArrayList<GrammarElement> ruleRHS = rule.getRightHandSide();
        for(int i = 0; i < cursorIndex; i++) {
            rhs += ruleRHS.get(i) + " ";
        }
        rhs += ". ";
        for(int i = cursorIndex; i < ruleRHS.size(); i++) {
            rhs += ruleRHS.get(i) + " ";
        }
        rhs = rhs.substring(0, rhs.length() - 1);

        return "<" + rule.getLeftHandSide() + " -> " + rhs + ", " + tag + ">";
    }
}
