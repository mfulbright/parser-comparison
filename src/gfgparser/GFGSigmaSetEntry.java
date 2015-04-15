package gfgparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GFGSigmaSetEntry {

    private GFGNode node;
    private int tag;
    private ArrayList<GFGSigmaSetEntry> precedingEntries;

    public GFGSigmaSetEntry(GFGNode n, int t) {
        node = n;
        tag = t;
        precedingEntries = new ArrayList<>();
    }

    public GFGSigmaSetEntry(GFGNode n, int t, GFGSigmaSetEntry precedingEntry) {
        node = n;
        tag = t;
        precedingEntries = new ArrayList<>();
        precedingEntries.add(precedingEntry);
    }

    public GFGNode getNode() {
        return node;
    }

    public int getTag() {
        return tag;
    }

    public void addPrecedingEntry(GFGSigmaSetEntry precedingEntry) {
        precedingEntries.add(precedingEntry);
    }

    public List<GFGSigmaSetEntry> getPrecedingEntries() {
        return precedingEntries;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof GFGSigmaSetEntry)) {
            return false;
        }
        GFGSigmaSetEntry otherGFGSigmaSetEntry = (GFGSigmaSetEntry) other;
        return node.equals(otherGFGSigmaSetEntry.node) &&
                tag == otherGFGSigmaSetEntry.tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, tag);
    }

    public String toString() {
        return "<" + node + ", " + tag + ">";
    }
}
