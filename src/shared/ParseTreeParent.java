package shared;

import java.util.ArrayList;
import java.util.List;

public class ParseTreeParent implements ParseTreeNode {

    private ParseTreeParent parent;
    private Nonterminal nonterminal;
    private ArrayList<ParseTreeNode> children;

    public ParseTreeParent(ParseTreeParent p, Nonterminal n) {
        parent = p;
        nonterminal = n;
        children = new ArrayList<>();
    }

    public ParseTreeParent getParent() {
        return parent;
    }

    public Nonterminal getNonterminal() {
        return nonterminal;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }

    public String toString() {
        String rhs = "";
        for(int i = 0; i < children.size(); i++) {
            ParseTreeNode child = children.get(i);
            if(child instanceof ParseTreeParent) {
                rhs += ((ParseTreeParent) child).getNonterminal() + " ";
            } else {
                rhs += ((ParseTreeLeaf) child).getSymbol() + " ";
            }
        }
        rhs = rhs.substring(0, rhs.length() - 1);
        return nonterminal + " -> " + rhs;
    }
}
