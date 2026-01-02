import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <fichier>");
            return;
        }

        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            grammarTCLLexer lexer = new grammarTCLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            grammarTCLParser parser = new grammarTCLParser(tokens);

            ParseTree tree = parser.main();

            if (parser.getNumberOfSyntaxErrors() > 0) {
                System.exit(1);
            }

            TyperVisitor typer = new TyperVisitor();
            typer.visit(tree);

            System.out.println("Le code est correctement typé.");

        } catch (IOException e) {
            System.err.println("Erreur de lecture : " + e.getMessage());
        } catch (SemanticError e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}