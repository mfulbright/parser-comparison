package earleyparser;

import shared.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EarleyParser implements Parser{

    private Grammar grammar;

    public EarleyParser(Grammar g) {
        grammar = g;
    }

    @Override
    public void setGrammar(Grammar g) {
        grammar = g;
    }

    @Override
    public ParseTreeNode parse(List<Token> tokens) {
        // Keep a list of sigma sets. In this list, index j will correspond to
        // the sigma set right before the jth token
        ArrayList<EarleySigmaSet> sigmaSets = new ArrayList<>();

        // Set up the first sigma set
        EarleySigmaSet sigmaSet0 = new EarleySigmaSet();
        sigmaSets.add(sigmaSet0);
        ArrayDeque<EarleySigmaSetEntry> sigmaSet0ToProcess = new ArrayDeque<>();
        GrammarRule startRule = grammar.getStartRule();
        CursorGrammarRule startCursorRule = new CursorGrammarRule(startRule, 0);
        EarleySigmaSetEntry startRuleEntry = new EarleySigmaSetEntry(startCursorRule, 0);
        sigmaSet0ToProcess.add(startRuleEntry);
        fillSigmaSet(sigmaSets, 0, sigmaSet0ToProcess);

        // Process the input
        for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token currentToken = tokens.get(tokenIndex);
            EarleySigmaSet previousSigmaSet = sigmaSets.get(tokenIndex);
            EarleySigmaSet nextSigmaSet = new EarleySigmaSet();
            sigmaSets.add(nextSigmaSet);
            ArrayDeque<EarleySigmaSetEntry> toProcess = new ArrayDeque<>();
            Set<EarleySigmaSetEntry> scanableEntries = previousSigmaSet.getEntriesPrecedingSymbol(currentToken.getType());
            for(EarleySigmaSetEntry scanableEntry : scanableEntries) {
                EarleySigmaSetEntry newEntry = new EarleySigmaSetEntry(
                        scanableEntry.getCursorGrammarRule().createNext(),
                        scanableEntry.getTag(),
                        scanableEntry);
                toProcess.add(newEntry);
            }

            fillSigmaSet(sigmaSets, tokenIndex + 1, toProcess);
        }

        CursorGrammarRule acceptingCursorRule = new CursorGrammarRule(startRule, startRule.getRightHandSide().size());
        EarleySigmaSetEntry acceptingSigmaSetEntry = new EarleySigmaSetEntry(acceptingCursorRule, 0);
        if (! sigmaSets.get(tokens.size()).contains(acceptingSigmaSetEntry)) {
            return null;
        } else {
            return new ParseTreeParent(null, null);
        }
        /*

        // The recognizing was successful - rebuild the parse tree
        EarleyParseTreeParent currentParseTreeParent = new EarleyParseTreeParent(null, startRule);
        int currentSigmaSetIndex = tokens.size();
        HashSet<OldEarleySigmaSetEntry> currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        OldEarleySigmaSetEntry currentSigmaSetEntry = null;
        for(OldEarleySigmaSetEntry possible : currentSigmaSet) {
            if(possible.equals(acceptingSigmaSetEntry)) {
                currentSigmaSetEntry = possible;
                break;
            }
        }
        Stack<OldEarleySigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            int cursorIndex = currentSigmaSetEntry.getCursorIndex();
            if(cursorIndex > 0) {
                // we can keep working backwards through the current rule
                // look at what the grammar element is immediately before the cursor
                GrammarElement previousElement = currentSigmaSetEntry.getGrammarRule().getRightHandSide().get(cursorIndex - 1);
                if(previousElement.isNonterminal()) {
                    // Reverse the end/exit rule
                    // This means I need to look in my current sigma set for an entry where this nonterminal is finished.
                    // That is, an entry where the left hand side of the grammar rule is this nonterminal, and its cursor is
                    // at the end of the right hand side
                    // We call this "lowerSigmaSetEntry" because it's the entry that takes us lower in the parse tree
                    OldEarleySigmaSetEntry lowerSigmaSetEntry = null;
                    for(OldEarleySigmaSetEntry possible : currentSigmaSet) {
                        // First, the nonterminal on the left side of the grammar rule must be the same as the element we're trying to match
                        if(! possible.getGrammarRule().getLeftHandSide().equals(previousElement)) {
                            continue;
                        }
                        // Second, it must have its cursor all the way at the end of the right hand side
                        if(possible.getCursorIndex() < possible.getGrammarRule().getRightHandSide().size()) {
                            continue;
                        }
                        // Third, we must look in the sigma set represented by its tag, and there must be an entry identical
                        // to our current one, except the cursor is to the left of the nonterminal in question, not to the right of it
                        int possibleTag = possible.getTag();
                        HashSet<OldEarleySigmaSetEntry> possibleTagSigmaSet = sigmaSets.get(possibleTag);
                        OldEarleySigmaSetEntry necessarySigmaSetEntry = new OldEarleySigmaSetEntry(currentSigmaSetEntry.getGrammarRule(), currentSigmaSetEntry.getCursorIndex() - 1, currentSigmaSetEntry.getTag());
                        if(! possibleTagSigmaSet.contains(necessarySigmaSetEntry)) {
                            continue;
                        }
                        // this is the right entry
                        // for an ambiguous string, there might be multiple entries in this set that fit this criterion. this
                        // is where we would look for those
                        lowerSigmaSetEntry = possible;
                        break;
                    }
                    EarleyParseTreeParent previousElementParent = new EarleyParseTreeParent(currentParseTreeParent, lowerSigmaSetEntry.getGrammarRule());
                    currentParseTreeParent.getChildren().add(0, previousElementParent);
                    currentParseTreeParent = previousElementParent;
                    callStack.push(currentSigmaSetEntry);
                    currentSigmaSetEntry = lowerSigmaSetEntry;
                } else {
                    // Reverse the scan rule
                    Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                    EarleyParseTreeLeaf terminalNode = new EarleyParseTreeLeaf(currentParseTreeParent, scannedToken);
                    currentParseTreeParent.getChildren().add(0, terminalNode);
                    // Find the previous sigma set entry
                    currentSigmaSetIndex--;
                    currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
                    for(OldEarleySigmaSetEntry possible : currentSigmaSet) {
                        if(possible.getGrammarRule().equals(currentSigmaSetEntry.getGrammarRule()) &&
                                possible.getTag() == currentSigmaSetEntry.getTag() &&
                                possible.getCursorIndex() == currentSigmaSetEntry.getCursorIndex() - 1) {
                            currentSigmaSetEntry = possible;
                            break;
                        }
                    }
                }
            } else {
                // Reverse the call/start rule
                // This is actually quite simple; I go up one parent in the parse tree, and I pop an entry off
                // the stack and decrement its cursor index by 1
                currentParseTreeParent = currentParseTreeParent.getParent();
                OldEarleySigmaSetEntry poppedEntry = callStack.pop();
                currentSigmaSetEntry = new OldEarleySigmaSetEntry(poppedEntry.getGrammarRule(), poppedEntry.getCursorIndex() - 1, poppedEntry.getTag());
            }
        }

        return currentParseTreeParent;
        */
    }

    private void fillSigmaSet(ArrayList<EarleySigmaSet> sigmaSets,
                              int currentSigmaSetIndex,
                              ArrayDeque<EarleySigmaSetEntry> toProcess) {
        EarleySigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        while(! toProcess.isEmpty()) {
            EarleySigmaSetEntry processing = toProcess.remove();
            if (currentSigmaSet.contains(processing)) {
                // I think this might be wrong for parse forests
                continue;
            }
            currentSigmaSet.add(processing);

            CursorGrammarRule processingCursorRule = processing.getCursorGrammarRule();
            if(processingCursorRule.isCursorAtEnd()) {
                // This is the Exit & End step
                // Look for calling entries in the appropriate sigma set
                Nonterminal endingNonterminal = processingCursorRule.getGrammarRule().getLeftHandSide();
                int endingTag = processing.getTag();
                EarleySigmaSet callingSigmaSet = sigmaSets.get(endingTag);
                Set<EarleySigmaSetEntry> callingEntries = callingSigmaSet.getEntriesPrecedingNonterminal(endingNonterminal);
                for(EarleySigmaSetEntry callingEntry : callingEntries) {
                    CursorGrammarRule callingCursorRule = callingEntry.getCursorGrammarRule();
                    CursorGrammarRule nextCursorRule = callingCursorRule.createNext();
                    int callingEntryTag = callingEntry.getTag();
                    EarleySigmaSetEntry newEntry = new EarleySigmaSetEntry(nextCursorRule, callingEntryTag, processing);
                    toProcess.add(newEntry);
                }
            } else {
                GrammarElement nextElement = processingCursorRule.getNextGrammarElement();
                if(nextElement instanceof Nonterminal) {
                    // This is the Call & Start step
                    Nonterminal nextNonterminal = (Nonterminal) nextElement;
                    List<GrammarRule> nextNonterminalRules = grammar.getRulesWithLeftHandSide(nextNonterminal);
                    for(GrammarRule rule : nextNonterminalRules) {
                        CursorGrammarRule cursorRule = new CursorGrammarRule(rule, 0);
                        EarleySigmaSetEntry newEntry = new EarleySigmaSetEntry(cursorRule, currentSigmaSetIndex, processing);
                        toProcess.add(newEntry);
                    }
                } else {
                    // This is the Scan step
                    // We don't scan in this method, so nothing happens here
                }
            }
        }
    }
}
