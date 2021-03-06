package gfgparser;

import shared.*;

import java.util.*;

public class GFGParser implements Parser {

    private Grammar grammar;
    private HashMap<Nonterminal, StartGFGNode> startNodes;
    private HashMap<Nonterminal, EndGFGNode> endNodes;
    // This is used during the parsing phase, when we're working backwards
    // through the GFG path using the call stack
    private HashMap<InnerGFGNode, InnerGFGNode> returnNodesToCallNodes;

    public GFGParser(Grammar g) {
        setGrammar(g);
    }

    @Override
    public void setGrammar(Grammar g) {
        grammar = g;
        // Build the GFG
        // First, build all the start and end nodes
        startNodes = new HashMap<>();
        endNodes = new HashMap<>();
        Set<Nonterminal> nonterminals = grammar.getNonterminals();
        for(Nonterminal nonterminal : nonterminals) {
            StartGFGNode startNode = new StartGFGNode(nonterminal);
            startNodes.put(nonterminal, startNode);
            EndGFGNode endNode = new EndGFGNode(nonterminal);
            endNodes.put(nonterminal, endNode);
        }

        // Now add the inner nodes for every grammar rule
        returnNodesToCallNodes = new HashMap<>();
        for(Nonterminal ruleLeftHandSide : nonterminals) {
            StartGFGNode leftHandSideStartNode = startNodes.get(ruleLeftHandSide);
            EndGFGNode leftHandSideEndNode = endNodes.get(ruleLeftHandSide);
            List<GrammarRule> rulesWithLeftHandSide = grammar.getRulesWithLeftHandSide(ruleLeftHandSide);
            for(GrammarRule rule : rulesWithLeftHandSide) {
                InnerGFGNode entryNode = new InnerGFGNode();
                leftHandSideStartNode.addNextNode(entryNode);
                InnerGFGNode previousNode = entryNode;
                List<GrammarElement> ruleRHS = rule.getRightHandSide();
                for(int currentIndex = 0; currentIndex < ruleRHS.size(); currentIndex++) {
                    // Go ahead and create the next GFG node
                    InnerGFGNode nextNode = new InnerGFGNode();
                    GrammarElement currentElement = ruleRHS.get(currentIndex);
                    if(currentElement instanceof Terminal) {
                        // This is a typical transition edge
                        Terminal currentTerminal = (Terminal) currentElement;
                        previousNode.setTransition(currentTerminal);
                        previousNode.setNextNode(nextNode);
                    } else { // currentElement instanceof Nonterminal
                        // previousNode is a call node, and nextNode is a return node
                        Nonterminal currentNonterminal = (Nonterminal) currentElement;
                        // Leave previousNode's transition null, to represent an epsilon transition
                        StartGFGNode calledStartNode = startNodes.get(currentNonterminal);
                        previousNode.setNextNode(calledStartNode);
                        EndGFGNode calledEndNode = endNodes.get(currentNonterminal);
                        calledEndNode.mapNodes(previousNode, nextNode);
                        returnNodesToCallNodes.put(nextNode, previousNode);
                    }
                    previousNode = nextNode;
                }
                // previousNode is now the exit node for this rule. Leave its
                // transition null to represent an epsilon transition
                previousNode.setNextNode(leftHandSideEndNode);
            }
        }
    }

    @Override
    public ParseTreeNode parse(List<Token> tokens) {
        // In this list, index j will correspond to the sigma set right before
        // the jth token
        ArrayList<GFGSigmaSet> sigmaSets = new ArrayList<>();

        // Set up the first sigma set
        GFGSigmaSet sigmaSet0 = new GFGSigmaSet();
        sigmaSets.add(sigmaSet0);
        ArrayDeque<GFGSigmaSetEntry> sigmaSet0ToProcess = new ArrayDeque<>();
        GrammarRule startRule = grammar.getStartRule();
        Nonterminal startNonterminal = startRule.getLeftHandSide();
        StartGFGNode startRuleStartNode = startNodes.get(startNonterminal);
        GFGSigmaSetEntry startRuleEntry = new GFGSigmaSetEntry(startRuleStartNode, 0);
        sigmaSet0ToProcess.add(startRuleEntry);
        fillSigmaSet(sigmaSets, 0, sigmaSet0ToProcess);

        // Process the input
        for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token currentToken = tokens.get(tokenIndex);
            GFGSigmaSet previousSigmaSet = sigmaSets.get(tokenIndex);
            GFGSigmaSet nextSigmaSet = new GFGSigmaSet();
            sigmaSets.add(nextSigmaSet);
            ArrayDeque<GFGSigmaSetEntry> toProcess = new ArrayDeque<>();
            Set<GFGSigmaSetEntry> scanableEntries = previousSigmaSet.getEntriesPrecedingSymbol(currentToken.getType());
            for(GFGSigmaSetEntry scanableEntry : scanableEntries) {
                InnerGFGNode scanningNode = (InnerGFGNode) scanableEntry.getNode();
                GFGNode nextNode = scanningNode.getNextNode();
                GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, scanableEntry.getTag(), scanableEntry);
                toProcess.add(newEntry);
            }

            fillSigmaSet(sigmaSets, tokenIndex + 1, toProcess);
        }

        EndGFGNode acceptingNode = endNodes.get(grammar.getStartRule().getLeftHandSide());
        GFGSigmaSetEntry acceptingEntry = new GFGSigmaSetEntry(acceptingNode, 0);
        GFGSigmaSet finalSigmaSet = sigmaSets.get(tokens.size());
        if(! finalSigmaSet.contains(acceptingEntry)) {
            return null;
        }

        // The recognizing was successful - rebuild the parse tree
        return buildParseTree(tokens, sigmaSets);
    }

    private void fillSigmaSet(ArrayList<GFGSigmaSet> sigmaSets,
                              int currentSigmaSetIndex,
                              ArrayDeque<GFGSigmaSetEntry> toProcess) {
        GFGSigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        currentSigmaSet.addAll(toProcess);
        while(! toProcess.isEmpty()) {
            GFGSigmaSetEntry processing = toProcess.remove();

            GFGNode entryNode = processing.getNode();
            if(entryNode instanceof StartGFGNode) {
                // This is the Start step
                StartGFGNode startEntryNode = (StartGFGNode) entryNode;
                List<InnerGFGNode> nextNodes = startEntryNode.getNextNodes();
                for(InnerGFGNode nextNode : nextNodes) {
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                    if(! currentSigmaSet.contains(newEntry)) {
                        currentSigmaSet.add(newEntry);
                        toProcess.add(newEntry);
                    }
                }
            } else if(entryNode instanceof InnerGFGNode) {
                InnerGFGNode innerEntryNode = (InnerGFGNode) entryNode;
                if(innerEntryNode.getTransition() != null) {
                    // This is the scan step. We do this in the parse method,
                    // so just drop this now
                } else {
                    GFGNode nextNode = innerEntryNode.getNextNode();
                    if(nextNode instanceof StartGFGNode) {
                        // This is the Call step
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, currentSigmaSetIndex, processing);
                        if(! currentSigmaSet.contains(newEntry)) {
                            currentSigmaSet.add(newEntry);
                            toProcess.add(newEntry);
                        }
                    } else {
                        // This is the Exit step
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                        if(! currentSigmaSet.contains(newEntry)) {
                            currentSigmaSet.add(newEntry);
                            toProcess.add(newEntry);
                        }
                    }
                }
            } else { // entryNode instanceof EndGFGNode
                // This is the End step
                EndGFGNode endEntryNode = (EndGFGNode) entryNode;
                int endingTag = processing.getTag();
                GFGSigmaSet endingSigmaSet = sigmaSets.get(endingTag);
                Nonterminal endingNonterminal = endEntryNode.getNonterminal();
                Set<GFGSigmaSetEntry> callingEntries = endingSigmaSet.getEntriesPrecedingNonterminal(endingNonterminal);
                for(GFGSigmaSetEntry callingEntry : callingEntries) {
                    InnerGFGNode callNode = (InnerGFGNode) callingEntry.getNode();
                    InnerGFGNode returnNode = endEntryNode.getReturnNode(callNode);
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(returnNode, callingEntry.getTag(), processing);
                    if(! currentSigmaSet.contains(newEntry)) {
                        currentSigmaSet.add(newEntry);
                        toProcess.add(newEntry);
                    } else {
                        // This entry is already in the set, so it
                        // doesn't need to be added to toProcess. But
                        // we do need to get the entry in the set and
                        // modify its preceding entries
                        GFGSigmaSetEntry existingEntry = currentSigmaSet.get(newEntry);
                        existingEntry.addPrecedingEntry(processing);
                    }
                }
            }
        }
    }

    private ParseTreeNode buildParseTree(List<Token> tokens, List<GFGSigmaSet> sigmaSets) {
        GrammarRule startRule = grammar.getStartRule();
        ParseTreeParent root = new ParseTreeParent(startRule.getLeftHandSide());
        EndGFGNode acceptingNode = endNodes.get(grammar.getStartRule().getLeftHandSide());
        GFGSigmaSetEntry acceptingSigmaSetEntry = new GFGSigmaSetEntry(acceptingNode, 0);
        GFGSigmaSet lastSigmaSet = sigmaSets.get(tokens.size());
        GFGSigmaSetEntry lastSigmaSetEntry = lastSigmaSet.get(acceptingSigmaSetEntry);

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
            GFGSigmaSetEntry currentEntry,
            List<ParseTreeNode> accumulatedNodes,
            ParseTreeNodeCache existingNodes,
            int currentSigmaSetIndex,
            List<GFGSigmaSet> sigmaSets,
            List<Token> tokens) {
        while(true) {
            GFGNode currentNode = currentEntry.getNode();
            if(currentNode instanceof StartGFGNode) {
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
            } else if(currentNode instanceof EndGFGNode) {
                // This is reversing the Exit step
                currentEntry = currentEntry.getPrecedingEntries().get(0);
            } else { // currentNode instanceof InnerGFGNode
                GFGSigmaSetEntry precedingEntry = currentEntry.getPrecedingEntries().get(0);
                GFGNode precedingNode = precedingEntry.getNode();
                if(precedingNode instanceof StartGFGNode) {
                    // This is reversing the Start step
                    currentEntry = currentEntry.getPrecedingEntries().get(0);
                } else if(precedingNode instanceof EndGFGNode) {
                    // This is reversing the End step
                    EndGFGNode precedingEndNode = (EndGFGNode) precedingNode;
                    Nonterminal previousNonterminal = precedingEndNode.getNonterminal();
                    List<GFGSigmaSetEntry> precedingEntries = currentEntry.getPrecedingEntries();
                    if(precedingEntries.size() == 1) {
                        // Easy case - we just need to add a nonterminal node
                        // as a child, fill it with children, and then
                        // keep going through the while loop
                        // First, let's get ready to keep working left, by
                        // getting the sigma set entry we'll need to continue
                        // from
                        int callingNonterminalTag = precedingEntry.getTag();
                        GFGSigmaSet endingCallSigmaSet = sigmaSets.get(callingNonterminalTag);
                        InnerGFGNode returnNode = (InnerGFGNode) currentNode;
                        InnerGFGNode endingCallNode = returnNodesToCallNodes.get(returnNode);
                        GFGSigmaSetEntry callingEntryCopy = new GFGSigmaSetEntry(endingCallNode, currentEntry.getTag());
                        GFGSigmaSetEntry originalCallingEntry = endingCallSigmaSet.get(callingEntryCopy);

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
                        for(GFGSigmaSetEntry singlePrecedingEntry : precedingEntries) {
                            // Get the correct sigma set entry to continue
                            // working left from
                            int callingNonterminalTag = singlePrecedingEntry.getTag();
                            GFGSigmaSet endingCallSigmaSet = sigmaSets.get(callingNonterminalTag);
                            InnerGFGNode returnNode = (InnerGFGNode) currentNode;
                            InnerGFGNode endingCallNode = returnNodesToCallNodes.get(returnNode);
                            GFGSigmaSetEntry callingEntryCopy = new GFGSigmaSetEntry(endingCallNode, currentEntry.getTag());
                            GFGSigmaSetEntry originalCallingEntry = endingCallSigmaSet.get(callingEntryCopy);

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
                                        singlePrecedingEntry,
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
                } else { // precedingNode instanceof InnerGFGNode
                    // Reverse the Scan step
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
                }
            }
        }
    }
}
