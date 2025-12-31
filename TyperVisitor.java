import java.util.HashMap;
import java.util.Map;
import Type.*;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.*;

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
        Type t = visit(ctx.expr());
        solve(t, new PrimitiveType(Type.Base.BOOL));
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.INT));
        solve(t2, new PrimitiveType(Type.Base.INT));
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.BOOL));
        solve(t2, new PrimitiveType(Type.Base.BOOL));
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Type t = visit(ctx.expr());
        solve(t, new PrimitiveType(Type.Base.INT));
        return new PrimitiveType(Type.Base.INT);
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new PrimitiveType(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {

        // Type de l'expression t
        Type tType = visit(ctx.expr(0));

        // Type de l'index i
        Type indexType = visit(ctx.expr(1));

        // 1) L'index doit être un entier
        solve(indexType, new PrimitiveType(Type.Base.INT));

        // 2) Le tableau doit être un ArrayType (ou un UnknownType qui devient tableau)
        UnknownType elemType = new UnknownType();
        ArrayType expectedArray = new ArrayType(elemType);

        // Tenter d'unifier tType avec Array[T]
        solve(tType, expectedArray);

        // 3) Le résultat est le type des éléments
        return elemType;
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // on renvoie juste le type de l'expression à l'intérieur
        return visit(ctx.expr());
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.BOOL));
        solve(t2, new PrimitiveType(Type.Base.BOOL));
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVariable'");
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // On force les deux côtés à être des entiers car on a une multiplication
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.INT));
        solve(t2, new PrimitiveType(Type.Base.INT));
        return new PrimitiveType(Type.Base.INT);
    }
    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        // Pour l'égalité, on vérifie juste que t1 et t2 sont du même type
        solve(t1, t2);
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {

        // 1) Créer un nouveau type inconnu pour le type des éléments
        UnknownType T = new UnknownType();

        // 2) Pour chaque élément entre { ... }, unifier son type avec T
        for (var expr : ctx.expr()) {
            Type elemType = visit(expr);
            solve(elemType, T);
        }

        // 3) Retourner un tableau dont les éléments sont de type T
        return new ArrayType(T);
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {

            Type t1 = visit(ctx.expr(0));
            Type t2 = visit(ctx.expr(1));
            // On force les deux côtés à être des entiers car on a une addition
            solve(t1, new PrimitiveType(Type.Base.INT));
            solve(t2, new PrimitiveType(Type.Base.INT));
            return new PrimitiveType(Type.Base.INT);
        }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // Reconnaît 'int' et 'bool' dans la déclaration des fonctions
        if (ctx.getText().equals("int")) return new PrimitiveType(Type.Base.INT);
        if (ctx.getText().equals("bool")) return new PrimitiveType(Type.Base.BOOL);
        return new UnknownType();
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        // 1. Récupération du nom
        String name = ctx.VAR().getText();
        Type t;

        // 2. Détermination du type
        if (ctx.type() != null) {
            t = visit(ctx.type());
        } else {
            t = new UnknownType();
        }

        // 3. SETUP TABLE DES SYMBOLES : On enregistre
        symbolTable.put(name, t);

        // 4. Initialisation (On traite la liste d'expressions)
        if (ctx.expr() != null && !ctx.expr().isEmpty()) {
            // On récupère le premier élément de la LISTE avec .get(0)
            Type tExpr = visit(ctx.expr());
            solve(t, tExpr);
        }
        return null;

    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        String varName = ctx.VAR().getText();
        Type t = symbolTable.get(varName);
        if (t == null) throw new RuntimeException("Erreur : Variable '" + varName + "' n'est pas déclarée.");
        solve(t, new PrimitiveType(Type.Base.INT));
        return new PrimitiveType(Type.Base.VOID);
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