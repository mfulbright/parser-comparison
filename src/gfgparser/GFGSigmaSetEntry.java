package gfgparser;

import java.util.Objects;

public class GFGSigmaSetEntry {

    private GFGNode node;
    private int tag;

    public GFGSigmaSetEntry(GFGNode n, int t) {
        node = n;
        tag = t;
    }

    public GFGNode getGFGNode() {
        return node;
    }

    public int getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof GFGSigmaSetEntry)) {
            return false;
        }
        GFGSigmaSetEntry otherSigmaSetEntry = (GFGSigmaSetEntry) other;
        return node.equals(otherSigmaSetEntry.node) &&
                tag == otherSigmaSetEntry.tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, tag);
    }

    @Override
    public String toString() {
        return "<" + node.toString() + ", " + tag + ">";
    }
}
