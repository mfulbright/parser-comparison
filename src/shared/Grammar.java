package shared;

import java.util.ArrayList;
import java.util.HashMap;

public class Grammar {

    private GrammarRule startRule;
    // The most common use for a Grammar object right now is querying for all
    // rules that have a given nonterminal on their left hand side, so this
    // is how we'll store the rules for now.
    private HashMap<Nonterminal, ArrayList<GrammarRule>> grammarRules;

    public Grammar(GrammarRule start) {
        startRule = start;
        grammarRules = new HashMap<>();
        ArrayList<GrammarRule> startRuleList = new ArrayList<>();
        startRuleList.add(startRule);
        grammarRules.put(startRule.getLeftHandSide(), startRuleList);
    }

    public Grammar(GrammarRule start, ArrayList<GrammarRule> rules) {
        startRule = start;
        grammarRules = new HashMap<>();
        for(GrammarRule rule : rules) {
            addRule(rule);
        }
        assert(grammarRules.get(startRule.getLeftHandSide()).size() == 1);
    }

    public void addRule(GrammarRule rule) {
        Nonterminal ruleLHS = rule.getLeftHandSide();
        if(grammarRules.containsKey(ruleLHS)) {
            ArrayList<GrammarRule> lhsRules = grammarRules.get(ruleLHS);
            lhsRules.add(rule);
        } else {
            ArrayList<GrammarRule> newLhsRules = new ArrayList<>();
            newLhsRules.add(rule);
            grammarRules.put(ruleLHS, newLhsRules);
        }
    }

    public GrammarRule getStartRule() {
        return startRule;
    }

    public ArrayList<GrammarRule> getRulesWithLeftHandSide(Nonterminal lhs) {
        return grammarRules.get(lhs);
    }
}
