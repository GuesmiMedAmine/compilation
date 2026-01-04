import org.antlr.v4.runtime.ParserRuleContext;

public class SemanticError extends RuntimeException {

    public SemanticError(ParserRuleContext ctx, String message) {
        super("Erreur ligne " + ctx.getStart().getLine() + " : " + message);
    }
}