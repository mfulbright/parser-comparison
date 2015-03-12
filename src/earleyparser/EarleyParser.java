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
        // keep a list of sigma sets. In this list, index j will correspond to
        // the sigma set right before the jth token.
        ArrayList<HashSet<SigmaSetEntry>> sigmaSets = new ArrayList<>();

        // set up the first sigma set
        HashSet<SigmaSetEntry> sigmaSet0 = new HashSet<SigmaSetEntry>();
        sigmaSets.add(sigmaSet0);
        ArrayDeque<SigmaSetEntry> sigmaSet0ToProcess = new ArrayDeque<>();
        GrammarRule startRule = grammar.getStartRule();
        SigmaSetEntry startRuleEntry = new SigmaSetEntry(startRule, 0, 0);
        sigmaSet0ToProcess.add(startRuleEntry);
        fillSigmaSet(grammar, sigmaSets, 0, sigmaSet0ToProcess);

        // process the input
        for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token currentToken = tokens.get(tokenIndex);
            HashSet<SigmaSetEntry> previousSigmaSet = sigmaSets.get(tokenIndex);
            HashSet<SigmaSetEntry> nextSigmaSet = new HashSet<>();
            sigmaSets.add(nextSigmaSet);
            ArrayDeque<SigmaSetEntry> toProcess = new ArrayDeque<>();

            for(SigmaSetEntry previousEntry : previousSigmaSet) {
                GrammarRule previousGrammarRule = previousEntry.getGrammarRule();
                ArrayList<GrammarElement> previousRHS = previousGrammarRule.getRightHandSide();
                int previousCursor = previousEntry.getCursorIndex();
                if(previousCursor < previousRHS.size() && ! previousRHS.get(previousCursor).isNonterminal()) {
                    Terminal terminalAfterCursor = (Terminal) previousRHS.get(previousCursor);
                    if(terminalAfterCursor.getSymbol() == currentToken.getType()) {
                        SigmaSetEntry scanEntry = new SigmaSetEntry(previousGrammarRule, previousCursor + 1, previousEntry.getTag());
                        toProcess.add(scanEntry);
                    }
                }
            }

            fillSigmaSet(grammar, sigmaSets, tokenIndex + 1, toProcess);
        }

        SigmaSetEntry acceptingSigmaSetEntry = new SigmaSetEntry(startRule, startRule.getRightHandSide().size(), 0);
        if (! sigmaSets.get(tokens.size()).contains(acceptingSigmaSetEntry)) {
            return null;
        }

        // The recognizing was successful - rebuild the parse tree
        ParseTreeParent currentParseTreeParent = new ParseTreeParent(null, startRule);
        int currentSigmaSetIndex = tokens.size();
        HashSet<SigmaSetEntry> currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        SigmaSetEntry currentSigmaSetEntry = null;
        for(SigmaSetEntry possible : currentSigmaSet) {
            if(possible.equals(acceptingSigmaSetEntry)) {
                currentSigmaSetEntry = possible;
                break;
            }
        }
        Stack<SigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            //System.out.println(currentSigmaSetEntry);
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
                    SigmaSetEntry lowerSigmaSetEntry = null;
                    for(SigmaSetEntry possible : currentSigmaSet) {
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
                        HashSet<SigmaSetEntry> possibleTagSigmaSet = sigmaSets.get(possibleTag);
                        SigmaSetEntry necessarySigmaSetEntry = new SigmaSetEntry(currentSigmaSetEntry.getGrammarRule(), currentSigmaSetEntry.getCursorIndex() - 1, currentSigmaSetEntry.getTag());
                        if(! possibleTagSigmaSet.contains(necessarySigmaSetEntry)) {
                            continue;
                        }
                        // this is the right entry
                        // for an ambiguous string, there might be multiple entries in this set that fit this criterion. this
                        // is where we would look for those
                        lowerSigmaSetEntry = possible;
                        break;
                    }
                    ParseTreeParent previousElementParent = new ParseTreeParent(currentParseTreeParent, lowerSigmaSetEntry.getGrammarRule());
                    currentParseTreeParent.getChildren().add(0, previousElementParent);
                    currentParseTreeParent = previousElementParent;
                    callStack.push(currentSigmaSetEntry);
                    currentSigmaSetEntry = lowerSigmaSetEntry;
                } else {
                    // Reverse the scan rule
                    Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                    ParseTreeLeaf terminalNode = new ParseTreeLeaf(currentParseTreeParent, scannedToken);
                    currentParseTreeParent.getChildren().add(0, terminalNode);
                    // Find the previous sigma set entry
                    currentSigmaSetIndex--;
                    currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
                    for(SigmaSetEntry possible : currentSigmaSet) {
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
                SigmaSetEntry poppedEntry = callStack.pop();
                currentSigmaSetEntry = new SigmaSetEntry(poppedEntry.getGrammarRule(), poppedEntry.getCursorIndex() - 1, poppedEntry.getTag());
            }
        }

        return currentParseTreeParent;
    }

    private void fillSigmaSet(Grammar grammar, ArrayList<HashSet<SigmaSetEntry>> sigmaSets, int currentSigmaSetIndex, ArrayDeque<SigmaSetEntry> toProcess) {
        HashSet<SigmaSetEntry> currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        while(! toProcess.isEmpty()) {
            SigmaSetEntry processing = toProcess.remove();
            if(currentSigmaSet.contains(processing)) {
                continue;
            }
            currentSigmaSet.add(processing);

            GrammarRule entryGrammarRule = processing.getGrammarRule();
            ArrayList<GrammarElement> entryRHS = entryGrammarRule.getRightHandSide();
            int entryCursor = processing.getCursorIndex();
            if(entryCursor < entryRHS.size()) {
                GrammarElement elementAfterCursor = entryRHS.get(entryCursor);
                if(elementAfterCursor.isNonterminal()) {
                    // This is the Call and Start step (I've basically combined them in this implementation)
                    Nonterminal nonterminalAfterCursor = (Nonterminal) elementAfterCursor;
                    ArrayList<GrammarRule> callableGrammarRules = grammar.getRulesWithLeftHandSide(nonterminalAfterCursor);
                    for(GrammarRule callableGrammarRule : callableGrammarRules) {
                        SigmaSetEntry callEntry = new SigmaSetEntry(callableGrammarRule, 0, currentSigmaSetIndex);
                        toProcess.add(callEntry);
                    }
                } else {
                    // This is where the scan step would be. We're done flooding at this point, so just ignore this
                }
            } else {
                // This is the Exit and End step (I've basically combined them in this implementation)
                Nonterminal endingNonterminal = entryGrammarRule.getLeftHandSide();
                int endingTag = processing.getTag();
                HashSet<SigmaSetEntry> previousSigmaSet = sigmaSets.get(endingTag);
                for(SigmaSetEntry possibleEndingEntry : previousSigmaSet) {
                    GrammarRule possibleEndingGrammarRule = possibleEndingEntry.getGrammarRule();
                    ArrayList<GrammarElement> possibleEndingRHS = possibleEndingGrammarRule.getRightHandSide();
                    int possibleEndingCursorIndex = possibleEndingEntry.getCursorIndex();
                    if(possibleEndingCursorIndex < possibleEndingRHS.size() &&
                            possibleEndingRHS.get(possibleEndingCursorIndex).isNonterminal()) {
                        Nonterminal possibleEndingNonterminal = (Nonterminal) possibleEndingRHS.get(possibleEndingCursorIndex);
                        if(possibleEndingNonterminal.equals(endingNonterminal)) {
                            SigmaSetEntry newEndingEntry = new SigmaSetEntry(possibleEndingGrammarRule, possibleEndingCursorIndex + 1, possibleEndingEntry.getTag());
                            toProcess.add(newEndingEntry);
                        }
                    }
                }
            }
        }
    }
}
