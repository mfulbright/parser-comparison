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
        HashMap<Nonterminal, ArrayList<GrammarRule>> grammarRules = new HashMap<>();
        GrammarRule startRule = null; // The first rule in the grammar must be the start rule
        while(grammarFile.hasNextLine()) {
            line = grammarFile.nextLine();
            // The format of each line should be "Nonterminal = GrammarElement GrammarElement GrammarElement"
            String[] pieces = line.split(" ");
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
            GrammarRule newRule = new GrammarRule(lhsNonterminal, ruleRightHandSide);
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
        }
        assert(startRule != null);
        // The start rule must be the only rule with that nonterminal
        ArrayList<GrammarRule> startSymbolRules = grammarRules.get(startRule.getLeftHandSide());
        assert(startSymbolRules.size() == 1);

        // Compile the lexer patterns into a regex matcher
        StringBuffer combinedRegexBuffer = new StringBuffer();
        for(String name : symbols.keySet()) {
            Symbol symbol = symbols.get(name);
            combinedRegexBuffer.append(String.format("|(?<%s>%s)", name, symbol.getPattern()));
        }
        Pattern lexerPattern = Pattern.compile(combinedRegexBuffer.substring(1));

		Scanner input = new Scanner(System.in);
		while(true) {
			System.out.println("Enter a line of text to recognize:");
			
			String inputLine = input.nextLine();
			if(inputLine.equals("END")) {
				break;
			}

            // First tokenize the input line
            ArrayList<Token> tokens = new ArrayList<>();
            Matcher inputMatcher = lexerPattern.matcher(inputLine);
            while(inputMatcher.find()) {
                for(String name : symbols.keySet()) {
                    if(inputMatcher.group(name) != null) {
                        Token matchedToken = new Token(inputMatcher.group(name), symbols.get(name));
                        tokens.add(matchedToken);
                        break;
                    }
                }
            }

			ParseTreeNode recognizingResult = recognizes(grammarRules, startRule, tokens);
			
			if(recognizingResult == null) {
				System.out.println("That line is not in the language");
			} else {
				System.out.println("That line is in the language");
				printParseTree(recognizingResult);
			}
		}
	}
	
	/* Returns null if the line cannot be recognized */
	public static ParseTreeNode recognizes(HashMap<Nonterminal, ArrayList<GrammarRule>> grammarRules, GrammarRule startRule, ArrayList<Token> tokens) {
		// keep a list of sigma sets. In this list, index j will correspond to
		// the sigma set right before the jth token.
		ArrayList<HashSet<SigmaSetEntry>> sigmaSets = new ArrayList<>();

		// set up the first sigma set
		HashSet<SigmaSetEntry> sigmaSet0 = new HashSet<SigmaSetEntry>();
		sigmaSets.add(sigmaSet0);
		ArrayDeque<SigmaSetEntry> sigmaSet0ToProcess = new ArrayDeque<>();
		SigmaSetEntry startRuleEntry = new SigmaSetEntry(startRule, 0, 0);
		sigmaSet0ToProcess.add(startRuleEntry);
		fillSigmaSet(grammarRules, sigmaSets, 0, sigmaSet0ToProcess);
		
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

			fillSigmaSet(grammarRules, sigmaSets, tokenIndex + 1, toProcess);
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
	
	public static void fillSigmaSet(HashMap<Nonterminal, ArrayList<GrammarRule>> grammarRules, ArrayList<HashSet<SigmaSetEntry>> sigmaSets, int currentSigmaSetIndex, ArrayDeque<SigmaSetEntry> toProcess) {
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
					ArrayList<GrammarRule> callableGrammarRules = grammarRules.get(nonterminalAfterCursor);
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

    public static class Symbol {

        private String name;
        private String pattern;

        public Symbol(String n, String p) {
            name = n;
            pattern = p;
        }

        public String getName() {
            return name;
        }

        public String getPattern() {
            return pattern;
        }

        @Override
        public boolean equals(Object other) {
            if(! (other instanceof Symbol)) {
                return false;
            }
            Symbol otherSymbol = (Symbol) other;
            // TODO: should I check the pattern here too?
            return name.equals(otherSymbol.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 41 + pattern.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
	
	// TODO: Should probably override equals and hashCode here
	public static class Token {
		
		private String text;
		private Symbol type;
		
		public Token(String te, Symbol ty) {
			text = te;
			type = ty;
		}
		
		public String getText() {
			return text;
		}
		
		public Symbol getType() {
			return type;
		}
		
		@Override
		public String toString() {
			return "<" + type + ": " + text + ">";
		}
	}

	public static interface GrammarElement {
		public boolean isNonterminal();
	}
	
	public static class Nonterminal implements GrammarElement {
		
		private String name;
		
		public Nonterminal(String n) {
			name = n;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isNonterminal() {
			return true;
		}
		
		@Override
		public boolean equals(Object other) {
			if(! (other instanceof Nonterminal)) {
				return false;
			}
			Nonterminal otherNonterminal = (Nonterminal) other;
			return name.equals(otherNonterminal.name);
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public static class Terminal implements GrammarElement {
		
		private Symbol symbol;
		
		public Terminal(Symbol s) {
			symbol = s;
		}
		
		public Symbol getSymbol() {
			return symbol;
		}
		
		public boolean isNonterminal() {
			return false;
		}
		
		@Override
		public boolean equals(Object other) {
			if(! (other instanceof Terminal)) {
				return false;
			}
			Terminal otherTerminal = (Terminal) other;
			return symbol == otherTerminal.symbol;
		}
		
		@Override
		public int hashCode() {
			return symbol.hashCode();
		}
		
		@Override
		public String toString() {
			return symbol.toString();
		}
	}
	
	public static class GrammarRule {
		
		private Nonterminal leftHandSide;
		private ArrayList<GrammarElement> rightHandSide;
		
		public GrammarRule(Nonterminal left) {
			this(left, new ArrayList<GrammarElement>());
		}
		
		public GrammarRule(Nonterminal left, ArrayList<GrammarElement> right) {
			leftHandSide = left;
			rightHandSide = right;
		}
		
		public Nonterminal getLeftHandSide() {
			return leftHandSide;
		}
		
		public ArrayList<GrammarElement> getRightHandSide() {
			return rightHandSide;
		}
		
		@Override
		public boolean equals(Object other) {
			if(! (other instanceof GrammarRule)) {
				return false;
			}
			GrammarRule otherGrammarRule = (GrammarRule) other;
			return leftHandSide.equals(otherGrammarRule.leftHandSide) &&
					rightHandSide.equals(otherGrammarRule.rightHandSide);
		}
		
		@Override
		public int hashCode() {
			int hash = leftHandSide.hashCode();
			hash *= 17;
			hash += rightHandSide.hashCode();
			return hash;
		}
		
		@Override
		public String toString() {
			String rhs = "";
			for(GrammarElement gE : rightHandSide) {
				rhs += gE + " ";
			}
			rhs = rhs.substring(0, rhs.length() - 1);
			return leftHandSide + " -> " + rhs;
		}
	}
	
	public static class SigmaSetEntry {
		
		private GrammarRule rule;
		// this will be in the range [0, rule.getRightHandSide().size()]
		private int cursorIndex;
		private int tag;
		
		public SigmaSetEntry(GrammarRule r, int cI, int t) {
			rule = r;
			cursorIndex = cI;
			tag = t;
		}
		
		public GrammarRule getGrammarRule() {
			return rule;
		}
		
		public int getCursorIndex() {
			return cursorIndex;
		}
		
		public int getTag() {
			return tag;
		}
		
		@Override
		public boolean equals(Object other) {
			if(! (other instanceof SigmaSetEntry)) {
				return false;
			}
			SigmaSetEntry otherSigmaSetEntry = (SigmaSetEntry) other;
			return rule.equals(otherSigmaSetEntry.rule) && 
					cursorIndex == otherSigmaSetEntry.cursorIndex &&
					tag == otherSigmaSetEntry.tag;
		}
		
		@Override
		public int hashCode() {
			int hash = rule.hashCode();
			hash *= 31;
			hash += cursorIndex;
			hash *= 31;
			hash += tag;
			return hash;
		}
		
		@Override
		public String toString() {
			String rhs = "";
			ArrayList<GrammarElement> ruleRHS = rule.getRightHandSide();
			for(int i = 0; i < cursorIndex; i++) {
				rhs += ruleRHS.get(i) + " ";
			}
			rhs += ". ";
			for(int i = cursorIndex; i < ruleRHS.size(); i++) {
				rhs += ruleRHS.get(i) + " ";
			}
			rhs = rhs.substring(0, rhs.length() - 1);
			
			return "<" + rule.getLeftHandSide() + " -> " + rhs + ", " + tag + ">";
		}
	}
	
	public static interface ParseTreeNode {
		public boolean isLeafNode();
	}
	
	public static class ParseTreeParent implements ParseTreeNode {
		
		private ParseTreeParent parent;
		private GrammarRule grammarRule;
		private ArrayList<ParseTreeNode> children;
		
		public ParseTreeParent(ParseTreeParent p, GrammarRule g) {
			parent = p;
			grammarRule = g;
			children = new ArrayList<ParseTreeNode>();
		}
		
		public boolean isLeafNode() {
			return false;
		}
		
		public ParseTreeParent getParent() {
			return parent;
		}
		
		public ArrayList<ParseTreeNode> getChildren() {
			return children;
		}
		
		@Override
		public String toString() {
			return grammarRule.toString();
		}
	}
	
	public static class ParseTreeLeaf implements ParseTreeNode {
		
		private ParseTreeParent parent;
		private Token token;
		
		public ParseTreeLeaf(ParseTreeParent p, Token t) {
			parent = p;
			token = t;
		}
		
		public boolean isLeafNode() {
			return true;
		}
		
		@Override
		public String toString() {
			return token.toString();
		}
	}
}
