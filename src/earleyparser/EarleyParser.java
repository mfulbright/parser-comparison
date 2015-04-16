package earleyparser;

import shared.*;

import java.util.*;

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
        }

        // The recognizing was successful - rebuild the parse tree
        ParseTreeParent currentParseTreeParent = new ParseTreeParent(null, startRule.getLeftHandSide());
        int currentSigmaSetIndex = tokens.size();
        EarleySigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        EarleySigmaSetEntry currentSigmaSetEntry = currentSigmaSet.get(acceptingSigmaSetEntry);
        Stack<EarleySigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            // System.out.println("Starting to process: " + currentSigmaSetEntry);
            CursorGrammarRule currentCursorRule = currentSigmaSetEntry.getCursorGrammarRule();
            if(currentCursorRule.isCursorAtStart()) {
                // Reverse the Call & Start step
                // We're done adding children to the current parent. But
                // since we've been working backward through them, we've
                // added them in reverse order. So we fix that now
                Collections.reverse(currentParseTreeParent.getChildren());
                // Now go up one level in the parse tree
                currentParseTreeParent = currentParseTreeParent.getParent();
                // Update the current entry
                currentSigmaSetEntry = callStack.pop();
            } else {
                GrammarElement previousElement = currentCursorRule.getPreviousGrammarElement();
                if(previousElement instanceof Terminal) {
                    // Reverse the Scan step
                    Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                    ParseTreeLeaf scanLeaf = new ParseTreeLeaf(currentParseTreeParent, scannedToken);
                    currentParseTreeParent.getChildren().add(scanLeaf);
                    currentSigmaSetIndex--;
                    currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
                    currentSigmaSetEntry = currentSigmaSetEntry.getPrecedingEntries().get(0);
                } else {
                    // Reverse the Exit & End step
                    // Find the EarleySigmaSetEntry that started the
                    // call that is currently ending
                    EarleySigmaSetEntry precedingEntry = currentSigmaSetEntry.getPrecedingEntries().get(0);
                    int endingTag = precedingEntry.getTag();
                    EarleySigmaSet callingSigmaSet = sigmaSets.get(endingTag);
                    CursorGrammarRule previousCursorRule = currentSigmaSetEntry.getCursorGrammarRule().createPrevious();
                    EarleySigmaSetEntry callingEntryCopy = new EarleySigmaSetEntry(previousCursorRule, currentSigmaSetEntry.getTag());
                    EarleySigmaSetEntry callingEntry = callingSigmaSet.get(callingEntryCopy);
                    callStack.push(callingEntry);
                    // Now we need to add a new parent to the parse
                    // tree, and then make it the current parent
                    Nonterminal endingNonterminal = (Nonterminal) previousElement;
                    ParseTreeParent endingParent = new ParseTreeParent(currentParseTreeParent, endingNonterminal);
                    currentParseTreeParent.getChildren().add(endingParent);
                    currentParseTreeParent = endingParent;
                    currentSigmaSetEntry = precedingEntry;
                }
            }
        }

        return currentParseTreeParent;
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
