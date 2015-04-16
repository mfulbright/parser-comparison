package shared;

import java.util.List;

public interface Parser {
    public void setGrammar(Grammar grammar);
    public ParseTreeNode parse(List<Token> tokens);
}
