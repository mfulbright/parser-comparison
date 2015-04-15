package shared;

import java.util.List;

public interface Parser {
    public void setGrammar(Grammar grammar);
    public EarleyParseTreeNode parse(List<Token> tokens);
}
