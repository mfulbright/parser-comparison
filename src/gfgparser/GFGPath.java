package gfgparser;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class GFGPath {

    private ArrayList<GFGNode> path;
    private ArrayDeque<CursorGrammarRule> callStack;

    public GFGPath() {
        path = new ArrayList<>();
        callStack = new ArrayDeque<>();
    }

    public GFGNode getLastNode() {
        return path.get(path.size() - 1);
    }

    public void addNode(GFGNode nextInPath) {
        path.add(nextInPath);
    }

    public ArrayList<GFGNode> getPath() {
        return path;
    }

    public void pushCallStack(CursorGrammarRule toAdd) {
        callStack.push(toAdd);
    }

    public CursorGrammarRule peekCallStack() {
        return callStack.peek();
    }

    public CursorGrammarRule popCallStack() {
        return callStack.pop();
    }

    public GFGPath copy() {
        GFGPath copy = new GFGPath();
        copy.path = new ArrayList<>(this.path);
        copy.callStack = new ArrayDeque<>(this.callStack);
        return copy;
    }
}
