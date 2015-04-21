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
        }

        // The recognizing was successful - rebuild the parse tree
        return buildParseTree(tokens, sigmaSets);
    }

    private void fillSigmaSet(ArrayList<EarleySigmaSet> sigmaSets,
                              int currentSigmaSetIndex,
                              ArrayDeque<EarleySigmaSetEntry> toProcess) {
        EarleySigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        // We maintain the invariant that every element in toProcess is
        // also in currentSigmaSet
        currentSigmaSet.addAll(toProcess);
        while(! toProcess.isEmpty()) {
            EarleySigmaSetEntry processing = toProcess.remove();

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
                    if(! currentSigmaSet.contains(newEntry)) {
                        currentSigmaSet.add(newEntry);
                        toProcess.add(newEntry);
                    } else {
                        // This entry is already in the set, so it
                        // doesn't need to be added to toProcess. But
                        // we do need to get the entry in the set and
                        // modify its preceding entries
                        EarleySigmaSetEntry existingEntry = currentSigmaSet.get(newEntry);
                        existingEntry.addPrecedingEntry(processing);
                    }
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
                        if(! currentSigmaSet.contains(newEntry)) {
                            currentSigmaSet.add(newEntry);
                            toProcess.add(newEntry);
                        }
                    }
                } else {
                    // This is the Scan step
                    // We don't scan in this method, so nothing happens here
                }
            }
        }
    }

    private ParseTreeNode buildParseTree(List<Token> tokens, List<EarleySigmaSet> sigmaSets) {
        GrammarRule startRule = grammar.getStartRule();
        ParseTreeParent root = new ParseTreeParent(null, startRule.getLeftHandSide());
        CursorGrammarRule acceptingCursorGrammarRule = new CursorGrammarRule(startRule, startRule.getRightHandSide().size());
        EarleySigmaSetEntry acceptingSigmaSetEntry = new EarleySigmaSetEntry(acceptingCursorGrammarRule, 0);
        EarleySigmaSet lastSigmaSet = sigmaSets.get(tokens.size());
        EarleySigmaSetEntry lastSigmaSetEntry = lastSigmaSet.get(acceptingSigmaSetEntry);

        rebuildNode(root, lastSigmaSetEntry, new ArrayList<ParseTreeNode>(), tokens.size(), sigmaSets, tokens);
        return root;

        /*
        Stack<EarleySigmaSetEntry> callStack = new Stack<>();
        int currentSigmaSetIndex = tokens.size();
        EarleySigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);

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
        */
    }

    private void rebuildNode(ParseTreeParent parent, EarleySigmaSetEntry currentEntry, List<ParseTreeNode> accumulatedNodes, int currentSigmaSetIndex, List<EarleySigmaSet> sigmaSets, List<Token> tokens) {
        // System.out.println("Starting call");
        // System.out.println("- Current entry: " + currentEntry);
        // System.out.println("- Parent: " + parent);
        // System.out.println("- Node list: " + accumulatedNodes);
        // System.out.println("- Current SSI: " + currentSigmaSetIndex);
        int numNodesInList = accumulatedNodes.size();
        while(true) {
            // System.out.println("Starting while loop");
            // System.out.println("- Current entry: " + currentEntry);
            // System.out.println("- Parent: " + parent);
            // System.out.println("- Node list: " + accumulatedNodes);
            // System.out.println("- Current SSI: " + currentSigmaSetIndex);
            CursorGrammarRule currentGrammarRule = currentEntry.getCursorGrammarRule();
            if(currentGrammarRule.isCursorAtStart()) {
                // System.out.println("At the beginning of a cursor rule");
                // System.out.println("- Current entry: " + currentEntry);
                // System.out.println("- Parent: " + parent);
                // System.out.println("- Node list: " + accumulatedNodes);
                // System.out.println("- Current SSI: " + currentSigmaSetIndex);
                // We're done working backwards on this call
                // Add (a copy of) this list to the parent
                // They were added in from rightmost to leftmost, so we
                // need to reverse them
                ArrayList<ParseTreeNode> childTreeNodes = new ArrayList<>();
                for(int i = accumulatedNodes.size() - 1; i >= 0; i--) {
                    childTreeNodes.add(accumulatedNodes.get(i));
                }
                parent.addChildTree(childTreeNodes);
                // Now we need to clean up the accumulatedNodes list
                while(accumulatedNodes.size() > numNodesInList) {
                    accumulatedNodes.remove(accumulatedNodes.size() - 1);
                }
                // And we're done
                return;
            }
            GrammarElement previousElement = currentGrammarRule.getPreviousGrammarElement();
            if(previousElement instanceof Terminal) {
                // Easy case - just create a new node and add it to the list
                Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                ParseTreeLeaf scanLeaf = new ParseTreeLeaf(parent, scannedToken);
                accumulatedNodes.add(scanLeaf);
                currentSigmaSetIndex--;
                currentEntry = currentEntry.getPrecedingEntries().get(0);
            } else { // previousElement instanceof Nonterminal
                Nonterminal previousNonterminal = (Nonterminal) previousElement;
                List<EarleySigmaSetEntry> precedingEntries = currentEntry.getPrecedingEntries();
                for(EarleySigmaSetEntry precedingEntry : precedingEntries) {
                    // Go ahead and make the node for the nonterminal
                    ParseTreeParent nonterminalNode = new ParseTreeParent(parent, previousNonterminal);
                    // Now we work down to fill in nonterminalNode
                    rebuildNode(nonterminalNode, precedingEntry, new ArrayList<ParseTreeNode>(), currentSigmaSetIndex, sigmaSets, tokens);
                    // Add the (now filled in) nonterminalNode to the
                    // list
                    accumulatedNodes.add(nonterminalNode);
                    // Now we need to recurse SIDEWAYS to finish
                    // exploring this path
                    // Get the correct sigma set entry to continue
                    // working sideways from
                    CursorGrammarRule previousGrammarRule = currentGrammarRule.createPrevious();
                    int currentTag = currentEntry.getTag();
                    int callingNonterminalTag = precedingEntry.getTag();
                    EarleySigmaSet callingSigmaSet = sigmaSets.get(callingNonterminalTag);
                    EarleySigmaSetEntry callingEntryCopy = new EarleySigmaSetEntry(previousGrammarRule, currentTag);
                    EarleySigmaSetEntry originalCallingEntry = callingSigmaSet.get(callingEntryCopy);
                    // Here, we need to continue working sideways. If
                    // there is only one preceding entry, things are
                    // easy and we can continue with our current while
                    // loop. If there is more than one preceding entry,
                    // we need to do recursive backtracking to work
                    // sideways.
                    if(precedingEntries.size() == 1) {
                        currentEntry = originalCallingEntry;
                        currentSigmaSetIndex = callingNonterminalTag;
                    } else {
                        rebuildNode(parent, originalCallingEntry, accumulatedNodes, callingNonterminalTag, sigmaSets, tokens);
                        // And remove the nonterminal node from the list
                        // so the list is unmodified for the next
                        // iteration of this loop
                        accumulatedNodes.remove(accumulatedNodes.size() - 1);
                    }
                    // Now recurse sideways
                }
                // If we were doing the recursive calls sideways, we've
                // finished all of this method
                // All we have to do is clean up the accumulatedNodes
                // list and return
                if(precedingEntries.size() > 1) {
                    while(accumulatedNodes.size() > numNodesInList) {
                        accumulatedNodes.remove(accumulatedNodes.size() - 1);
                    }
                    return;
                }
            }
        }
    }
}
