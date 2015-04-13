package gfgparser;

import shared.*;

import java.util.*;

public class GFGParser implements Parser {

    private Grammar grammar;
    private HashMap<Nonterminal, StartEndGFGNode> startNodes;
    private HashMap<Nonterminal, StartEndGFGNode> endNodes;

    public GFGParser(Grammar g) {
        setGrammar(g);
    }

    @Override
    public void setGrammar(Grammar g) {
        grammar = g;
        // build the GFG
        // first, build all the start and end nodes
        startNodes = new HashMap<>();
        endNodes = new HashMap<>();
        Set<Nonterminal> nonterminals = grammar.getNonterminals();
        for(Nonterminal nonterminal : nonterminals) {
            StartEndGFGNode startNode = new StartEndGFGNode(nonterminal, true);
            startNodes.put(nonterminal, startNode);
            StartEndGFGNode endNode = new StartEndGFGNode(nonterminal, false);
            endNodes.put(nonterminal, endNode);
        }

        // now add all the call edges
        for(Nonterminal nonterminal : nonterminals) {
            StartEndGFGNode startNode = startNodes.get(nonterminal);
            StartEndGFGNode endNode = endNodes.get(nonterminal);
            ArrayList<GrammarRule> nonterminalRules = grammar.getRulesWithLeftHandSide(nonterminal);
            for(GrammarRule rule : nonterminalRules) {
                CursorGrammarRule firstCursorRule = new CursorGrammarRule(rule, 0);
                RuleGFGNode firstRuleNode = new RuleGFGNode(firstCursorRule);
                startNode.addNextNode(firstCursorRule, firstRuleNode);
                RuleGFGNode currentNode = firstRuleNode;
                for(int nextIndex = 1; nextIndex <= rule.getRightHandSide().size(); nextIndex++) {
                    CursorGrammarRule nextCursorRule = new CursorGrammarRule(rule, nextIndex);
                    RuleGFGNode nextRuleNode = new RuleGFGNode(nextCursorRule);
                    GrammarElement currentElement = rule.getRightHandSide().get(nextIndex - 1);
                    if(currentElement.isNonterminal()) {
                        // we need to add call and return edges
                        Nonterminal currentNonterminal = (Nonterminal) currentElement;
                        StartEndGFGNode currentNonterminalStartNode = startNodes.get(nonterminal);
                        currentNode.setNextNode(currentNonterminalStartNode);
                        StartEndGFGNode currentNonterminalEndNode = endNodes.get(nonterminal);
                        currentNonterminalEndNode.addNextNode(nextCursorRule, nextRuleNode);
                    } else {
                        currentNode.setNextNode(nextRuleNode);
                    }
                    currentNode = nextRuleNode;
                }
                currentNode.setNextNode(endNode);
            }
        }
        // and we're done
    }

    @Override
    public ParseTreeNode parse(List<Token> tokens) {
        // keep a list of sigma sets. In this list, index j will correspond to
        // the sigma set right before the jth token.
        ArrayList<HashSet<GFGSigmaSetEntry>> sigmaSets = new ArrayList<>();

        // set up the first sigma set
        HashSet<GFGSigmaSetEntry> sigmaSet0 = new HashSet<>();
        sigmaSets.add(sigmaSet0);
        ArrayDeque<GFGSigmaSetEntry> sigmaSet0ToProcess = new ArrayDeque<>();
        GrammarRule startRule = grammar.getStartRule();
        Nonterminal startNonterminal = startRule.getLeftHandSide();
        GFGNode startGFGNode = startNodes.get(startNonterminal);
        GFGSigmaSetEntry startRuleEntry = new GFGSigmaSetEntry(startGFGNode, 0);
        sigmaSet0ToProcess.add(startRuleEntry);
        fillSigmaSet(sigmaSets, 0, sigmaSet0ToProcess);

        // process the input
        for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token currentToken = tokens.get(tokenIndex);
            HashSet<GFGSigmaSetEntry> previousSigmaSet = sigmaSets.get(tokenIndex);
            HashSet<GFGSigmaSetEntry> nextSigmaSet = new HashSet<>();
            sigmaSets.add(nextSigmaSet);
            ArrayDeque<GFGSigmaSetEntry> toProcess = new ArrayDeque<>();

            for(GFGSigmaSetEntry previousEntry : previousSigmaSet) {
                GFGNode entryNode = previousEntry.getGFGNode();
                if(entryNode.isStartEndGFGNode()) {
                    continue;
                }
                RuleGFGNode entryRuleNode = (RuleGFGNode) entryNode;
                CursorGrammarRule entryCursorRule = entryRuleNode.getCursorGrammarRule();
                if(! entryCursorRule.cursorAtEnd() && ! entryCursorRule.getNextGrammarElement().isNonterminal()) {
                    Terminal terminalAfterCursor = (Terminal) entryCursorRule.getNextGrammarElement();
                    if(terminalAfterCursor.getSymbol() == currentToken.getType()) {
                        GFGNode nextNode = entryRuleNode.getNextNode();
                        int nextTag = previousEntry.getTag();
                        GFGSigmaSetEntry scanEntry = new GFGSigmaSetEntry(nextNode, nextTag);
                        toProcess.add(scanEntry);
                    }
                }
            }

            fillSigmaSet(sigmaSets, tokenIndex + 1, toProcess);
        }

        GFGNode acceptingNode = endNodes.get(grammar.getStartRule().getLeftHandSide());
        GFGSigmaSetEntry acceptingEntry = new GFGSigmaSetEntry(acceptingNode, 0);
        if(sigmaSets.get(tokens.size()).contains(acceptingEntry)) {
            return new ParseTreeParent(null, null);
        } else {
            return null;
        }
    }

    private void fillSigmaSet(ArrayList<HashSet<GFGSigmaSetEntry>> sigmaSets,
                              int currentSigmaSetIndex,
                              ArrayDeque<GFGSigmaSetEntry> toProcess) {
        HashSet<GFGSigmaSetEntry> currentSigmaSet = sigmaSets.get(currentSigmaSetIndex);
        while(! toProcess.isEmpty()) {
            GFGSigmaSetEntry processing = toProcess.remove();
            if(currentSigmaSet.contains(processing)) {
                continue;
            }
            currentSigmaSet.add(processing);

            GFGNode entryNode = processing.getGFGNode();
            if(entryNode.isStartEndGFGNode()) {
                StartEndGFGNode startEndNode = (StartEndGFGNode) entryNode;
                if(startEndNode.isStartNode()) {
                    // This is the Start step
                    Map<CursorGrammarRule, GFGNode> nextNodes = startEndNode.getNextNodes();
                    int currentTag = processing.getTag();
                    for(CursorGrammarRule cursorRule : nextNodes.keySet()) {
                        GFGNode nextNode = nextNodes.get(cursorRule);
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(nextNode, currentTag);
                        toProcess.add(newEntry);
                    }
                } else {
                    // This is the End step
                    int endingTag = processing.getTag();
                    Nonterminal endingNonterminal = startEndNode.getNonterminal();
                    HashSet<GFGSigmaSetEntry> previousSigmaSet = sigmaSets.get(endingTag);
                    for(GFGSigmaSetEntry possibleEndingEntry : previousSigmaSet) {
                        GFGNode possibleEndingNode = possibleEndingEntry.getGFGNode();
                        if(possibleEndingNode.isStartEndGFGNode()) {
                            continue;
                        }
                        RuleGFGNode possibleRuleNode = (RuleGFGNode) possibleEndingNode;
                        CursorGrammarRule cursorRule = possibleRuleNode.getCursorGrammarRule();
                        if(cursorRule.cursorAtEnd()) {
                            continue;
                        }
                        GrammarElement nextElement = cursorRule.getNextGrammarElement();
                        if(! nextElement.isNonterminal()) {
                            continue;
                        }
                        Nonterminal nextNonterminal = (Nonterminal) nextElement;
                        if(nextNonterminal.equals(endingNonterminal)) {
                            GFGNode continuingNode = possibleRuleNode.getNextNode();
                            int continuingTag = possibleEndingEntry.getTag();
                            GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(continuingNode, continuingTag);
                            toProcess.add(newEntry);
                        }
                    }
                }
            } else {
                RuleGFGNode ruleNode = (RuleGFGNode) entryNode;
                CursorGrammarRule cursorRule = ruleNode.getCursorGrammarRule();
                if(cursorRule.cursorAtEnd()) {
                    // It's the Exit step
                    GFGNode endNode = ruleNode.getNextNode();
                    int currentTag = processing.getTag();
                    GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(endNode, currentTag);
                    toProcess.add(newEntry);
                } else {
                    GrammarElement nextElement = cursorRule.getNextGrammarElement();
                    if(nextElement.isNonterminal()) {
                        // It's the Call step
                        GFGNode startNode = ruleNode.getNextNode();
                        GFGSigmaSetEntry newEntry = new GFGSigmaSetEntry(startNode, currentSigmaSetIndex);
                        toProcess.add(newEntry);
                    } else {
                        // It's the Scan step
                        // We're done flooding here, so just drop this node
                    }
                }
            }
        }
    }

    private HashSet<GFGPath> extendPaths(ArrayDeque<GFGPath> initialToProcess) {
        HashSet<GFGPath> extendedPaths = new HashSet<>();
        while(! initialToProcess.isEmpty()) {
            GFGPath startingPath = initialToProcess.remove();
            ArrayDeque<GFGPath> currentToProcess = new ArrayDeque<>();
            HashSet<CursorGrammarRule> processedRules = new HashSet<>();
            currentToProcess.add(startingPath);
            while(! currentToProcess.isEmpty()) {
                GFGPath currentPath = currentToProcess.remove();
            }
            while(true) {
                GFGNode lastNode = startingPath.getLastNode();
                if(lastNode.isStartEndGFGNode()) {
                    StartEndGFGNode lastStartEndNode = (StartEndGFGNode) lastNode;
                    if(lastStartEndNode.isStartNode()) {
                        // Here, the current path must split. We need copies of the
                        // current path for each of the possible next nodes in the
                        // GFG. So if there are n next nodes in the GFG, we make
                        // n-1 copies of the current path, and put those in the
                        // initialToProcess queue. Then, we take the last of the next nodes,
                        // apply it to the current copy of this path, and keep going.
                        Map<CursorGrammarRule, GFGNode> nextNodes = lastStartEndNode.getNextNodes();
                        int numProcessed = 0;
                        for(CursorGrammarRule nextRule : nextNodes.keySet()) {
                            GFGNode nextNode = nextNodes.get(nextRule);
                            if(numProcessed < nextNodes.size() - 1) {
                                GFGPath copy = startingPath.copy();
                                copy.addNode(nextNode);
                                initialToProcess.add(copy);
                            } else {
                                // this is the nth node - we just apply it to the
                                // current path
                                startingPath.addNode(nextNode);
                                initialToProcess.add(startingPath);
                            }
                            numProcessed++;
                        }
                    } else {
                        // this is the return step. Here we pop a CursorGrammarRule off our stack,
                        // which tells us where we're going back to. Then we can consult the end
                        // node and retrieve the correct next node to go to.
                        CursorGrammarRule returnRule = startingPath.peekCallStack();
                        if(returnRule == null) {
                            // The only time there shouldn't be a return address on the stack is
                            // when we've finished processing the start nonterminal. Assuming that's
                            // the case, we can just add our current path to the final set and
                            // be done.
                            extendedPaths.add(startingPath);
                            break;
                        }
                        startingPath.popCallStack();
                        startingPath.addNode(lastStartEndNode.getNextNodeForGrammarRule(returnRule));
                    }
                } else {
                    RuleGFGNode lastRuleNode = (RuleGFGNode) lastNode;
                    CursorGrammarRule lastCursorGrammarRule = lastRuleNode.getCursorGrammarRule();
                    int cursorIndex = lastCursorGrammarRule.getCursorIndex();
                    GrammarRule lastRule = lastCursorGrammarRule.getGrammarRule();
                    if(cursorIndex == lastRule.getRightHandSide().size()) {
                        // we've gotten to the very end of the rule. The next node will be the
                        // end node, so just add that to the current path and move on to the
                        // next iteration
                        startingPath.addNode(lastRuleNode.getNextNode());
                    } else {
                        GrammarElement nextElement = lastRule.getRightHandSide().get(cursorIndex);
                        if(nextElement.isNonterminal()) {
                            // this is the call step. Here we need to add the correct "return address"
                            // to our call stack. The way the GFG is structured, every end node knows every
                            // potential node it could return to, and has them indexed by their CursorGrammarRule.
                            // So we simply add the next CursorGrammarRule to our stack right now and then continue.
                            CursorGrammarRule nextCursorGrammarRule = new CursorGrammarRule(lastRule, cursorIndex + 1);
                            startingPath.pushCallStack(nextCursorGrammarRule);
                            // Now we just continue with the next node, which will start the "call"
                            startingPath.addNode(lastRuleNode.getNextNode());
                        } else {
                            // we've come up against a terminal, so we're done extending this path.
                            // just add it to the final set and we're done
                            extendedPaths.add(startingPath);
                            break;
                        }
                    }
                }
            }
        }

        return extendedPaths;
    }
}
