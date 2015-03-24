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
        ArrayDeque<GFGPath> initialToProcess = new ArrayDeque<>();
        GFGPath startPath = new GFGPath();
        startPath.addNode(startNodes.get(grammar.getStartRule().getLeftHandSide()));
        initialToProcess.add(startPath);
        HashSet<GFGPath> possiblePaths = extendPaths(initialToProcess);

        for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            Token currentToken = tokens.get(tokenIndex);
            ArrayDeque<GFGPath> toProcess = new ArrayDeque<>();
            // Java collections doesn't give me a good way to iterate over a set and
            // remove/modify elements as I go, so I have to structure my list this way
            while(! possiblePaths.isEmpty()) {
                Iterator<GFGPath> pathsIterator = possiblePaths.iterator();
                GFGPath possiblePath = pathsIterator.next();
                pathsIterator.remove();

                GFGNode lastNode = possiblePath.getLastNode();
                // Pretty much all "lastNode"s should be RuleGFGNodes that are right
                // before a terminal. However, there is an exception: if we finished
                // the start rule, the lastNode will be an end node. So if that's the
                // case, just skip it.
                if(lastNode.isStartEndGFGNode()) {
                    continue;
                }
                RuleGFGNode lastRuleNode = (RuleGFGNode) lastNode;
                CursorGrammarRule lastCursorGrammarRule = lastRuleNode.getCursorGrammarRule();
                GrammarRule lastRule = lastCursorGrammarRule.getGrammarRule();
                int cursorIndex = lastCursorGrammarRule.getCursorIndex();
                assert(cursorIndex < lastRule.getRightHandSide().size());
                GrammarElement nextGrammarElement = lastRule.getRightHandSide().get(cursorIndex);
                assert(! nextGrammarElement.isNonterminal());
                Terminal nextTerminal = (Terminal) nextGrammarElement;
                if(currentToken.getType().equals(nextTerminal.getSymbol())) {
                    // add one node to the path, and add it to the toProcess queue
                    possiblePath.addNode(lastRuleNode.getNextNode());
                    toProcess.add(possiblePath);
                }
            }

            // now that we've done the scan step for all the paths, flood them as far
            // as they'll go
            possiblePaths = extendPaths(toProcess);
        }

        // for now, it's just going to be a recognizer
        GFGNode acceptingNode = endNodes.get(grammar.getStartRule().getLeftHandSide());
        for(GFGPath possiblePath : possiblePaths) {
            if(possiblePath.getLastNode() == acceptingNode) {
                return new ParseTreeParent(null, null);
            }
        }
        return null;
    }

    private HashSet<GFGPath> extendPaths(ArrayDeque<GFGPath> toProcess) {
        HashSet<GFGPath> extendedPaths = new HashSet<>();
        while(! toProcess.isEmpty()) {
            GFGPath currentPath = toProcess.remove();
            while(true) {
                GFGNode lastNode = currentPath.getLastNode();
                if(lastNode.isStartEndGFGNode()) {
                    StartEndGFGNode lastStartEndNode = (StartEndGFGNode) lastNode;
                    if(lastStartEndNode.isStartNode()) {
                        // Here, the current path must split. We need copies of the
                        // current path for each of the possible next nodes in the
                        // GFG. So if there are n next nodes in the GFG, we make
                        // n-1 copies of the current path, and put those in the
                        // toProcess queue. Then, we take the last of the next nodes,
                        // apply it to the current copy of this path, and keep going.
                        Map<CursorGrammarRule, GFGNode> nextNodes = lastStartEndNode.getNextNodes();
                        int numProcessed = 0;
                        for(CursorGrammarRule nextRule : nextNodes.keySet()) {
                            GFGNode nextNode = nextNodes.get(nextRule);
                            if(numProcessed < nextNodes.size() - 1) {
                                GFGPath copy = currentPath.copy();
                                copy.addNode(nextNode);
                                toProcess.add(copy);
                            } else {
                                // this is the nth node - we just apply it to the
                                // current path
                                currentPath.addNode(nextNode);
                            }
                            numProcessed++;
                        }
                    } else {
                        // this is the return step. Here we pop a CursorGrammarRule off our stack,
                        // which tells us where we're going back to. Then we can consult the end
                        // node and retrieve the correct next node to go to.
                        CursorGrammarRule returnRule = currentPath.peekCallStack();
                        if(returnRule == null) {
                            // The only time there shouldn't be a return address on the stack is
                            // when we've finished processing the start nonterminal. Assuming that's
                            // the case, we can just add our current path to the final set and
                            // be done.
                            extendedPaths.add(currentPath);
                            break;
                        }
                        currentPath.popCallStack();
                        currentPath.addNode(lastStartEndNode.getNextNodeForGrammarRule(returnRule));
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
                        currentPath.addNode(lastRuleNode.getNextNode());
                    } else {
                        GrammarElement nextElement = lastRule.getRightHandSide().get(cursorIndex);
                        if(nextElement.isNonterminal()) {
                            // this is the call step. Here we need to add the correct "return address"
                            // to our call stack. The way the GFG is structured, every end node knows every
                            // potential node it could return to, and has them indexed by their CursorGrammarRule.
                            // So we simply add the next CursorGrammarRule to our stack right now and then continue.
                            CursorGrammarRule nextCursorGrammarRule = new CursorGrammarRule(lastRule, cursorIndex + 1);
                            currentPath.pushCallStack(nextCursorGrammarRule);
                            // Now we just continue with the next node, which will start the "call"
                            currentPath.addNode(lastRuleNode.getNextNode());
                        } else {
                            // we've come up against a terminal, so we're done extending this path.
                            // just add it to the final set and we're done
                            extendedPaths.add(currentPath);
                            break;
                        }
                    }
                }
            }
        }

        return extendedPaths;
    }
}
