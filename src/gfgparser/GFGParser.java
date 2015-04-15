package gfgparser;

import shared.*;

import java.util.*;

public class GFGParser implements Parser {

    private Grammar grammar;
    private HashMap<Nonterminal, StartGFGNode> startNodes;
    private HashMap<Nonterminal, EndGFGNode> endNodes;
    private HashMap<Nonterminal, OldStartEndGFGNode> oldStartNodes;
    private HashMap<Nonterminal, OldStartEndGFGNode> oldEndNodes;

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
                    if(! currentElement.isNonterminal()) {
                        // This is a typical transition edge
                        Terminal currentTerminal = (Terminal) currentElement;
                        previousNode.setTransition(currentTerminal);
                        previousNode.setNextNode(nextNode);
                    } else {
                        // previousNode is a call node, and nextNode is a return node
                        Nonterminal currentNonterminal = (Nonterminal) currentElement;
                        // Leave previousNode's transition null, to represent an epsilon transition
                        StartGFGNode calledStartNode = startNodes.get(currentNonterminal);
                        previousNode.setNextNode(calledStartNode);
                        EndGFGNode calledEndNode = endNodes.get(currentNonterminal);
                        calledEndNode.mapNodes(previousNode, nextNode);
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
    public EarleyParseTreeNode parse(List<Token> tokens) {
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
        EarleyParseTreeParent currentParseTreeParent = new EarleyParseTreeParent(null, startRule);
        int currentSigmaSetIndex = tokens.size();
        GFGSigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        // We want to get the original accepting sigma set entry, because it
        // has references to the sigma set entries preceding it
        GFGSigmaSetEntry currentSigmaSetEntry = currentSigmaSet.get(acceptingEntry);
        Stack<GFGSigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            GFGNode currentNode = currentSigmaSetEntry.getNode();
            if(currentNode instanceof EndGFGNode) {
                EndGFGNode endCurrentNode = (EndGFGNode) currentNode;
                // By the time I'm at an end node, I've already passed the
                // return node I need to push onto the stack.
            }
        }

        /*
        // The recognizing was successful - rebuild the parse tree
        EarleyParseTreeParent currentParseTreeParent = new EarleyParseTreeParent(null, startRule);
        int currentSigmaSetIndex = tokens.size();
        HashSet<OldGFGSigmaSetEntry> currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        OldGFGSigmaSetEntry currentSigmaSetEntry = acceptingEntry;
        Stack<OldGFGSigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            OldGFGNode currentOldGFGNode = currentSigmaSetEntry.getGFGNode();
            if(currentOldGFGNode.isStartEndGFGNode()) {
                OldStartEndGFGNode currentStartEndNode = (OldStartEndGFGNode) currentOldGFGNode;
                if(currentStartEndNode.isStartNode()) {
                    // Reverse the Call step
                    currentParseTreeParent = currentParseTreeParent.getParent();
                    OldGFGSigmaSetEntry finishedSigmaSetEntry = callStack.pop();
                    CursorGrammarRule finishedCursorRule = ((OldRuleGFGNode) finishedSigmaSetEntry.getGFGNode()).getCursorGrammarRule();
                    for(OldGFGSigmaSetEntry possibleEntry : currentSigmaSet) {
                        if(possibleEntry.getTag() != finishedSigmaSetEntry.getTag()) {
                            continue;
                        }
                        OldGFGNode possibleNode = possibleEntry.getGFGNode();
                        if(possibleNode.isStartEndGFGNode()) {
                            continue;
                        }
                        OldRuleGFGNode possibleRuleNode = (OldRuleGFGNode) possibleNode;
                        if(possibleRuleNode.getCursorGrammarRule().createNext().equals(finishedCursorRule)) {
                            currentSigmaSetEntry = possibleEntry;
                            break;
                        }
                    }
                } else {
                    // Reverse the Exit step
                    // Just search through this sigma set for rules with this nonterminal
                    // that are finished
                    OldGFGSigmaSetEntry exitingEntry = null;
                    for(OldGFGSigmaSetEntry possibleEntry : currentSigmaSet) {
                        if(possibleEntry.getTag() != currentSigmaSetEntry.getTag()) {
                            continue;
                        }
                        OldGFGNode possibleNode = possibleEntry.getGFGNode();
                        if(possibleNode.isStartEndGFGNode()) {
                            continue;
                        }
                        OldRuleGFGNode possibleRuleNode = (OldRuleGFGNode) possibleNode;
                        if(possibleRuleNode.getNextNode().equals(currentStartEndNode)) {
                            // This is a match. There may be other matches, if the string is ambiguous
                            exitingEntry = possibleEntry;
                            break;
                        }
                    }
                    OldRuleGFGNode exitingRuleNode = (OldRuleGFGNode) exitingEntry.getGFGNode();
                    GrammarRule exitingGrammarRule = exitingRuleNode.getCursorGrammarRule().getGrammarRule();
                    EarleyParseTreeParent exitingElementParent = new EarleyParseTreeParent(currentParseTreeParent, exitingGrammarRule);
                    currentParseTreeParent.getChildren().add(0, exitingElementParent);
                    currentParseTreeParent = exitingElementParent;
                    currentSigmaSetEntry = exitingEntry;
                }
            } else {
                OldRuleGFGNode currentRuleNode = (OldRuleGFGNode) currentOldGFGNode;
                if(currentRuleNode.getCursorGrammarRule().cursorAtStart()) {
                    // Reverse the Start step
                    Nonterminal startingNonterminal = currentRuleNode.getCursorGrammarRule().getGrammarRule().getLeftHandSide();
                    for(OldGFGSigmaSetEntry possibleEntry : currentSigmaSet) {
                        OldGFGNode possibleNode = possibleEntry.getGFGNode();
                        if(! possibleNode.isStartEndGFGNode()) {
                            continue;
                        }
                        OldStartEndGFGNode possibleStartEndNode = (OldStartEndGFGNode) possibleNode;
                        if(! possibleStartEndNode.isStartNode()) {
                            continue;
                        }
                        if(possibleStartEndNode.getNonterminal().equals(startingNonterminal)) {
                            currentSigmaSetEntry = possibleEntry;
                            break;
                        }
                    }
                } else {
                    // Keep working backwards through the current rule
                    GrammarElement previousElement = currentRuleNode.getCursorGrammarRule().getPreviousGrammarElement();
                    if(previousElement.isNonterminal()) {
                        // Reverse the End step
                        // Look in the current sigma set for the appropriate end node
                        OldGFGSigmaSetEntry endingEntry = null;
                        Nonterminal endingNonterminal = (Nonterminal) previousElement;
                        entry_search_loop:
                        for(OldGFGSigmaSetEntry possibleEntry : currentSigmaSet) {
                            if(! possibleEntry.getGFGNode().isStartEndGFGNode()) {
                                continue;
                            }
                            OldStartEndGFGNode possibleEndingNode = (OldStartEndGFGNode) possibleEntry.getGFGNode();
                            if(possibleEndingNode.isStartNode()) {
                                continue;
                            }
                            if(! possibleEndingNode.getNonterminal().equals(endingNonterminal)) {
                                continue;
                            }
                            // For it to be a match, there must also be the corresponding call node in the right sigma set
                            int possibleEndingTag = possibleEntry.getTag();
                            HashSet<OldGFGSigmaSetEntry> possibleEndingSigmaSet = sigmaSets.get(possibleEndingTag);
                            int currentTag = currentSigmaSetEntry.getTag();
                            for(OldGFGSigmaSetEntry possibleCallEntry : possibleEndingSigmaSet) {
                                if(possibleCallEntry.getTag() != currentTag) {
                                    continue;
                                }
                                OldGFGNode possibleCallNode = possibleCallEntry.getGFGNode();
                                if(possibleCallNode.isStartEndGFGNode()) {
                                    continue;
                                }
                                OldRuleGFGNode possibleCallRuleNode = (OldRuleGFGNode) possibleCallNode;
                                CursorGrammarRule possibleCallCursorRule = possibleCallRuleNode.getCursorGrammarRule();
                                CursorGrammarRule endingCursorRule = currentRuleNode.getCursorGrammarRule();
                                if(possibleCallCursorRule.createNext().equals(endingCursorRule)) {
                                    // This is a match. We don't need to modify the parse tree at all, so just
                                    // update the current sigma set entry and proceed to the next main loop iteration
                                    callStack.push(currentSigmaSetEntry);
                                    currentSigmaSetEntry = possibleEntry;
                                    break entry_search_loop;
                                }
                            }
                        }
                    } else {
                        // Reverse the Scan step
                        Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                        EarleyParseTreeLeaf terminalNode = new EarleyParseTreeLeaf(currentParseTreeParent, scannedToken);
                        currentParseTreeParent.getChildren().add(0, terminalNode);
                        // Find the previous sigma set entry
                        HashSet<OldGFGSigmaSetEntry> previousSigmaSet = sigmaSets.get(currentSigmaSetIndex - 1);
                        for(OldGFGSigmaSetEntry possibleEntry : previousSigmaSet) {
                            if(possibleEntry.getTag() != currentSigmaSetEntry.getTag()) {
                                continue;
                            }
                            OldGFGNode possibleNode = possibleEntry.getGFGNode();
                            if(possibleNode.isStartEndGFGNode()) {
                                continue;
                            }
                            OldRuleGFGNode possibleRuleNode = (OldRuleGFGNode) possibleNode;
                            if(possibleRuleNode.getNextNode().equals(currentOldGFGNode)) {
                                currentSigmaSetEntry = possibleEntry;
                                currentSigmaSetIndex--;
                                currentSigmaSet = previousSigmaSet;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return currentParseTreeParent;
        */
    }

    private void fillSigmaSet(ArrayList<GFGSigmaSet> sigmaSets,
                              int currentSigmaSetIndex,
                              ArrayDeque<GFGSigmaSetEntry> toProcess) {
        GFGSigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        while(! toProcess.isEmpty()) {
            GFGSigmaSetEntry processing = toProcess.remove();
            if(currentSigmaSet.contains(processing)) {
                // I think this might need to be changed. It works for
                // recognizing and getting a single parse tree, but I
                // think to properly enumerate all possible parse trees,
                // we need to not just drop this entry but map it to
                // the entry already in the set. Maybe. Or something
                // like that.
                continue;
            }
            currentSigmaSet.add(processing);

            GFGNode entryNode = processing.getNode();
            if(entryNode instanceof StartGFGNode) {
                // This is the Start rule
                StartGFGNode startEntryNode = (StartGFGNode) entryNode;
                List<InnerGFGNode> nextNodes = startEntryNode.getNextNodes();
                for(InnerGFGNode nextNode : nextNodes) {
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                    toProcess.add(newEntry);
                }
            } else if(entryNode instanceof InnerGFGNode) {
                InnerGFGNode innerEntryNode = (InnerGFGNode) entryNode;
                if(innerEntryNode.getTransition() != null) {
                    // This is the scan rule. We do this in the parse method,
                    // so just drop this now
                } else {
                    GFGNode nextNode = innerEntryNode.getNextNode();
                    if(nextNode instanceof StartGFGNode) {
                        // This is the Call rule
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, currentSigmaSetIndex, processing);
                        toProcess.add(newEntry);
                    } else {
                        // This is the Exit rule
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                        toProcess.add(newEntry);
                    }
                }
            } else { // entryNode instanceof EndGFGNode
                // This is the End rule
                EndGFGNode endEntryNode = (EndGFGNode) entryNode;
                int endingTag = processing.getTag();
                GFGSigmaSet endingSigmaSet = sigmaSets.get(endingTag);
                Nonterminal endingNonterminal = endEntryNode.getNonterminal();
                Set<GFGSigmaSetEntry> callingEntries = endingSigmaSet.getEntriesPrecedingNonterminal(endingNonterminal);
                for(GFGSigmaSetEntry callingEntry : callingEntries) {
                    InnerGFGNode callNode = (InnerGFGNode) callingEntry.getNode();
                    InnerGFGNode returnNode = endEntryNode.getReturnNode(callNode);
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(returnNode, callingEntry.getTag(), processing);
                    toProcess.add(newEntry);
                }
            }
        }
    }
}
