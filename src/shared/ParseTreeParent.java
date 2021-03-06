package shared;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ParseTreeParent implements ParseTreeNode {

    private Nonterminal nonterminal;
    private HashSet<List<ParseTreeNode>> childTrees;

    public ParseTreeParent(Nonterminal n) {
        nonterminal = n;
        childTrees = new HashSet<>();
    }

    public Nonterminal getNonterminal() {
        return nonterminal;
    }

    public void addChildTree(List<ParseTreeNode> childTree) {
        childTrees.add(childTree);
    }

    public Set<List<ParseTreeNode>> getChildTrees() {
        return childTrees;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof ParseTreeParent)) {
            return false;
        }
        ParseTreeParent otherParent = (ParseTreeParent) other;
        return otherParent.nonterminal.equals(nonterminal) &&
                otherParent.childTrees.equals(childTrees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonterminal, childTrees);
    }

    public String toString() {
        String ret = nonterminal + ": ";
        int childCount = 1;
        for(List<ParseTreeNode> childTree : childTrees) {
            String childString = "";
            for(int i = 0; i < childTree.size(); i++) {
                ParseTreeNode child = childTree.get(i);
                if(child instanceof ParseTreeParent) {
                    childString += ((ParseTreeParent) child).getNonterminal() + " ";
                } else {
                    childString += ((ParseTreeLeaf) child).getSymbol() + " ";
                }
            }
            childString = childString.substring(0, childString.length() - 1);
            ret += childCount++ + " " + childString + "; ";
        }
        return ret;
    }
}
