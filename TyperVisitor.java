import java.util.HashMap;
import java.util.Map;
import Type.PrimitiveType;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.Type;
import Type.UnknownType;

public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType,Type> types = new HashMap<UnknownType,Type>();


    // 1. Table des Symboles (Section 3.1 du plan)
    // Associe le nom de variable à son type
    private Map<String, Type> symbolTable = new HashMap<>();

    /**
     * 2. Le Solver (Section 3.2 du plan)
     * Tente d'unifier t1 et t2 et met à jour la map globale de substitution 'types'.
     */
    private void solve(Type t1, Type t2) {
        // 1. Appliquer les connaissances actuelles (substitutions déjà trouvées)
        Type t1_sub = t1.substituteAll(this.types);
        Type t2_sub = t2.substituteAll(this.types);

        // 2. Tenter d'unifier
        Map<UnknownType, Type> res = t1_sub.unify(t2_sub);

        // Gestion d'erreur si l'unification échoue
        if (res == null) {
            throw new Error("Erreur de typage: Impossible d'unifier " + t1_sub + " et " + t2_sub);
        }

        // 3. Mettre à jour la solution globale 'types'

        // a) Mettre à jour les anciennes entrées avec les nouvelles découvertes
        // Ex: si on savait A=B et qu'on découvre B=int, alors A devient int.
        for (Map.Entry<UnknownType, Type> entry : this.types.entrySet()) {
            entry.setValue(entry.getValue().substituteAll(res));
        }

        // b) Ajouter les nouvelles découvertes
        this.types.putAll(res);
    }

    public Map<UnknownType, Type> getTypes() {
        return types;
    }

    @Override
    public Type visitNegation(grammarTCLParser.NegationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNegation'");
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitComparison'");
    }

    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOr'");
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOpposite'");
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitInteger'");
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBrackets'");
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBoolean'");
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAnd'");
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVariable'");
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMultiplication'");
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEquality'");
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAddition'");
    }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBase_type'");
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        String name = ctx.VAR().getText();

        // 1. Vérification de redéclaration (Règle 1.2 de l'énoncé)
        // On vérifie si le nom existe déjà dans le bloc actuel
        if (symbolTable.containsKey(name)) {
            throw new Error("Erreur sémantique : La variable '" + name + "' est déjà déclarée !");
        }

        Type t;
        // 2. Gestion du type et du mot-clé 'auto' [cite: 38, 54]
        // Si ctx.type() est nul ou contient "auto", on utilise UnknownType
        if (ctx.type() == null || ctx.type().getText().equals("auto")) {
            t = new UnknownType();
        } else {
            t = visit(ctx.type()); // On visite le type (int, bool, tab)
        }

        // 3. Enregistrement dans la table des symboles
        symbolTable.put(name, t);

        // 4. Initialisation (ex: int x = 5)
        if (ctx.expr() != null) {
            Type tExpr = visit(ctx.expr());
            solve(t, tExpr); // Le Solver fait l'inférence
        }

        // En TCL, une déclaration est une instruction, elle ne renvoie pas de valeur
        return null;
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrint'");
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        // 1. Récupération du nom
        String name = ctx.VAR().getText();

        // 2. Vérification dans la symbolTable
        Type varType = symbolTable.get(name);
        if (varType == null) {
            throw new Error("Erreur : La variable '" + name + "' n'est pas déclarée !");
        }

        // 3. Récupération de la nouvelle valeur depuis la LISTE
        if (ctx.expr() != null && !ctx.expr().isEmpty()) {
            Type exprType = visit(ctx.expr().get(0));
            // 4. Unification via le Solver
            solve(varType, exprType);
        }

        return null;
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        // 1. On sauvegarde la table des symboles actuelle (Le "Scope" parent)
        Map<String, Type> parentScope = new HashMap<>(this.symbolTable);

        // 2. On visite tous les enfants du bloc (les instructions) un par un
        // visitChildren est une méthode magique qui évite de devoir connaître le nom exact de la règle
        super.visitChildren(ctx);

        // 3. On restaure la table d'origine : les variables créées dans le bloc sont supprimées
        this.symbolTable = parentScope;

        return null;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        // 1. On récupère le type de la condition entre parenthèses
        Type condType = visit(ctx.expr());

        // 2. On force cette condition à être un BOOLÉEN
        // C'est la règle de sécurité : un "if(5)" ne doit pas passer.
        solve(condType, new PrimitiveType(Type.Base.BOOL));

        // 3. On visite le reste (le corps du 'if' et du 'else')
        // On utilise visitChildren pour éviter les erreurs de noms d'instructions
        super.visitChildren(ctx);

        return null;
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        // 1. On récupère le type de la condition de boucle
        Type condType = visit(ctx.expr());

        // 2. La condition du while doit obligatoirement être un BOOLÉEN
        solve(condType, new PrimitiveType(Type.Base.BOOL));

        // 3. On visite le corps de la boucle
        super.visitChildren(ctx);

        return null;
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFor'");
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturn'");
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCore_fct'");
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDecl_fct'");
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMain'");
    }


}