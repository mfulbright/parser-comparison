package gfgparser;

import java.util.Objects;

public class OldGFGSigmaSetEntry {

    private OldGFGNode node;
    private int tag;

    public OldGFGSigmaSetEntry(OldGFGNode n, int t) {
        node = n;
        tag = t;
    }

    public OldGFGNode getGFGNode() {
        return node;
    }

    public int getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof OldGFGSigmaSetEntry)) {
            return false;
        }
        OldGFGSigmaSetEntry otherSigmaSetEntry = (OldGFGSigmaSetEntry) other;
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
