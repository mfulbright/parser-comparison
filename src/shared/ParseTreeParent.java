package shared;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseTreeParent implements ParseTreeNode {

    // temp
    private static int idcount = 0;
    public String id;

    private ParseTreeParent parent;
    private Nonterminal nonterminal;
    private HashSet<List<ParseTreeNode>> childTrees;

    public ParseTreeParent(ParseTreeParent p, Nonterminal n) {
        id = "P" + idcount++;
        parent = p;
        nonterminal = n;
        childTrees = new HashSet<>();
    }

    public ParseTreeParent getParent() {
        return parent;
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

    public String toString() {
        String ret = nonterminal + " (" + id + "): ";
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
