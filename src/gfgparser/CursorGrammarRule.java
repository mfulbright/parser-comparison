package gfgparser;

import shared.GrammarElement;
import shared.GrammarRule;

import java.util.Objects;

public class CursorGrammarRule {

    private GrammarRule grammarRule;
    private int cursorIndex;

    public CursorGrammarRule(GrammarRule rule, int index) {
        if(index < 0 || index > grammarRule.getRightHandSide().size()) {
            throw new IllegalArgumentException("Cursor index must be in bounds");
        }
        grammarRule = rule;
        cursorIndex = index;
    }

    public GrammarRule getGrammarRule() {
        return grammarRule;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public boolean cursorAtStart() {
        return cursorIndex == 0;
    }

    public boolean cursorAtEnd() {
        return cursorIndex == grammarRule.getRightHandSide().size();
    }

    public GrammarElement getNextGrammarElement() {
        if(cursorIndex == grammarRule.getRightHandSide().size()) {
            return null;
        }
        return grammarRule.getRightHandSide().get(cursorIndex);
    }

    public GrammarElement getPreviousGrammarElement() {
        if(cursorIndex == 0) {
            return null;
        }
        return grammarRule.getRightHandSide().get(cursorIndex - 1);
    }

    public CursorGrammarRule createNext() {
        return new CursorGrammarRule(grammarRule, cursorIndex + 1);
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
}
