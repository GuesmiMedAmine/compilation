import java.util.HashMap;
import java.util.Map;
import Type.*;
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
        String name = ctx.getText();
        Type t = symbolTable.get(name);
        if (t == null) {
            throw new RuntimeException("Erreur Variable '" + name + "' non déclarée.");
        }
        return t;
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
        String name = ctx.VAR().getText();
        Type t;

        // Déduction du type
        if (ctx.type().getText().equals("auto")) {
            t = new UnknownType();
        } else {
            t = visit(ctx.type());
        }

        // Enregistrement
        symbolTable.put(name, t);

        if (ctx.expr() != null) {
            Type tExpr = visit(ctx.expr());
            solve(t, tExpr);
        }
        return new PrimitiveType(Type.Base.VOID);
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
        String name = ctx.VAR().getText();
        Type tVar = symbolTable.get(name);
        if (tVar == null) throw new Error("Erreur : Variable '" + name + "' non déclarée !");

        // On prend toujours la derniere expression de la liste

        int lastIndex = ctx.expr().size() - 1;
        Type tExpr = visit(ctx.expr(lastIndex));

        solve(tVar, tExpr);
        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        Map<String, Type> parentScope = new HashMap<>(this.symbolTable);

        // On visite explicitement les instructions du bloc
        for(grammarTCLParser.InstrContext instr : ctx.instr()) {
            visit(instr);
        }

        this.symbolTable = parentScope;
        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL));
        // On visite le 'then'
        visit(ctx.instr(0));
        // On visite le 'else' s'il existe
        if (ctx.instr().size() > 1) {
            visit(ctx.instr(1));
        }
        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL));
        visit(ctx.instr());
        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        Map<String, Type> snapshot = new HashMap<>(symbolTable);

        // 1. Init (ex: int i=0)
        if (ctx.instr(0) != null) visit(ctx.instr(0));

        // 2. Condition (ex: i<10)
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL));

        // 3. Incrément (ex: i=i+1) - C'est la 3eme partie entre parenthèses
        if (ctx.instr().size() > 2) visit(ctx.instr(1));
        // Note: La grammaire for est un peu complexe, on suppose ici la structure standard

        // 4. Corps de la boucle (la dernière instruction)
        visit(ctx.instr(ctx.instr().size() - 1));

        symbolTable = snapshot;
        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        // On renvoie simplement le type de l'expression retournée
        return visit(ctx.expr());
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // On visite toutes les instructions une par une
        for (grammarTCLParser.InstrContext instr : ctx.instr()) {
            visit(instr);
        }
        // On visite le return final
        return visit(ctx.expr());
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDecl_fct'");
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        return visit(ctx.core_fct());
    }


}