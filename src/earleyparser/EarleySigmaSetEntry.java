package earleyparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EarleySigmaSetEntry {

    private CursorGrammarRule cursorGrammarRule;
    private int tag;
    private ArrayList<EarleySigmaSetEntry> precedingEntries;

    public EarleySigmaSetEntry(CursorGrammarRule r, int t) {
        cursorGrammarRule = r;
        tag = t;
        precedingEntries = new ArrayList<>();
    }

    public EarleySigmaSetEntry(CursorGrammarRule r, int t, EarleySigmaSetEntry e) {
        cursorGrammarRule = r;
        tag = t;
        precedingEntries = new ArrayList<>();
        precedingEntries.add(e);
    }

    public CursorGrammarRule getCursorGrammarRule() {
        return cursorGrammarRule;
    }

    public int getTag() {
        return tag;
    }

    public void addPrecedingEntry(EarleySigmaSetEntry precedingEntry) {
        precedingEntries.add(precedingEntry);
    }

    public List<EarleySigmaSetEntry> getPrecedingEntries() {
        return precedingEntries;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof EarleySigmaSetEntry)) {
            return false;
        }
        EarleySigmaSetEntry otherEntry = (EarleySigmaSetEntry) other;
        return otherEntry.cursorGrammarRule.equals(cursorGrammarRule) &&
                otherEntry.tag == tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursorGrammarRule, tag);
    }

    // Returns just the cursor grammar rule and the tag, NOT the
    // preceding entries
    public String entryString() {
        return "<" + cursorGrammarRule + ", " + tag + ">";
    }

    @Override
    public String toString() {
        String precedingEntriesString = "(";
        if(precedingEntries.size() > 0) {
            precedingEntriesString += precedingEntries.get(0).entryString();
        }
        for(int i = 1; i < precedingEntries.size(); i++) {
            precedingEntriesString += ", " + precedingEntries.get(i).entryString();
        }
        precedingEntriesString += ")";

        return "<" + cursorGrammarRule + ", " + tag + "> - " + precedingEntriesString;
    }
}
