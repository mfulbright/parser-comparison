package shared;

import java.util.List;

/**
 * Created by Mark on 3/12/2015.
 */
public interface Parser {
    public void setGrammar(Grammar grammar);
    public ParseTreeNode parse(List<Token> tokens);
}
