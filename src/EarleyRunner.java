import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;


public class EarleyRunner {

	public static void main(String[] args) {
		
		Scanner input = new Scanner(System.in);
		while(true) {
			System.out.println("Enter a line of text to recognize:");
			
			String line = input.nextLine();
			if(line.equals("END")) {
				break;
			}
			
			ParseTreeNode recognizingResult = recognizes(line);
			
			if(recognizingResult == null) {
				System.out.println("That line is not in the language");
			} else {
				System.out.println("That line is in the language");
				printParseTree(recognizingResult);
			}
		}
	}
	
	/* Returns null if the line cannot be recognized */
	public static ParseTreeNode recognizes(String line) {
		ArrayList<Token> tokens = new ArrayList<>();
		int currentIndex = 0;
		while(currentIndex < line.length()) {
			char currentCharacter = line.charAt(currentIndex);
			if(currentCharacter == '(') {
				Token currentToken = new Token("(", Symbol.OPEN_PARENTHESIS);
				tokens.add(currentToken);
				currentIndex++;
			} else if(currentCharacter == ')') {
				Token currentToken = new Token(")", Symbol.CLOSE_PARENTHESIS);
				tokens.add(currentToken);
				currentIndex++;	
			} else if(currentCharacter == '+') {
				Token currentToken = new Token("+", Symbol.PLUS);
				tokens.add(currentToken);
				currentIndex++;	
			} else if(Character.isDigit(currentCharacter)) {
				int startingIndex = currentIndex;
				while(currentIndex < line.length() && Character.isDigit(line.charAt(currentIndex))) {
					currentIndex++;
				}
				Token currentToken = new Token(line.substring(startingIndex, currentIndex), Symbol.INT);
				tokens.add(currentToken);
				// no currentIndex++ here
			} else {
				throw new RuntimeException("Encountered unknown symbol while reading input: " + line.charAt(currentIndex));
			}
		}
		
		// build the grammar
		// first, create all the terminals and non-terminals
		Terminal openParenthesisTerminal = new Terminal(Symbol.OPEN_PARENTHESIS);
		Terminal closeParenthesisTerminal = new Terminal(Symbol.CLOSE_PARENTHESIS);
		Terminal plusTerminal = new Terminal(Symbol.PLUS);
		Terminal intTerminal = new Terminal(Symbol.INT);
		Nonterminal sNonterminal = new Nonterminal("S");
		Nonterminal eNonterminal = new Nonterminal("E");
		// now build the rules
		// S -> E
		ArrayList<GrammarElement> startRuleRHS = new ArrayList<>();
		startRuleRHS.add(eNonterminal);
		GrammarRule startRule = new GrammarRule(sNonterminal, startRuleRHS);
		// E -> INT
		ArrayList<GrammarElement> intRuleRHS = new ArrayList<>();
		intRuleRHS.add(intTerminal);
		GrammarRule intRule = new GrammarRule(eNonterminal, intRuleRHS);
		// E -> (E+E)
		ArrayList<GrammarElement> parenthesizedRuleRHS = new ArrayList<>();
		parenthesizedRuleRHS.add(openParenthesisTerminal);
		parenthesizedRuleRHS.add(eNonterminal);
		parenthesizedRuleRHS.add(plusTerminal);
		parenthesizedRuleRHS.add(eNonterminal);
		parenthesizedRuleRHS.add(closeParenthesisTerminal);
		GrammarRule parenthesizedRule = new GrammarRule(eNonterminal, parenthesizedRuleRHS);
		// E -> E+E
		ArrayList<GrammarElement> plusRuleRHS = new ArrayList<>();
		plusRuleRHS.add(eNonterminal);
		plusRuleRHS.add(plusTerminal);
		plusRuleRHS.add(eNonterminal);
		GrammarRule plusRule = new GrammarRule(eNonterminal, plusRuleRHS);
		// combine them into a single collection. organize the rules by the non-terminals on
		// their left hand sides, for fast lookup later
		HashMap<Nonterminal, ArrayList<GrammarRule>> grammarRules = new HashMap<>();
		// first the only rule we have for S: S -> E
		ArrayList<GrammarRule> sNonterminalRules = new ArrayList<>();
		sNonterminalRules.add(startRule);
		grammarRules.put(sNonterminal, sNonterminalRules);
		// and then the three rules for E: E -> INT | (E+E) | E+E
		ArrayList<GrammarRule> eNonterminalRules = new ArrayList<>();
		eNonterminalRules.add(intRule);
		eNonterminalRules.add(parenthesizedRule);
		eNonterminalRules.add(plusRule);
		grammarRules.put(eNonterminal, eNonterminalRules);
		
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
	
	public static enum Symbol {
		INT, OPEN_PARENTHESIS, CLOSE_PARENTHESIS, PLUS 
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
			return symbol.toString().toLowerCase();
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
