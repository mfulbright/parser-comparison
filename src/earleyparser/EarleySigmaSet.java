package earleyparser;

import shared.GrammarElement;
import shared.Nonterminal;
import shared.Symbol;
import shared.Terminal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class EarleySigmaSet {

    // Occasionally, we want to see if there is an entry in this sigma set that
    // represents a specific rule, cursor, and tag, but we don't have a reference to
    // it. To allow for that, when two EarleySigmaSetEntry's are hashed and/or
    // compared for equality, only their rule, cursor, and tag are considered (not
    // their precedingEntries). This allows the kind of lookup we want.
    // Other times, we want to quickly get at an EarleySigmaSetEntry instance in
    // this set, so we can look at its precedingEntries. So instead of simply
    // keeping a HashSet of EarleySigmaSetEntry's, we keep a map, and map each
    // entry to itself. Then, to quickly get at a specific element, we just
    // look it up in the map (which, again, will ignore the precedingEntries)
    // and return it.
    private HashMap<EarleySigmaSetEntry, EarleySigmaSetEntry> allEntries;
    private HashMap<Symbol, HashSet<EarleySigmaSetEntry>> entriesPrecedingSymbol;
    private HashMap<Nonterminal, HashSet<EarleySigmaSetEntry>> entriesPrecedingNonterminal;

    public EarleySigmaSet() {
        allEntries = new HashMap<>();
        entriesPrecedingSymbol = new HashMap<>();
        entriesPrecedingNonterminal = new HashMap<>();
    }

    public void add(EarleySigmaSetEntry entry) {
        allEntries.put(entry, entry);
        CursorGrammarRule cursorRule = entry.getCursorGrammarRule();
        if(cursorRule.isCursorAtEnd()) {
            return;
        }
        GrammarElement nextElement = cursorRule.getNextGrammarElement();
        if(nextElement instanceof Terminal) {
            Symbol nextSymbol = ((Terminal) nextElement).getSymbol();
            ensurePrecedingSymbolSet(nextSymbol);
            entriesPrecedingSymbol.get(nextSymbol).add(entry);
        } else { // nextElement instanceof Nonterminal
            Nonterminal nextNonterminal = (Nonterminal) nextElement;
            ensurePrecedingNonterminalSet(nextNonterminal);
            entriesPrecedingNonterminal.get(nextNonterminal).add(entry);
        }
    }

    public boolean contains(EarleySigmaSetEntry entry) {
        return allEntries.containsKey(entry);
    }

    public EarleySigmaSetEntry get(EarleySigmaSetEntry entry) {
        return allEntries.get(entry);
    }

    public Set<EarleySigmaSetEntry> getEntriesPrecedingSymbol(Symbol symbol) {
        ensurePrecedingSymbolSet(symbol);
        return entriesPrecedingSymbol.get(symbol);
    }

    public Set<EarleySigmaSetEntry> getEntriesPrecedingNonterminal(Nonterminal nonterminal) {
        ensurePrecedingNonterminalSet(nonterminal);
        return entriesPrecedingNonterminal.get(nonterminal);
    }

    private void ensurePrecedingSymbolSet(Symbol symbol) {
        HashSet<EarleySigmaSetEntry> listForTerminal = entriesPrecedingSymbol.get(symbol);
        if(listForTerminal == null) {
            listForTerminal = new HashSet<>();
            entriesPrecedingSymbol.put(symbol, listForTerminal);
        }
    }

    private void ensurePrecedingNonterminalSet(Nonterminal nonterminal) {
        HashSet<EarleySigmaSetEntry> listForNonterminal = entriesPrecedingNonterminal.get(nonterminal);
        if(listForNonterminal == null) {
            listForNonterminal = new HashSet<>();
            entriesPrecedingNonterminal.put(nonterminal, listForNonterminal);
        }
    }
}
