import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main <inputfile>");
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

            new TyperVisitor().visit(tree);

            System.out.println("Le code est correctement typé.");

        } catch (IOException e) {
            System.err.println("Erreur fichier : " + e.getMessage());
        } catch (RuntimeException | Error e) {
            System.err.println("Erreur de typage : " + e.getMessage());
            System.exit(1);
        }
    }
}