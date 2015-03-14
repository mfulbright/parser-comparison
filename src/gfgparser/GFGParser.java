package gfgparser;

import shared.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GFGParser implements Parser {

    private Grammar grammar;

    public GFGParser(Grammar g) {
        setGrammar(g);
    }

    @Override
    public void setGrammar(Grammar g) {
        grammar = g;
        // build the GFG
        // first, build all the start and end nodes
        HashMap<Nonterminal, StartEndGFGNode> startNodes = new HashMap<>();
        HashMap<Nonterminal, StartEndGFGNode> endNodes = new HashMap<>();
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
        return null;
    }
}
