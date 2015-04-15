package gfgparser;

import shared.Nonterminal;
import shared.Symbol;
import shared.Terminal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GFGSigmaSet {

    // Occasionally, we want to see if there is an entry in this sigma set that
    // represents a specific GFGNode and tag, but we don't have a reference to
    // it. To allow for that, when two GFGSigmaSetEntry's are hashed and/or
    // compared for equality, only their GFGNode and tag are considered (not
    // their precedingEntries). This allows the kind of lookup we want.
    // Other times, we want to quickly get at a GFGSigmaSetEntry instance in
    // this set, so we can look at its precedingEntries. So instead of simply
    // keeping a HashSet of GFGSigmaSetEntries, we keep a map, and map each
    // entry to itself. Then, to quickly get at a specific element, we just
    // look it up in the map (which, again, will ignore the precedingEntries)
    // and return it.
    private HashMap<GFGSigmaSetEntry, GFGSigmaSetEntry> allEntries;
    private HashMap<Symbol, HashSet<GFGSigmaSetEntry>> entriesPrecedingSymbol;
    private HashMap<Nonterminal, HashSet<GFGSigmaSetEntry>> entriesPrecedingNonterminal;

    public GFGSigmaSet() {
        allEntries = new HashMap<>();
        entriesPrecedingSymbol = new HashMap<>();
        entriesPrecedingNonterminal = new HashMap<>();
    }

    public void add(GFGSigmaSetEntry entry) {
        allEntries.put(entry, entry);
        GFGNode entryNode = entry.getNode();
        if(entryNode instanceof InnerGFGNode) {
            InnerGFGNode innerEntryNode = (InnerGFGNode) entryNode;
            Terminal transition = innerEntryNode.getTransition();
            if(transition != null) {
                Symbol transitionSymbol = transition.getSymbol();
                ensurePrecedingSymbolSet(transitionSymbol);
                entriesPrecedingSymbol.get(transitionSymbol).add(entry);
            } else {
                // Just because the transition was not an epsilon transition
                // doesn't necessarily mean this entry precedes a nonterminal;
                // it could be an exit node
                GFGNode nextNode = innerEntryNode.getNextNode();
                if(nextNode instanceof StartGFGNode) {
                    StartGFGNode startNextNode = (StartGFGNode) nextNode;
                    Nonterminal nextNonterminal = startNextNode.getNonterminal();
                    ensurePrecedingNonterminalSet(nextNonterminal);
                    entriesPrecedingNonterminal.get(nextNonterminal).add(entry);
                }
            }
        }
    }

    public boolean contains(GFGSigmaSetEntry entry) {
        return allEntries.containsKey(entry);
    }

    public GFGSigmaSetEntry get(GFGSigmaSetEntry entry) {
        return allEntries.get(entry);
    }

    public Set<GFGSigmaSetEntry> getEntriesPrecedingSymbol(Symbol symbol) {
        ensurePrecedingSymbolSet(symbol);
        return entriesPrecedingSymbol.get(symbol);
    }

    public Set<GFGSigmaSetEntry> getEntriesPrecedingNonterminal(Nonterminal nonterminal) {
        ensurePrecedingNonterminalSet(nonterminal);
        return entriesPrecedingNonterminal.get(nonterminal);
    }

    private void ensurePrecedingSymbolSet(Symbol symbol) {
        HashSet<GFGSigmaSetEntry> listForTerminal = entriesPrecedingSymbol.get(symbol);
        if(listForTerminal == null) {
            listForTerminal = new HashSet<>();
            entriesPrecedingSymbol.put(symbol, listForTerminal);
        }
    }

    private void ensurePrecedingNonterminalSet(Nonterminal nonterminal) {
        HashSet<GFGSigmaSetEntry> listForNonterminal = entriesPrecedingNonterminal.get(nonterminal);
        if(listForNonterminal == null) {
            listForNonterminal = new HashSet<>();
            entriesPrecedingNonterminal.put(nonterminal, listForNonterminal);
        }
    }

    public String toString() {
        return allEntries.toString();
    }
}
