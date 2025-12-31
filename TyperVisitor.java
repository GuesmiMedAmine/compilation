import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        Type tTab = visit(ctx.expr(0));   // le tableau
        Type tIndex = visit(ctx.expr(1)); // l'index

        // L'index doit être un entier
        solve(tIndex, new PrimitiveType(Type.Base.INT));

        // Le tableau doit être un ArrayType contenant un type inconnu T
        UnknownType elemType = new UnknownType();
        solve(tTab, new ArrayType(elemType));

        return elemType; // on retourne le type des éléments
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // on renvoie juste le type de l'expression à l'intérieur
        return visit(ctx.expr());
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        String name = ctx.VAR().getText();
        Type t = symbolTable.get(name);
        if (!(t instanceof FunctionType)) throw new Error("Fonction inconnue ou variable utilisée comme fonction : " + name);

        FunctionType fType = (FunctionType) t;

        // Vérification du nombre d'arguments
        if (ctx.expr().size() != fType.getNbArgs()) 
            throw new Error("Mauvais nombre d'arguments pour " + name);

        // Création d'une instance fraîche pour chaque UnknownType (polymorphisme)
        FunctionType instance = freshFunctionType(fType);

        // Vérification des types des arguments
        for (int i = 0; i < ctx.expr().size(); i++) {
            Type argType = visit(ctx.expr(i));
            solve(argType, instance.getArgsType(i));
        }

        return instance.getReturnType();
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
        if (ctx.expr().isEmpty()) {
            // Cas : new type[N] n'existe pas encore dans ta grammaire, 
            // on peut juste retourner un ArrayType d'un type inconnu
            return new ArrayType(new UnknownType());
        } else {
            // Cas : {val1, val2, ...}
            UnknownType elemType = new UnknownType();
            for (grammarTCLParser.ExprContext expr : ctx.expr()) {
                Type tElem = visit(expr);
                solve(tElem, elemType); // Tous les éléments doivent avoir le même type
            }
            return new ArrayType(elemType);
        }
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
        // ctx.type() existe car tab_type: type '[' ']'
        Type baseType = visit(ctx.type());
        return new ArrayType(baseType);
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
        // Création d'un scope local pour les instructions
        Map<String, Type> snapshot = new HashMap<>(symbolTable);

        // On visite toutes les instructions du corps
        for (grammarTCLParser.InstrContext instr : ctx.instr()) {
            visit(instr);
        }

        // Visite du return final et récupération de son type
        Type retType = visit(ctx.expr());

        // Restauration du scope parent (variables locales effacées)
        symbolTable = snapshot;

        return retType;
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        String name = ctx.VAR(0).getText(); // nom de la fonction

        // Type de retour
        Type returnType = ctx.type(0).getText().equals("auto") ? new UnknownType() : visit(ctx.type(0));

        // Création de la signature des arguments
        ArrayList<Type> argsTypes = new ArrayList<>();

        // Les arguments commencent à VAR(1), type(1) correspond à VAR(1)
        for (int i = 1; i < ctx.VAR().size(); i++) {
            Type argType = visit(ctx.type(i)); // type du paramètre
            argsTypes.add(argType);
            symbolTable.put(ctx.VAR(i).getText(), argType); // ajout au scope local
        }

        FunctionType fType = new FunctionType(returnType, argsTypes);

        // Ajout au scope global pour récursivité
        symbolTable.put(name, fType);

        // Sauvegarde du scope pour le corps de la fonction
        Map<String, Type> snapshot = new HashMap<>(symbolTable);

        // Visite du corps
        visit(ctx.core_fct());

        // Restauration du scope global
        symbolTable = snapshot;

        // On remet la fonction globale
        symbolTable.put(name, fType);

        return new PrimitiveType(Type.Base.VOID);
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        // On considère 'main' comme une fonction sans argument et de type int
        FunctionType mainType = new FunctionType(new PrimitiveType(Type.Base.INT), new ArrayList<>());
        symbolTable.put("main", mainType);

        // Visite des éventuelles déclarations de fonction avant le main
        for (int i = 0; i < ctx.decl_fct().size(); i++) {
            visit(ctx.decl_fct(i));
        }

        // Visite du corps du main
        Type ret = visit(ctx.core_fct());

        // Ici on pourrait faire un check final que le type du main est int
        solve(ret, mainType.getReturnType());

        return mainType;
    }

    // Génère une "copie fraîche" d'une FunctionType en clonant tous les UnknownType
    private FunctionType freshFunctionType(FunctionType f) {
        Map<UnknownType, UnknownType> mapping = new HashMap<>();

        ArrayList<Type> newArgs = new ArrayList<>();
        for (Type arg : f.getArgsTypes()) {
            newArgs.add(freshType(arg, mapping));
        }
        Type newReturn = freshType(f.getReturnType(), mapping);
        return new FunctionType(newReturn, newArgs);
    }

    // Clone récursif des UnknownType pour créer de nouvelles instances
    private Type freshType(Type t, Map<UnknownType, UnknownType> mapping) {
        if (t instanceof UnknownType) {
            UnknownType ut = (UnknownType) t;
            if (!mapping.containsKey(ut)) mapping.put(ut, new UnknownType());
            return mapping.get(ut);
        } else if (t instanceof ArrayType) {
            return new ArrayType(freshType(((ArrayType) t).getTabType(), mapping));
        } else if (t instanceof FunctionType) {
            FunctionType ft = (FunctionType) t;
            ArrayList<Type> args = new ArrayList<>();
            for (Type arg : ft.getArgsTypes()) args.add(freshType(arg, mapping));
            return new FunctionType(freshType(ft.getReturnType(), mapping), args);
        } else {
            return t; // PrimitiveType
        }
    }

}