package gfgparser;

import shared.GrammarRule;

public class CursorGrammarRule {

    private GrammarRule grammarRule;
    private int cursorIndex;

    public CursorGrammarRule(GrammarRule rule, int index) {
        grammarRule = rule;
        cursorIndex = index;
    }

    public GrammarRule getGrammarRule() {
        return grammarRule;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public CursorGrammarRule createNext() {
        return new CursorGrammarRule(grammarRule, cursorIndex + 1);
    }
}
