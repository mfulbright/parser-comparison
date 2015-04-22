package shared;

import earleyparser.EarleyParser;
import gfgparser.GFGParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
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
        ArrayList<Symbol> symbols = new ArrayList<>();
        ArrayList<String> symbolNames = new ArrayList<>();
        // Map the names of terminals to the terminal instances
        HashMap<String, Terminal> terminals = new HashMap<>();
        line = grammarFile.nextLine();
        while(! line.equals("")) {
            // The format of each line should be "name = pattern"
            String name = line.substring(0, line.indexOf(" "));
            String pattern = line.substring(line.indexOf(" ") + 3);
            assert(! symbolNames.contains(name));
            Symbol newSymbol = new Symbol(name, pattern);
            symbols.add(newSymbol);
            symbolNames.add(name);
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
        for(int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            String symbolName = symbolNames.get(i);
            combinedRegexBuffer.append(String.format("|\\s*(?<%s>%s)", symbolName, symbol.getPattern()));
        }
        Pattern lexerPattern = Pattern.compile(combinedRegexBuffer.substring(1));

        Parser earleyParser = new EarleyParser(grammar);
        Parser gfgParser = new GFGParser(grammar);

        Scanner input = new Scanner(System.in);
        InputLoop:
        while(true) {
            System.out.println("Enter a line of text to parse:");

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
                for(int i = 0; i < symbols.size(); i++) {
                    String symbolName = symbolNames.get(i);
                    if(inputMatcher.group(symbolName) != null) {
                        Symbol symbol = symbols.get(i);
                        Token matchedToken = new Token(inputMatcher.group(symbolName), symbol);
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

            ParseTreeNode earleyResult = earleyParser.parse(tokens);
            ParseTreeNode gfgResult = gfgParser.parse(tokens);
            if( (earleyResult == null && gfgResult == null) ||
                    earleyResult.equals(gfgResult)) {
                System.out.println("The parsers returned the same trees");
            } else {
                System.out.println("ERROR: The parsers returned different trees");
                System.out.println("Earley tree:");
                if(earleyResult == null) {
                    System.out.println(earleyResult);
                } else {
                    printParseTree(earleyResult);
                }
                System.out.println();
                System.out.println("GFG tree:");
                if(gfgResult == null) {
                    System.out.println(gfgResult);
                } else {
                    printParseTree(gfgResult);
                }
                continue;
            }

            if(earleyResult == null) {
                System.out.println("That line is not in the language");
            } else {
                printAllParseTrees(earleyResult);
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

    public static void printParseTreeHelper(String prefix, ParseTreeNode node) {
        if(node instanceof ParseTreeLeaf) {
            System.out.println(prefix + node);
        } else {
            ParseTreeParent parent = (ParseTreeParent) node;
            Set<List<ParseTreeNode>> childTrees = parent.getChildTrees();
            if(childTrees.size() == 1) {
                List<ParseTreeNode> children = childTrees.iterator().next();
                System.out.print(prefix);
                System.out.print(parent.getNonterminal());
                System.out.print(" -> ");
                for(ParseTreeNode child : children) {
                    if(child instanceof ParseTreeParent) {
                        System.out.print(((ParseTreeParent) child).getNonterminal() + " ");
                    } else {
                        System.out.print(((ParseTreeLeaf) child).getSymbol() + " ");
                    }
                }
                System.out.println();
                for(ParseTreeNode child : children) {
                    printParseTreeHelper(prefix + " ", child);
                }
            } else {
                System.out.print(prefix);
                System.out.println(parent.getNonterminal());
                int i = 1;
                for(List<ParseTreeNode> childTree : childTrees) {
                    String rhs = "";
                    for(ParseTreeNode child : childTree) {
                        if(child instanceof ParseTreeParent) {
                            rhs += ((ParseTreeParent) child).getNonterminal() + " ";
                        } else {
                            rhs += ((ParseTreeLeaf) child).getSymbol() + " ";
                        }
                    }
                    System.out.println(prefix + i++ + ": " + rhs);
                    for(ParseTreeNode child : childTree) {
                        printParseTreeHelper(prefix + " ", child);
                    }
                }
            }
        }
    }

    public static void printAllParseTrees(ParseTreeNode root) {
        List<String> parses = getAllParseStrings("", root);
        System.out.println(parses.size() + " parse tree" + (parses.size() == 1 ? "" : "s"));
        for(String parse : parses) {
            System.out.println(parse);
        }
    }

    public static List<String> getAllParseStrings(String prefix, ParseTreeNode node) {
        if(node instanceof ParseTreeLeaf) {
            return Arrays.asList(prefix + node + "\n");
        } else {
            ParseTreeParent parent = (ParseTreeParent) node;
            List<String> parses = new ArrayList<>();
            for(List<ParseTreeNode> childTree : parent.getChildTrees()) {
                String header = parent.getNonterminal() + " -> ";
                for(ParseTreeNode childNode : childTree) {
                    if(childNode instanceof ParseTreeParent) {
                        header += ((ParseTreeParent) childNode).getNonterminal() + " ";
                    } else {
                        header += ((ParseTreeLeaf) childNode).getSymbol() + " ";
                    }
                }
                header = prefix + header.substring(0, header.length() - 1) + "\n";
                ArrayList<List<String>> childStrings = new ArrayList<>(childTree.size());
                for(int i = 0; i < childTree.size(); i++) {
                    childStrings.add(getAllParseStrings(prefix + " ", childTree.get(i)));
                }

                ArrayList<Integer> stack = new ArrayList<>();
                for(int i = 0; i < childStrings.size(); i++) {
                    stack.add(0);
                }
                while(true) {
                    // print
                    String parse = "";
                    for(int i = 0; i < stack.size(); i++) {
                        parse += childStrings.get(i).get(stack.get(i));
                    }
                    parses.add(header + parse);
                    // modify
                    while(! stack.isEmpty() && stack.get(stack.size() - 1) == childStrings.get(stack.size() - 1).size() - 1) {
                        stack.remove(stack.size() - 1);
                    }
                    if(stack.isEmpty()) {
                        break;
                    }
                    stack.set(stack.size() - 1, stack.get(stack.size() - 1) + 1);
                    while(stack.size() < childStrings.size()) {
                        stack.add(0);
                    }
                }
            }
            return parses;
        }
    }
}
