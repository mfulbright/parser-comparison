package shared;

import earleyparser.SigmaSetEntry;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EarleyRunner {

    public static final String GRAMMAR_FILE_NAME = "grammarmod_grammar.txt";

    public static void main(String[] args) throws IOException {

        Scanner grammarFile = new Scanner(new File(GRAMMAR_FILE_NAME));

        // first the lexing section
        String line = grammarFile.nextLine();
        assert(line.equals("LEX"));

        // Map the names of symbols to the symbol instances
        HashMap<String, Symbol> symbols = new HashMap<>();
        // Map the names of terminals to the terminal instances
        HashMap<String, Terminal> terminals = new HashMap<>();
        line = grammarFile.nextLine();
        while(! line.equals("")) {
            // The format of each line should be "name = pattern"
            String name = line.substring(0, line.indexOf(" "));
            String pattern = line.substring(line.indexOf(" ") + 3);
            assert(! symbols.containsKey(name));
            Symbol newSymbol = new Symbol(name, pattern);
            symbols.put(name, newSymbol);
            terminals.put(name, new Terminal(newSymbol));
            line = grammarFile.nextLine();
        }

        line = grammarFile.nextLine();
        assert(line.equals("GRAMMAR"));

        // Map the names of nonterminals to the nonterminal instances
        HashMap<String, Nonterminal> nonterminals = new HashMap<>();
        // The first rule is the start rule
        GrammarRule startRule = parseGrammarRule(grammarFile.nextLine(), terminals, nonterminals);
        Grammar grammar = new Grammar(startRule);
        while(grammarFile.hasNextLine()) {
            grammar.addRule(parseGrammarRule(grammarFile.nextLine(), terminals, nonterminals));
        }

        // Compile the lexer patterns into a regex matcher
        StringBuffer combinedRegexBuffer = new StringBuffer();
        for(String name : symbols.keySet()) {
            Symbol symbol = symbols.get(name);
            combinedRegexBuffer.append(String.format("|(?<%s>%s)", name, symbol.getPattern()));
        }
        Pattern lexerPattern = Pattern.compile(combinedRegexBuffer.substring(1));

        Scanner input = new Scanner(System.in);
        InputLoop:
        while(true) {
            System.out.println("Enter a line of text to recognize:");

            String inputLine = input.nextLine();
            if(inputLine.equals("END")) {
                break;
            }

            // First tokenize the input line
            ArrayList<Token> tokens = new ArrayList<>();
            Matcher inputMatcher = lexerPattern.matcher(inputLine);
            int lastIndexMatched = 0;
            while(inputMatcher.find()) {
                if(inputMatcher.start() != lastIndexMatched) {
                    System.out.println("That line failed to be tokenized");
                    continue InputLoop;
                }
                for(String name : symbols.keySet()) {
                    if(inputMatcher.group(name) != null) {
                        Token matchedToken = new Token(inputMatcher.group(name), symbols.get(name));
                        tokens.add(matchedToken);
                        break;
                    }
                }
                lastIndexMatched = inputMatcher.end();
            }
            if(lastIndexMatched != inputLine.length()) {
                System.out.println("That line failed to be tokenized");
                continue;
            }

            ParseTreeNode recognizingResult = recognizes(tokens, grammar);

            if(recognizingResult == null) {
                System.out.println("That line is not in the language");
            } else {
                System.out.println("That line is in the language");
                printParseTree(recognizingResult);
            }
        }
    }

    public static GrammarRule parseGrammarRule(String grammarRuleLine, HashMap<String, Terminal> terminals, HashMap<String, Nonterminal> nonterminals) {
        // The format of each line should be "Nonterminal = GrammarElement GrammarElement GrammarElement"
        String[] pieces = grammarRuleLine.split(" ");
        String nonterminalName = pieces[0];
        Nonterminal lhsNonterminal;
        if(nonterminals.containsKey(nonterminalName)) {
            lhsNonterminal = nonterminals.get(nonterminalName);
        } else {
            lhsNonterminal = new Nonterminal(nonterminalName);
            nonterminals.put(nonterminalName, lhsNonterminal);
        }
        ArrayList<GrammarElement> ruleRightHandSide = new ArrayList<>();
        // pieces[1] will be the '='
        for(int rhsIndex = 2; rhsIndex < pieces.length; rhsIndex++) {
            String grammarElementName = pieces[rhsIndex];
            if(terminals.containsKey(grammarElementName)) {
                ruleRightHandSide.add(terminals.get(grammarElementName));
            } else {
                Nonterminal rhsNonterminal;
                if(nonterminals.containsKey(grammarElementName)) {
                    rhsNonterminal = nonterminals.get(grammarElementName);
                } else {
                    rhsNonterminal = new Nonterminal(grammarElementName);
                    nonterminals.put(grammarElementName, rhsNonterminal);
                }
                ruleRightHandSide.add(rhsNonterminal);
            }
        }
        return new GrammarRule(lhsNonterminal, ruleRightHandSide);
        /*
        // If this is the first rule, it must be the start rule
        if(startRule == null) {
            startRule = newRule;
        }
        // Add the new rule into the grammarRules map
        if(grammarRules.containsKey(lhsNonterminal)) {
            ArrayList<GrammarRule> nonterminalRules = grammarRules.get(lhsNonterminal);
            nonterminalRules.add(newRule);
        } else {
            ArrayList<GrammarRule> nonterminalRules = new ArrayList<>();
            nonterminalRules.add(newRule);
            grammarRules.put(lhsNonterminal, nonterminalRules);
        }
        */

    }

    /* Returns null if the line cannot be recognized */
    public static ParseTreeNode recognizes(ArrayList<Token> tokens, Grammar grammar) {
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

    public static void fillSigmaSet(Grammar grammar, ArrayList<HashSet<SigmaSetEntry>> sigmaSets, int currentSigmaSetIndex, ArrayDeque<SigmaSetEntry> toProcess) {
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

    public static void printParseTree(ParseTreeNode root) {
        printParseTreeHelper("", root);
    }

    public static void printParseTreeHelper(String prefix, ParseTreeNode root) {
        System.out.print(prefix);
        System.out.println(root);
        if(! root.isLeafNode()) {
            ParseTreeParent parent = (ParseTreeParent) root;
            for(ParseTreeNode child : parent.getChildren()) {
                printParseTreeHelper(prefix + " ", child);
            }
        }
    }

}
