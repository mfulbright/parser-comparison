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
        ParseTreeParent root = new ParseTreeParent(startRule.getLeftHandSide());
        CursorGrammarRule acceptingCursorGrammarRule = new CursorGrammarRule(startRule, startRule.getRightHandSide().size());
        EarleySigmaSetEntry acceptingSigmaSetEntry = new EarleySigmaSetEntry(acceptingCursorGrammarRule, 0);
        EarleySigmaSet lastSigmaSet = sigmaSets.get(tokens.size());
        EarleySigmaSetEntry lastSigmaSetEntry = lastSigmaSet.get(acceptingSigmaSetEntry);

        addChildrenRightToLeft(
                root,
                lastSigmaSetEntry,
                new ArrayList<ParseTreeNode>(),
                new ParseTreeNodeCache(),
                tokens.size(),
                sigmaSets,
                tokens
        );
        return root;
    }

    private void addChildrenRightToLeft(
            ParseTreeParent parent,
            EarleySigmaSetEntry currentEntry,
            List<ParseTreeNode> accumulatedNodes,
            ParseTreeNodeCache existingNodes,
            int currentSigmaSetIndex,
            List<EarleySigmaSet> sigmaSets,
            List<Token> tokens) {
        while(true) {
            CursorGrammarRule currentGrammarRule = currentEntry.getCursorGrammarRule();
            if(currentGrammarRule.isCursorAtStart()) {
                // We're done working right to left
                // Add (a copy of) this list to the parent
                // They were added in from right to left, so we
                // need to reverse them first
                ArrayList<ParseTreeNode> childTreeNodes = new ArrayList<>();
                for(int i = accumulatedNodes.size() - 1; i >= 0; i--) {
                    childTreeNodes.add(accumulatedNodes.get(i));
                }
                parent.addChildTree(childTreeNodes);
                // And we're done
                return;
            }
            GrammarElement previousElement = currentGrammarRule.getPreviousGrammarElement();
            if(previousElement instanceof Terminal) {
                // Easy case - just create a new node and add it to the list
                int tokenIndex = currentSigmaSetIndex - 1;
                ParseTreeLeaf scanLeaf;
                if(existingNodes.containsLeaf(tokenIndex)) {
                    scanLeaf = existingNodes.getLeaf(tokenIndex);
                } else {
                    Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                    scanLeaf = new ParseTreeLeaf(scannedToken);
                    existingNodes.addLeaf(tokenIndex, scanLeaf);
                }
                accumulatedNodes.add(scanLeaf);
                currentSigmaSetIndex--;
                currentEntry = currentEntry.getPrecedingEntries().get(0);
            } else { // previousElement instanceof Nonterminal
                Nonterminal previousNonterminal = (Nonterminal) previousElement;
                List<EarleySigmaSetEntry> precedingEntries = currentEntry.getPrecedingEntries();
                if(precedingEntries.size() == 1) {
                    // Easy case - we just need to add a nonterminal node
                    // as a child, fill it with children, and then
                    // keep going through the while loop
                    // First, let's get ready to keep working left, by
                    // getting the sigma set entry we'll need to continue
                    // from
                    CursorGrammarRule previousGrammarRule = currentGrammarRule.createPrevious();
                    int currentTag = currentEntry.getTag();
                    EarleySigmaSetEntry precedingEntry = precedingEntries.get(0);
                    int callingNonterminalTag = precedingEntry.getTag();
                    EarleySigmaSet callingSigmaSet = sigmaSets.get(callingNonterminalTag);
                    EarleySigmaSetEntry callingEntryCopy = new EarleySigmaSetEntry(previousGrammarRule, currentTag);
                    EarleySigmaSetEntry originalCallingEntry = callingSigmaSet.get(callingEntryCopy);
                    // Now we'll get the nonterminal node if it already
                    // exists, or create it and fill it if it doesn't
                    int lastTokenIndexCovered = currentSigmaSetIndex - 1;
                    int firstTokenIndexCovered = callingNonterminalTag;
                    ParseTreeParent nonterminalNode;
                    if(existingNodes.containsParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal)) {
                        nonterminalNode = existingNodes.getParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal);
                    } else {
                        nonterminalNode = new ParseTreeParent(previousNonterminal);
                        existingNodes.addParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal, nonterminalNode);
                        addChildrenRightToLeft(
                                nonterminalNode,
                                precedingEntry,
                                new ArrayList<ParseTreeNode>(),
                                existingNodes,
                                currentSigmaSetIndex,
                                sigmaSets,
                                tokens
                        );
                    }
                    accumulatedNodes.add(nonterminalNode);
                    // Update the fields and keep working left through
                    // the while loop
                    currentEntry = originalCallingEntry;
                    currentSigmaSetIndex = callingNonterminalTag;
                } else {
                    // This is the harder case - we need to do recursive
                    // backtracking to follow all possible paths to the
                    // left
                    // Keep track of the current size of the
                    // accumulated nodes list so that after every
                    // recursive call we can get it back to its
                    // current state
                    int numAccumNodes = accumulatedNodes.size();
                    for(EarleySigmaSetEntry precedingEntry : precedingEntries) {
                        // Get the correct sigma set entry to continue
                        // working left from
                        CursorGrammarRule previousGrammarRule = currentGrammarRule.createPrevious();
                        int currentTag = currentEntry.getTag();
                        int callingNonterminalTag = precedingEntry.getTag();
                        EarleySigmaSet callingSigmaSet = sigmaSets.get(callingNonterminalTag);
                        EarleySigmaSetEntry callingEntryCopy = new EarleySigmaSetEntry(previousGrammarRule, currentTag);
                        EarleySigmaSetEntry originalCallingEntry = callingSigmaSet.get(callingEntryCopy);

                        int lastTokenIndexCovered = currentSigmaSetIndex - 1;
                        int firstTokenIndexCovered = callingNonterminalTag;
                        ParseTreeParent nonterminalNode;
                        if(existingNodes.containsParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal)) {
                            nonterminalNode = existingNodes.getParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal);
                        } else {
                            nonterminalNode = new ParseTreeParent(previousNonterminal);
                            existingNodes.addParent(firstTokenIndexCovered, lastTokenIndexCovered, previousNonterminal, nonterminalNode);
                            addChildrenRightToLeft(
                                    nonterminalNode,
                                    precedingEntry,
                                    new ArrayList<ParseTreeNode>(),
                                    existingNodes,
                                    currentSigmaSetIndex,
                                    sigmaSets,
                                    tokens
                            );
                        }
                        accumulatedNodes.add(nonterminalNode);

                        // Do recursive backtracking to follow all paths
                        // First we must save the size of the
                        // accumulatedNodes list, so we can remove all
                        // the children that the recursive call adds
                        addChildrenRightToLeft(
                                parent,
                                originalCallingEntry,
                                accumulatedNodes,
                                existingNodes,
                                callingNonterminalTag,
                                sigmaSets,
                                tokens);
                        // Remove the nodes now
                        while (accumulatedNodes.size() > numAccumNodes) {
                            accumulatedNodes.remove(accumulatedNodes.size() - 1);
                        }
                    }
                    // We've done all the working left we need to do
                    // through the recursive calls, so we can just
                    // return
                    return;
                }
            }
        }
    }
}
