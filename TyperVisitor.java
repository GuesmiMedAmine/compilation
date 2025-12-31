import java.util.HashMap;
import java.util.Map;
import Type.*;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDeclaration'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignment'");
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIf'");
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhile'");
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
