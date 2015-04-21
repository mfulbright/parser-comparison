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
        ParseTreeParent currentParseTreeParent = new ParseTreeParent(null, startRule.getLeftHandSide());
        int currentSigmaSetIndex = tokens.size();
        GFGSigmaSet currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        // We want to get the original accepting sigma set entry, because it
        // has references to the sigma set entries preceding it
        GFGSigmaSetEntry currentSigmaSetEntry = currentSigmaSet.get(acceptingEntry);
        Stack<GFGSigmaSetEntry> callStack = new Stack<>();

        while(! currentSigmaSetEntry.equals(startRuleEntry)) {
            GFGNode currentNode = currentSigmaSetEntry.getNode();
            if(currentNode instanceof EndGFGNode) {
                // Reverse the Exit step
                // For an ambiguous string, there could be multiple preceding
                // entries. For now, we just arbitrarily pick one
                currentSigmaSetEntry = currentSigmaSetEntry.getPrecedingEntries().get(0);
            } else if(currentNode instanceof InnerGFGNode) {
                // We have to examine the preceding entry to determine what case this is
                GFGSigmaSetEntry precedingEntry = currentSigmaSetEntry.getPrecedingEntries().get(0);
                GFGNode precedingNode = precedingEntry.getNode();
                if(precedingNode instanceof StartGFGNode) {
                    // Reverse the Start step
                    currentSigmaSetEntry = precedingEntry;
                } else if(precedingNode instanceof EndGFGNode) {
                    // Reverse the End step
                    // We must find the GFGSigmaSetEntry containing the call
                    // node for the currently ending node
                    int endingCallTag = precedingEntry.getTag();
                    GFGSigmaSet endingCallSigmaSet = sigmaSets.get(endingCallTag);
                    InnerGFGNode returnNode = (InnerGFGNode) currentNode;
                    InnerGFGNode endingCallNode = returnNodesToCallNodes.get(returnNode);
                    GFGSigmaSetEntry endingCallEntryCopy = new GFGSigmaSetEntry(endingCallNode, currentSigmaSetEntry.getTag());
                    GFGSigmaSetEntry endingCallEntry = endingCallSigmaSet.get(endingCallEntryCopy);
                    callStack.push(endingCallEntry);
                    // Now we need to create a new parent node in the parse
                    // tree, and set it as our currentParseTreeParent
                    EndGFGNode endPrecedingNode = (EndGFGNode) precedingNode;
                    Nonterminal endingNonterminal = endPrecedingNode.getNonterminal();
                    ParseTreeParent endingNonterminalTreeNode = new ParseTreeParent(currentParseTreeParent, endingNonterminal);
                    currentParseTreeParent.getChildren().add(endingNonterminalTreeNode);
                    currentParseTreeParent = endingNonterminalTreeNode;
                    // Could there be multiple preceding nodes in this case? I
                    // don't know, I'll think about that when I do the parse
                    // forest stuff
                    currentSigmaSetEntry = precedingEntry;
                } else { // precedingNode instanceof InnerGFGNode
                    // Reverse the Scan step
                    Token scannedToken = tokens.get(currentSigmaSetIndex - 1);
                    ParseTreeLeaf scanLeaf = new ParseTreeLeaf(currentParseTreeParent, scannedToken);
                    currentParseTreeParent.getChildren().add(scanLeaf);
                    currentSigmaSetIndex--;
                    currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
                    currentSigmaSetEntry = currentSigmaSetEntry.getPrecedingEntries().get(0);
                }
            } else { // currentNode instanceof StartGFGNode
                // Reverse the Call step
                // We're finished working backwards through all of the current
                // parent's nonterminal. However, as we've worked backwards,
                // we've added the children in reverse order. So we reverse
                // them now.
                Collections.reverse(currentParseTreeParent.getChildren());
                // Now we go up one level in the tree
                currentParseTreeParent = currentParseTreeParent.getParent();
                // And pop off the call stack
                currentSigmaSetEntry = callStack.pop();
            }
        }

        return currentParseTreeParent;
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
                // I'm definitely not doing it right for parse forests right
                // now, because I never even consider adding to an entry's
                // precedingEntries.
                continue;
            }
            currentSigmaSet.add(processing);

            GFGNode entryNode = processing.getNode();
            if(entryNode instanceof StartGFGNode) {
                // This is the Start step
                StartGFGNode startEntryNode = (StartGFGNode) entryNode;
                List<InnerGFGNode> nextNodes = startEntryNode.getNextNodes();
                for(InnerGFGNode nextNode : nextNodes) {
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                    toProcess.add(newEntry);
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
                        toProcess.add(newEntry);
                    } else {
                        // This is the Exit step
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, processing.getTag(), processing);
                        toProcess.add(newEntry);
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
                    toProcess.add(newEntry);
                }
            }
        }
    }
}
