import Asm.Program;
import allocReg.AllocationReg;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <fichier.tcl>");
            return;
        }

        String sourceFile = args[0];

        try {
            // 1) Analyse lexicale / syntaxique
            CharStream input = CharStreams.fromFileName(sourceFile);
            grammarTCLLexer lexer = new grammarTCLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            grammarTCLParser parser = new grammarTCLParser(tokens);

            ParseTree tree = parser.main();

            if (parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("Erreur(s) de syntaxe dans " + sourceFile);
                System.exit(1);
            }

            // 2) Typage
            TyperVisitor typer = new TyperVisitor();
            typer.visit(tree);
            System.out.println("Le code est correctement typé.");

            // 3) Génération de code linéaire
            CodeGenerator generator = new CodeGenerator(typer.getSymbolTable());
            Program program = generator.visit(tree);

            String asmLin = "code_semi_compile.asm";
            try (FileWriter writer = new FileWriter(asmLin)) {
                writer.write(program.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 4) Allocation de registres sur le code linéaire
            AllocationReg alloc = new AllocationReg(asmLin);
            alloc.afficherDebug();

            if (alloc.is32Colorable()) {
                System.out.println("CODE ASSEMBLEUR FINAL");
                String codeFinal = alloc.reecriture();
                System.out.println(codeFinal);

                alloc.reecritureOutput(); // écrit prog.asm

                System.out.println("Le code peut-être éxécuté avec: python3 simproc.py");
            } else {
                System.out.println("Impossible d'allouer les registres");
                System.out.println("Le programme nécessite plus de 32 registres");
                System.out.println("L'allocation a échoué");
            }

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
