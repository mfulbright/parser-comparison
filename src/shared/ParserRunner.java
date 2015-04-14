package shared;

import gfgparser.GFGParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParserRunner {

    public static final String GRAMMAR_FILE_NAME = "grammar.txt";

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

        Parser parser = new GFGParser(grammar);

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

            ParseTreeNode parsingResult = parser.parse(tokens);

            if(parsingResult == null) {
                System.out.println("That line is not in the language");
            } else {
                System.out.println("That line is in the language");
                printParseTree(parsingResult);
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
