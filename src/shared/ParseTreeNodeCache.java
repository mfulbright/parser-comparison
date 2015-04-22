package shared;

import java.util.HashMap;
import java.util.Objects;

public class ParseTreeNodeCache {

    private HashMap<ParseTreeNodeKey, ParseTreeNode> cache;

    public ParseTreeNodeCache() {
        cache = new HashMap<>();
    }

    // For all methods dealing with parents (containsParent, addParent,
    // and getParent), the indices are inclusive on both sides, i.e.
    // [start, end]
    public boolean containsParent(int start, int end, Nonterminal nonterminal) {
        return cache.containsKey(new ParseTreeNodeKey(start, end, nonterminal));
    }

    // Leaves can be completely identified by the index of their token
    // in the tokens list, so there's no point in taking other
    // parameters
    public boolean containsLeaf(int index) {
        return cache.containsKey(new ParseTreeNodeKey(index, -1, null));
    }

    public void addParent(int start, int end, Nonterminal nonterminal, ParseTreeParent parent) {
        cache.put(new ParseTreeNodeKey(start, end, nonterminal), parent);
    }

    public void addLeaf(int index, ParseTreeLeaf leaf) {
        cache.put(new ParseTreeNodeKey(index, -1, null), leaf);
    }

    public ParseTreeParent getParent(int start, int end, Nonterminal nonterminal) {
        return (ParseTreeParent) cache.get(new ParseTreeNodeKey(start, end, nonterminal));
    }

    public ParseTreeLeaf getLeaf(int index) {
        return (ParseTreeLeaf) cache.get(new ParseTreeNodeKey(index, -1, null));
    }

    private class ParseTreeNodeKey {

        public int startIndex;
        public int endIndex;
        public GrammarElement element;

        public ParseTreeNodeKey(int s, int e, GrammarElement gE) {
            startIndex = s;
            endIndex = e;
            element = gE;
        }

        @Override
        public boolean equals(Object other) {
            if(! (other instanceof ParseTreeNodeKey)) {
                return false;
            }
            ParseTreeNodeKey otherKey = (ParseTreeNodeKey) other;
            return otherKey.startIndex == startIndex &&
                    otherKey.endIndex == endIndex &&
                    (otherKey.element == null ? element == null : otherKey.element.equals(element));
        }

        @Override
        public int hashCode() {
            return Objects.hash(startIndex, endIndex, element);
        }
    }
}
