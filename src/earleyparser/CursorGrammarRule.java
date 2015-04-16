package earleyparser;

import shared.GrammarElement;
import shared.GrammarRule;

import java.util.ArrayList;
import java.util.Objects;

public class CursorGrammarRule {

    private GrammarRule grammarRule;
    private int cursorIndex;

    public CursorGrammarRule(GrammarRule rule, int index) {
        if(index < 0 || index > rule.getRightHandSide().size()) {
            throw new IllegalArgumentException("Cursor index must be in bounds");
        }
        grammarRule = rule;
        cursorIndex = index;
    }

    public CursorGrammarRule createNext() {
        return new CursorGrammarRule(grammarRule, cursorIndex + 1);
    }

    public GrammarRule getGrammarRule() {
        return grammarRule;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public boolean isCursorAtStart() {
        return cursorIndex == 0;
    }

    public boolean isCursorAtEnd() {
        return cursorIndex == grammarRule.getRightHandSide().size();
    }

    public GrammarElement getNextGrammarElement() {
        if(isCursorAtEnd()) {
            return null;
        }
        return grammarRule.getRightHandSide().get(cursorIndex);
    }

    public GrammarElement getPreviousGrammarElement() {
        if(isCursorAtStart()) {
            return null;
        }
        return grammarRule.getRightHandSide().get(cursorIndex - 1);
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof CursorGrammarRule)) {
            return false;
        }
        CursorGrammarRule otherRule = (CursorGrammarRule) other;
        return grammarRule.equals(otherRule.grammarRule) &&
                cursorIndex == otherRule.cursorIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grammarRule, cursorIndex);
    }

    @Override
    public String toString() {
       String rhs = "";
        ArrayList<GrammarElement> ruleRHS = grammarRule.getRightHandSide();
        for(int i = 0; i < cursorIndex; i++) {
            rhs += ruleRHS.get(i) + " ";
        }
        rhs += ". ";
        for(int i = cursorIndex; i < ruleRHS.size(); i++) {
            rhs += ruleRHS.get(i) + " ";
        }
        rhs = rhs.substring(0, rhs.length() - 1);

        return grammarRule.getLeftHandSide() + " -> " + rhs;
    }
}
