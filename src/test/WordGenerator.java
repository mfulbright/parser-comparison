package test;

import shared.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WordGenerator {

    public static final String TEST_GRAMMAR_FILE_NAME = "test_grammar.txt";

    public static void main(String[] args) throws IOException {

        Scanner grammarFile = new Scanner(new File(TEST_GRAMMAR_FILE_NAME));

        // first the terminals section
        String line = grammarFile.nextLine();
        assert(line.equals("TERMINALS"));

        // Map the names of terminals to the terminal instances
        HashMap<String, Terminal> namesToTerminals = new HashMap<>();
        line = grammarFile.nextLine();
        while(! line.equals("")) {
            // The format of each line should be "name = pattern"
            String name = line.substring(0, line.indexOf(" "));
            String pattern = line.substring(line.indexOf(" ") + 3);
            Symbol newSymbol = new Symbol(name, pattern);
            namesToTerminals.put(name, new Terminal(newSymbol));
            line = grammarFile.nextLine();
        }

        line = grammarFile.nextLine();
        assert(line.equals("GRAMMAR"));

        // Map the names of nonterminals to the nonterminal instances
        HashMap<String, Nonterminal> namesToNonterminals = new HashMap<>();
        // The first rule is the start rule
        GrammarRule startRule = ParserRunner.parseGrammarRule(grammarFile.nextLine(), namesToTerminals, namesToNonterminals);
        Grammar grammar = new Grammar(startRule);
        while(grammarFile.hasNextLine()) {
            grammar.addRule(ParserRunner.parseGrammarRule(grammarFile.nextLine(), namesToTerminals, namesToNonterminals));
        }

        // Now go ahead and make sets for the terminals and nonterminals
        HashSet<Terminal> terminals = new HashSet<>();
        for(String name : namesToTerminals.keySet()) {
            terminals.add(namesToTerminals.get(name));
        }
        HashSet<Nonterminal> nonterminals = new HashSet<>();
        for(String name : namesToNonterminals.keySet()) {
            nonterminals.add(namesToNonterminals.get(name));
        }

        // Generate the graph of the grammar
        // First, create nodes for all the nonterminals
        HashMap<Nonterminal, TarjanNode> nonterminalsToNodes = new HashMap<>();
        for(Nonterminal nonterminal: nonterminals) {
            nonterminalsToNodes.put(nonterminal, new TarjanNode(nonterminal));
        }
        // Now map nodes to nodes they connect to
        HashMap<TarjanNode, HashSet<TarjanNode>> graphEdges =
                new HashMap<>();
        for(Nonterminal nonterminal: nonterminals) {
            List<GrammarRule> nonterminalRules =
                    grammar.getRulesWithLeftHandSide(nonterminal);
            HashSet<TarjanNode> connectedNodes = new HashSet<>();
            for(GrammarRule rule : nonterminalRules) {
                for(GrammarElement element : rule.getRightHandSide()) {
                    if(element instanceof Terminal) {
                        continue;
                    }
                    TarjanNode rhsNode = nonterminalsToNodes.get(element);
                    connectedNodes.add(rhsNode);
                }
            }
            TarjanNode nonterminalNode = nonterminalsToNodes.get(nonterminal);
            graphEdges.put(nonterminalNode, connectedNodes);
        }

        // Now run Tarjan's algorithm
        MutableInt index = new MutableInt();
        Deque<TarjanNode> stack = new ArrayDeque<TarjanNode>();
        HashSet<HashSet<TarjanNode>> stronglyConnectedComponents = new
                HashSet<>();
        for(TarjanNode node : graphEdges.keySet()) {
            if(node.index == -1) {
                strongConnect(node, graphEdges, index, stack, stronglyConnectedComponents);
            }
        }

        // Now we have the graph partitioned into strongly connected
        // components. We can now combine all nodes in cycles into one
        // set
        HashSet<Nonterminal> cyclicNonterminals = new HashSet<>();
        for(HashSet<TarjanNode> sCC : stronglyConnectedComponents) {
            // This strongly connected component is cyclic if either:
            // 1) It has a size greater than 1 OR
            // 2) It has a size of 1 AND the one node in it has an edge
            //    to itself
            boolean cyclicComponent = sCC.size() > 1;
            if(! cyclicComponent) {
                TarjanNode singleNode = sCC.iterator().next();
                cyclicComponent = graphEdges.get(singleNode).contains(singleNode);
            }
            if(cyclicComponent) {
                for(TarjanNode node : sCC) {
                    cyclicNonterminals.add(node.nonterminal);
                }
            }
        }

        // Now we need to find all nodes that either are in a cycle
        // themselves, or can reach a cyclic node. The easiest way to
        // do this is to reverse every edge in the graph, and then
        // do a DFS starting from every cyclic node
        // Here, we only consider nonterminals because they are the
        // only nodes that could be cyclic or reach a cyclic node
        HashMap<Nonterminal, HashSet<Nonterminal>> reversedGraphEdges =
                new HashMap<>();
        // first fill it with empty sets
        for(Nonterminal nonterminal: nonterminals) {
            reversedGraphEdges.put(nonterminal, new HashSet<Nonterminal>());
        }
        // Now populate the sets
        for(TarjanNode edgeStart : graphEdges.keySet()) {
            HashSet<TarjanNode> edgeEnds = graphEdges.get(edgeStart);
            for(TarjanNode edgeEnd : edgeEnds) {
                reversedGraphEdges.get(edgeEnd.nonterminal).add(edgeStart.nonterminal);
            }
        }

        // Now run DFS on the reversed edges, starting from all cyclic
        // nodes
        HashSet<Nonterminal> canReachCyclic = new HashSet<>();
        for(Nonterminal nonterminal : cyclicNonterminals) {
            depthFirstSearch(nonterminal, reversedGraphEdges, canReachCyclic);
        }

        // Now go through the nonterminals, and find all rules that
        // will not progress completely toward a terminal
        HashMap<Nonterminal, List<GrammarRule>> growingRules =
                new HashMap<>();
        for(Nonterminal nonterminal: nonterminals) {
            List<GrammarRule> rules = grammar.getRulesWithLeftHandSide(nonterminal);
            List<GrammarRule> goodRules = new ArrayList<>();
            for(GrammarRule possibleRule : rules) {
                for(GrammarElement element : possibleRule.getRightHandSide()) {
                    if(element instanceof Terminal) {
                        continue;
                    }
                    Nonterminal currentNonterminal = (Nonterminal) element;
                    if(canReachCyclic.contains(currentNonterminal)) {
                        goodRules.add(possibleRule);
                        break;
                    }
                }
            }
            if(! goodRules.isEmpty()) {
                growingRules.put(nonterminal, goodRules);
            } else {
                // There were no rules that kept us in a cycle or moving
                // towards a cycle. In other words, for every rule, the
                // right hand side evaluates to a finite length string
                // of terminals. So we'll just use all the rules, since
                // they're all equally bad.
                growingRules.put(nonterminal, rules);
            }
        }

        // Now we want to go through the nonterminals and, for each
        // one, come up with a finite length string that it produces.
        // Once we have expanded our working string to the desired
        // length, we will use these finite length strings to finish
        // making it entirely terminals.
        // Again, we will reverse the edges in the graph, this time
        // including the terminals. Then we will do a topological sort
        // on the graph.

        HashMap<Nonterminal, List<Terminal>> finishingStrings = new HashMap<>();
        HashMap<Nonterminal, List<List<GrammarElement>>> workingFinishingStrings
                 = new HashMap<>();
        HashMap<GrammarElement, HashSet<Nonterminal>> allReverseEdges = new HashMap<>();
        // Go ahead and put empty lists all in the maps
        for(Nonterminal nonterminal : nonterminals) {
            allReverseEdges.put(nonterminal, new HashSet<Nonterminal>());
            workingFinishingStrings.put(nonterminal, new ArrayList<List<GrammarElement>>());
        }
        for(Terminal terminal : terminals) {
            allReverseEdges.put(terminal, new HashSet<Nonterminal>());
        }
        // Now populate the allReverseEdges and workingFinishingStrings maps
        for(Nonterminal nonterminal : nonterminals) {
            List<GrammarRule> rules = grammar.getRulesWithLeftHandSide(nonterminal);
            for(GrammarRule rule : rules) {
                // First, add to the reverse edges map
                for(GrammarElement element : rule.getRightHandSide()) {
                    allReverseEdges.get(element).add(nonterminal);
                }
                // Now, get the workingFinishStrings map going
                List<GrammarElement> ruleList = new ArrayList<>();
                ruleList.addAll(rule.getRightHandSide());
                workingFinishingStrings.get(nonterminal).add(ruleList);
            }
        }

        // Now we do the topological sort
        Queue<GrammarElement> finishedElements = new ArrayDeque<>();
        finishedElements.addAll(terminals);
        while(! finishedElements.isEmpty()) {
            GrammarElement processing = finishedElements.remove();
            List<Terminal> terminalsOfProcessing;
            if(processing instanceof Terminal) {
                // just do this for convenience
                terminalsOfProcessing = new ArrayList<>();
                terminalsOfProcessing.add((Terminal) processing);
            } else {
                terminalsOfProcessing = finishingStrings.get(processing);
            }

            HashSet<Nonterminal> nonterminalsWithThisElement = allReverseEdges.get(processing);
            for(Nonterminal nonterminalToEdit : nonterminalsWithThisElement) {
                // If this nonterminal has already been finished, we
                // don't need to consider it at all. How could that
                // happen, if nonterminalToEdit is used in it somewhere
                // and we're just now about to start substituting
                // nonterminalToEdit into it? Well maybe one of its
                // rules uses nonterminalToEdit, but another rule
                // doesn't, and that other rule has already been finished.
                if(finishingStrings.containsKey(nonterminalToEdit)) {
                    continue;
                }
                // Look through all the rules that we're working with for
                // this nonterminal
                List<List<GrammarElement>> workingRules = workingFinishingStrings.get(nonterminalToEdit);
                for(List<GrammarElement> workingRule : workingRules) {
                    // Replace all instances of processing with their
                    // terminals
                    for(int i = 0; i < workingRule.size(); i++) {
                        if(workingRule.get(i).equals(processing)) {
                            // replace it with terminalsOfProcessing
                            workingRule.remove(i);
                            workingRule.addAll(i, terminalsOfProcessing);
                            // we can go ahead and increment i some
                            // must be careful not to forget that the for
                            // loop will increment i once more too
                            i += terminalsOfProcessing.size() - 1;
                        }
                    }
                    // Check if the list is all terminals now. If it it,
                    // it's done, and so we've got a finishing string
                    // for this nonterminal, so we're done
                    boolean allTerminals = true;
                    for(GrammarElement element : workingRule) {
                        if(element instanceof Nonterminal) {
                            allTerminals = false;
                            break;
                        }
                    }
                    if(allTerminals) {
                        // Java is dumb and is making me make an entirely
                        // new list, it says I can't cast workingRule to
                        // a List<Terminal>
                        List<Terminal> finishedRule = new ArrayList<>();
                        for(GrammarElement element : workingRule) {
                            finishedRule.add((Terminal) element);
                        }
                        finishingStrings.put(nonterminalToEdit, finishedRule);
                        finishedElements.add(nonterminalToEdit);
                        // And we're done with this nonterminal
                        break;
                    }
                }
            }
        }

        // At this point we're ready to randomly generate strings
        // We start with a list of a single node, the start nonterminal
        // (and the head node)
        int targetNumTerminals = 125;
        int numTerminals = 0;
        Random random = new Random();
        LinkedListNode head = new LinkedListNode(null);
        LinkedListNode firstNode =
                new LinkedListNode(grammar.getStartRule().getLeftHandSide());
        head.setNext(firstNode);
        firstNode.setPrevious(head);
        RandomRemovalSet<LinkedListNode> nonterminalsInList =
                new RandomRemovalSet<>();
        nonterminalsInList.add(firstNode);

        while(numTerminals < targetNumTerminals) {
            LinkedListNode removedNode = nonterminalsInList.removeRandom();
            Nonterminal removedNonterminal = (Nonterminal) removedNode.getElement();
            List<GrammarRule> replacements = growingRules.get(removedNonterminal);
            // randomly pick a growing rule
            GrammarRule replacement = replacements.get(random.nextInt(replacements.size()));
            // remove the old node from the list, keeping track of its
            // previous and next nodes
            LinkedListNode previousNode = removedNode.getPrevious();
            removedNode.setPrevious(null);
            previousNode.setNext(null);
            LinkedListNode nextNode = removedNode.getNext();
            removedNode.setNext(null);
            // watch out, nextNode can be null
            if(nextNode != null) {
                nextNode.setPrevious(null);
            }
            // add the replacements into the list
            LinkedListNode prev = previousNode;
            for(GrammarElement newElement : replacement.getRightHandSide()) {
                LinkedListNode newNode = new LinkedListNode(newElement);
                prev.setNext(newNode);
                newNode.setPrevious(prev);
                if(newElement instanceof Terminal) {
                    numTerminals++;
                } else {
                    nonterminalsInList.add(newNode);
                }
                prev = newNode;
            }
            prev.setNext(nextNode);
            if(nextNode != null) {
                nextNode.setPrevious(prev);
            }
        }

        // Now the list has the desired amount of terminals in it. All
        // we have to do now is replace all the remaining nonterminals
        // with finishing strings
        // Since we're just going to print the list anyway, we'll do it
        // on the fly
        LinkedListNode currentNode = head.getNext();
        boolean firstWord = true;
        while(currentNode != null) {
            GrammarElement currentElement = currentNode.getElement();
            if(currentElement instanceof Terminal) {
                Terminal currentTerminal = (Terminal) currentElement;
                if(! firstWord) {
                    System.out.print(" ");
                }
                System.out.print(currentTerminal.getSymbol().getPattern());
                firstWord = false;
            } else {
                Nonterminal currentNonterminal = (Nonterminal) currentElement;
                List<Terminal> finishingString = finishingStrings.get(currentNonterminal);
                for(Terminal t : finishingString) {
                    if(! firstWord) {
                        System.out.print(" ");
                    }
                    System.out.print(t.getSymbol().getPattern());
                    firstWord = false;
                }
            }
            currentNode = currentNode.getNext();
        }

        // and we're done
    }

    private static void depthFirstSearch(Nonterminal nonterminal,
                                         HashMap<Nonterminal, HashSet<Nonterminal>> edges,
                                         HashSet<Nonterminal> result) {
        if(result.contains(nonterminal)) {
            return;
        }
        result.add(nonterminal);
        for(Nonterminal edgeEnd : edges.get(nonterminal)) {
            depthFirstSearch(edgeEnd, edges, result);
        }
    }

    private static void strongConnect(TarjanNode node,
                                      HashMap<TarjanNode, HashSet<TarjanNode>> graphEdges,
                                      MutableInt index,
                                      Deque<TarjanNode> stack,
                                      HashSet<HashSet<TarjanNode>> sCCs) {
        node.index = index.value;
        node.lowLink = index.value;
        index.value++;
        stack.push(node);
        node.onStack = true;

        HashSet<TarjanNode> nodeEdges = graphEdges.get(node);
        for(TarjanNode connectedNode : nodeEdges) {
            if (connectedNode.index == -1) {
                strongConnect(connectedNode, graphEdges, index, stack, sCCs);
                node.lowLink = Math.min(node.lowLink, connectedNode.lowLink);
            } else if(connectedNode.onStack) {
                node.lowLink = Math.min(node.lowLink, connectedNode.index);
            }
        }

        if(node.lowLink == node.index) {
            HashSet<TarjanNode> sCC = new HashSet<>();
            while(node.onStack) {
                TarjanNode poppedNode = stack.pop();
                poppedNode.onStack = false;
                sCC.add(poppedNode);
            }
            sCCs.add(sCC);
        }
    }

    private static class TarjanNode {

        public int index;
        public int lowLink;
        public boolean onStack;
        public Nonterminal nonterminal;

        public TarjanNode(Nonterminal n) {
            index = -1;
            lowLink = -1;
            onStack = false;
            nonterminal = n;
        }

        @Override
        public boolean equals(Object other) {
            if(! (other instanceof TarjanNode)) {
                return false;
            }
            TarjanNode otherNode = (TarjanNode) other;
            return otherNode.nonterminal.equals(nonterminal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nonterminal);
        }

        @Override
        public String toString() {
            return "Node[" + nonterminal + ", " + index + ", " + lowLink +
                    ", " + onStack + "]";
        }
    }

    private static class MutableInt {

        public int value;

        public MutableInt() {
            value = 0;
        }
    }

    private static class LinkedListNode {

        private LinkedListNode previous;
        private LinkedListNode next;
        private GrammarElement element;

        public LinkedListNode(GrammarElement gE) {
            element = gE;
        }

        public LinkedListNode getPrevious() {
            return previous;
        }

        public LinkedListNode getNext() {
            return next;
        }

        public void setPrevious(LinkedListNode p) {
            previous = p;
        }

        public void setNext(LinkedListNode n) {
            next = n;
        }

        public GrammarElement getElement() {
            return element;
        }

        // intentionally leaving equals and hashCode blank, I want
        // them to be based on identity

        @Override
        public String toString() {
            return element.toString();
        }
    }

    private static class RandomRemovalSet<E> {

        private HashMap<E, Integer> map;
        private ArrayList<E> list;
        private Random random;

        public RandomRemovalSet() {
            map = new HashMap<>();
            list = new ArrayList<>();
            random = new Random();
        }

        public void add(E element) {
            if(map.containsKey(element)) {
                return;
            }
            map.put(element, list.size());
            list.add(element);
        }

        public E removeRandom() {
            int randIndex = random.nextInt(list.size());
            E removedElement = list.get(randIndex);
            list.set(randIndex, list.get(list.size() - 1));
            list.remove(list.size() - 1);
            map.remove(removedElement);
            return removedElement;
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public int size() {
            return map.size();
        }
    }
}
