import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext; // Important pour les lignes d'erreur
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.*;

/**
 * TyperVisitor est le visiteur de typage pour la grammaire TCL générée par ANTLR.
 *
 * Rôle général:
 * - Parcourt l'arbre de syntaxe et calcule/unifie les types des expressions et instructions.
 * - Gère l'inférence de type (mots‑clés « auto ») via des variables de type (UnknownType) et un solveur d’unification.
 * - Maintient une table des symboles (symbolTable) pour les variables/fonctions visibles dans le scope courant.
 * - Produit des erreurs sémantiques précises avec localisation (ligne/colonne) grâce aux ParserRuleContext.
 *
 * Principaux mécanismes:
 * - Unification: la méthode privée solve(t1, t2, ctx) tente d’unifier deux types après substitution des
 *   équations connues (carte "types"). En cas d’échec, une SemanticError est levée avec le contexte.
 * - Substitutions: chaque visite d’expression renvoie un Type pouvant contenir des UnknownType qui
 *   seront progressivement remplacés par des types plus précis au fil des contraintes (unifications).
 * - Portée des symboles: les blocs/instructions créent et restaurent des instantanés de la table des
 *   symboles pour simuler l’entrée/sortie de scope.
 * - Tableaux: l’accès/affectation t[i] force i à être INT et contraint t à être un ArrayType dont on
 *   récupère/unifie le type d’élément.
 * - Fonctions: les déclarations construisent un FunctionType (retour + liste d’arguments). Les appels
 *   utilisent un « freshening » des variables de type (freshFunctionType/freshType) pour obtenir une
 *   instance polymorphe indépendante avant d’unifier chaque argument réel avec le paramètre formel.
 * - Main: impose que main retourne un INT et vérifie la cohérence du type renvoyé par son corps.
 *
 * Champs principaux:
 * - types: équations courantes entre UnknownType et Type, enrichies par les unifications réussies.
 * - symbolTable: portée courante des identifiants (variables et fonctions) vers leurs Types.
 *
 * Exceptions:
 * - SemanticError est levée pour toute incohérence (type non unifiable, variable/fonction inconnue,
 *   nombre d’arguments incorrect, etc.), en utilisant le ParserRuleContext pour des messages précis.

 */
public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType,Type> types = new HashMap<UnknownType,Type>();
    private Map<String, Type> symbolTable = new HashMap<>();

    /**
     * Unifie deux types sous les substitutions courantes et enrichit l’ensemble d’équations.
     * En cas d’échec d’unification, lève une SemanticError contextualisée avec la position d’ANTLR.
     *
     * @param t1  premier type à unifier (après substitution progressive)
     * @param t2  second type à unifier (après substitution progressive)
     * @param ctx contexte ANTLR utilisé pour reporter précisément l’erreur en cas d’échec
     * @throws SemanticError si les types ne peuvent pas être unifiés
     */
    private void solve(Type t1, Type t2, ParserRuleContext ctx) {
        Type t1_sub = t1.substituteAll(this.types);
        Type t2_sub = t2.substituteAll(this.types);

        Map<UnknownType, Type> res = t1_sub.unify(t2_sub);

        if (res == null) {
            throw new SemanticError(ctx, "Erreur de typage : Impossible d'unifier " + t1_sub + " et " + t2_sub);
        }

        for (Map.Entry<UnknownType, Type> entry : this.types.entrySet()) {
            entry.setValue(entry.getValue().substituteAll(res));
        }
        this.types.putAll(res);
    }

    /**
     * Retourne l’ensemble courant des équations de types (substitutions) accumulées par le solveur.
     * Utile pour l’inspection/debug ou des passes ultérieures.
     *
     * @return la table de substitutions entre UnknownType et Type
     */
    public Map<UnknownType, Type> getTypes() {
        return types;
    }

    // --- OPÉRATIONS LOGIQUES & ARITHMÉTIQUES ---

    /**
     * Visite d’une négation logique: vérifie que l’opérande est BOOL et renvoie BOOL.
     * @param ctx nœud ANTLR de la négation
     * @return le type BOOL
     * @throws SemanticError si l’opérande n’est pas de type booléen
     */
    @Override
    public Type visitNegation(grammarTCLParser.NegationContext ctx) {
        Type t = visit(ctx.expr());
        solve(t, new PrimitiveType(Type.Base.BOOL), ctx);
        return new PrimitiveType(Type.Base.BOOL);
    }

    /**
     * Visite d’une comparaison (<, >, <=, >=, ...): impose INT sur les deux opérandes et renvoie BOOL.
     * @param ctx nœud ANTLR de la comparaison
     * @return le type BOOL
     * @throws SemanticError si un opérande n’est pas entier
     */
    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.INT), ctx);
        solve(t2, new PrimitiveType(Type.Base.INT), ctx);
        return new PrimitiveType(Type.Base.BOOL);
    }

    /**
     * Visite d’un OU logique: force les deux opérandes à BOOL et renvoie BOOL.
     * @param ctx nœud ANTLR de l’opérateur OR
     * @return le type BOOL
     * @throws SemanticError si un des opérandes n’est pas booléen
     */
    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.BOOL), ctx);
        solve(t2, new PrimitiveType(Type.Base.BOOL), ctx);
        return new PrimitiveType(Type.Base.BOOL);
    }

    /**
     * Visite d’un ET logique: force les deux opérandes à BOOL et renvoie BOOL.
     * @param ctx nœud ANTLR de l’opérateur AND
     * @return le type BOOL
     * @throws SemanticError si un des opérandes n’est pas booléen
     */
    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.BOOL), ctx);
        solve(t2, new PrimitiveType(Type.Base.BOOL), ctx);
        return new PrimitiveType(Type.Base.BOOL);
    }

    /**
     * Visite de l’opposé unaire (-e): impose INT sur l’opérande et renvoie INT.
     * @param ctx nœud ANTLR de l’opposé
     * @return le type INT
     * @throws SemanticError si l’opérande n’est pas entier
     */
    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Type t = visit(ctx.expr());
        solve(t, new PrimitiveType(Type.Base.INT), ctx);
        return new PrimitiveType(Type.Base.INT);
    }

    /**
     * Visite d’une multiplication/division: impose INT sur les deux opérandes et renvoie INT.
     * @param ctx nœud ANTLR de la multiplication
     * @return le type INT
     * @throws SemanticError si un opérande n’est pas entier
     */
    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.INT), ctx);
        solve(t2, new PrimitiveType(Type.Base.INT), ctx);
        return new PrimitiveType(Type.Base.INT);
    }

    /**
     * Visite d’une addition/soustraction: impose INT sur les deux opérandes et renvoie INT.
     * @param ctx nœud ANTLR de l’addition
     * @return le type INT
     * @throws SemanticError si un opérande n’est pas entier
     */
    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, new PrimitiveType(Type.Base.INT), ctx);
        solve(t2, new PrimitiveType(Type.Base.INT), ctx);
        return new PrimitiveType(Type.Base.INT);
    }

    /**
     * Visite d’une égalité/inegalité (==, !=): unifie les deux opérandes et renvoie BOOL.
     * @param ctx nœud ANTLR de l’égalité
     * @return le type BOOL
     * @throws SemanticError si les opérandes ne peuvent pas être unifiés
     */
    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        Type t1 = visit(ctx.expr(0));
        Type t2 = visit(ctx.expr(1));
        solve(t1, t2, ctx);
        return new PrimitiveType(Type.Base.BOOL);
    }

    // --- TYPES DE BASE ---

    /**
     * Littéral entier: retourne INT.
     * @param ctx nœud ANTLR du littéral
     * @return le type INT
     */
    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new PrimitiveType(Type.Base.INT);
    }

    /**
     * Littéral booléen: retourne BOOL.
     * @param ctx nœud ANTLR du littéral
     * @return le type BOOL
     */
    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new PrimitiveType(Type.Base.BOOL);
    }

    /**
     * Type de base référencé dans le code (int, bool ou auto): traduit en PrimitiveType ou UnknownType.
     * @param ctx nœud ANTLR du type de base
     * @return INT, BOOL, ou UnknownType pour auto/indéterminé
     */
    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        if (ctx.getText().equals("int")) return new PrimitiveType(Type.Base.INT);
        if (ctx.getText().equals("bool")) return new PrimitiveType(Type.Base.BOOL);
        return new UnknownType();
    }

    // --- VARIABLES & SCOPES ---

    /**
     * Utilisation d’une variable: récupère son type depuis la table des symboles.
     * @param ctx nœud ANTLR de la variable
     * @return le type de la variable
     * @throws SemanticError si la variable n’est pas déclarée dans la portée courante
     */
    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        String name = ctx.getText();
        Type t = symbolTable.get(name);
        if (t == null) {
            throw new SemanticError(ctx, "La variable '" + name + "' n'est pas déclarée.");
        }
        return t;
    }

    /**
     * Déclaration de variable avec type explicite ou auto et option d’initialisation.
     * Contrainte le type par l’expression si présente et enregistre le symbole dans la portée.
     * @param ctx nœud ANTLR de la déclaration
     * @return VOID
     * @throws SemanticError si redéclaration locale ou incompatibilité de types
     */
    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        String name = ctx.VAR().getText();

        if (symbolTable.containsKey(name)) {
            throw new SemanticError(ctx, "La variable '" + name + "' est déjà déclarée dans ce bloc.");
        }

        Type t;
        if (ctx.type().getText().equals("auto")) {
            t = new UnknownType();
        } else {
            t = visit(ctx.type());
        }

        if (ctx.expr() != null) {
            Type tExpr = visit(ctx.expr());
            solve(t, tExpr, ctx);
        }
        symbolTable.put(name, t);
        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Affectation sur variable ou accès tabulaire en chaîne (t[i][j] = v).
     * Vérifie les index (INT), contraint les niveaux de tableaux, puis unifie la valeur assignée.
     * @param ctx nœud ANTLR de l’affectation
     * @return VOID
     * @throws SemanticError si la variable est inconnue ou si les types/index sont invalides
     */
    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        String name = ctx.VAR().getText();
        Type tVar = symbolTable.get(name);
        if (tVar == null) throw new SemanticError(ctx, "La variable '" + name + "' n'est pas déclarée.");

        // La valeur à assigner est toujours la dernière expression
        int lastIndex = ctx.expr().size() - 1;
        Type tExpr = visit(ctx.expr(lastIndex));

        // Le nombre de crochets correspond au nombre d'expr - 1 (la valeur)
        int nbAccess = ctx.expr().size() - 1;
        Type currentType = tVar;

        for (int i = 0; i < nbAccess; i++) {
            // L'index doit être un int
            Type tIndex = visit(ctx.expr(i));
            solve(tIndex, new PrimitiveType(Type.Base.INT), ctx);

            // On creuse : currentType doit être un tableau
            UnknownType content = new UnknownType();
            solve(currentType, new ArrayType(content), ctx);
            currentType = content;
        }

        solve(currentType, tExpr, ctx);
        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Instruction d’affichage: s’assure que la variable existe et n’est pas de type VOID.
     * @param ctx nœud ANTLR du print
     * @return VOID
     * @throws SemanticError si la variable est inconnue ou de type VOID
     */
    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        String varName = ctx.VAR().getText();
        Type t = symbolTable.get(varName);
        if (t == null) throw new SemanticError(ctx, "La variable '" + varName + "' n'est pas déclarée.");

        if (t instanceof PrimitiveType && ((PrimitiveType)t).getType() == Type.Base.VOID) {
            throw new SemanticError(ctx, "Impossible d'afficher du VOID.");
        }
        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Bloc d’instructions: crée une nouvelle portée temporaire, visite chaque instruction, puis restaure.
     * @param ctx nœud ANTLR du bloc
     * @return VOID
     */
    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        Map<String, Type> parentScope = new HashMap<>(this.symbolTable);
        for(grammarTCLParser.InstrContext instr : ctx.instr()) {
            visit(instr);
        }
        this.symbolTable = parentScope;
        return new PrimitiveType(Type.Base.VOID);
    }

    // --- STRUCTURES DE CONTRÔLE ---

    /**
     * Instruction conditionnelle if/else: impose un booléen pour la condition et visite les branches.
     * @param ctx nœud ANTLR du if
     * @return VOID
     * @throws SemanticError si la condition n’est pas booléenne
     */
    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL), ctx);
        visit(ctx.instr(0));
        if (ctx.instr().size() > 1) {
            visit(ctx.instr(1));
        }
        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Boucle while: impose un booléen pour la condition et visite le corps.
     * @param ctx nœud ANTLR du while
     * @return VOID
     * @throws SemanticError si la condition n’est pas booléenne
     */
    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL), ctx);
        visit(ctx.instr());
        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Boucle for (init; cond; incr): visite l’init et l’incrément si présents, impose BOOL sur la condition,
     * et exécute le corps dans une portée restaurée à la fin.
     * @param ctx nœud ANTLR du for
     * @return VOID
     * @throws SemanticError si la condition n’est pas booléenne
     */
    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        Map<String, Type> snapshot = new HashMap<>(symbolTable);
        if (ctx.instr(0) != null) visit(ctx.instr(0)); // Init
        solve(visit(ctx.expr()), new PrimitiveType(Type.Base.BOOL), ctx); // Condition
        if (ctx.instr().size() > 2) visit(ctx.instr(1)); // Incrément
        visit(ctx.instr(ctx.instr().size() - 1)); // Corps
        symbolTable = snapshot;
        return new PrimitiveType(Type.Base.VOID);
    }

    // --- TABLEAUX ---

    /**
     * Accès à un tableau t[i]: vérifie que l’index est INT et retourne le type des éléments du tableau.
     * @param ctx nœud ANTLR de l’accès tabulaire
     * @return le type des éléments du tableau
     * @throws SemanticError si l’index n’est pas entier ou si l’expression n’est pas un tableau
     */
    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Type tTab = visit(ctx.expr(0));
        Type tIndex = visit(ctx.expr(1));

        solve(tIndex, new PrimitiveType(Type.Base.INT), ctx);

        UnknownType elemType = new UnknownType();
        solve(tTab, new ArrayType(elemType), ctx);

        return elemType;
    }

    /**
     * Initialisation de tableau: contraint tous les éléments à un type commun et retourne ArrayType(elem).
     * Si la liste est vide, retourne un tableau d’éléments inconnus.
     * @param ctx nœud ANTLR de l’initialisation de tableau
     * @return un ArrayType dont le type d’élément est unifié sur la liste
     */
    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        if (ctx.expr().isEmpty()) {
            return new ArrayType(new UnknownType());
        } else {
            UnknownType elemType = new UnknownType();
            for (grammarTCLParser.ExprContext expr : ctx.expr()) {
                Type tElem = visit(expr);
                solve(tElem, elemType, ctx);
            }
            return new ArrayType(elemType);
        }
    }

    /**
     * Type tableau: construit un ArrayType du type de base fourni.
     * @param ctx nœud ANTLR du type tableau
     * @return un ArrayType(baseType)
     */
    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        Type baseType = visit(ctx.type());
        return new ArrayType(baseType);
    }

    /**
     * Parenthèses: propage simplement le type de l’expression entre parenthèses.
     * @param ctx nœud ANTLR des parenthèses
     * @return le type de l’expression interne
     */
    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        return visit(ctx.expr());
    }

    // --- FONCTIONS ---

    /**
     * Déclaration de fonction: construit la signature (retour + paramètres), gère les types auto,
     * vérifie les doublons d’arguments, visite le corps et unifie le type de retour effectif avec la signature.
     * @param ctx nœud ANTLR de la déclaration de fonction
     * @return VOID
     * @throws SemanticError si redéclaration, doublon d’arguments ou incompatibilité de types
     */
    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        String name = ctx.VAR(0).getText();
        if (symbolTable.containsKey(name)) {
            throw new SemanticError(ctx, "La fonction '" + name + "' est déjà définie.");
        }

        Type returnType = ctx.type(0).getText().equals("auto") ? new UnknownType() : visit(ctx.type(0));
        ArrayList<Type> argsTypes = new ArrayList<>();
        Map<String, Type> tempArgs = new HashMap<>();

        for (int i = 1; i < ctx.VAR().size(); i++) {
            String argName = ctx.VAR(i).getText();
            if (tempArgs.containsKey(argName)) {
                throw new SemanticError(ctx, "L'argument '" + argName + "' est dupliqué dans la fonction " + name);
            }
            Type argType = ctx.type(i).getText().equals("auto") ? new UnknownType() : visit(ctx.type(i));
            argsTypes.add(argType);
            tempArgs.put(argName, argType);
        }

        FunctionType fType = new FunctionType(returnType, argsTypes);
        symbolTable.put(name, fType);

        Map<String, Type> snapshot = new HashMap<>(symbolTable);
        symbolTable.putAll(tempArgs);

        // Visite du corps et récupération du type réel retourné
        Type bodyReturnType = visit(ctx.core_fct());

        solve(bodyReturnType, returnType, ctx);

        symbolTable = snapshot;
        symbolTable.put(name, fType);

        return new PrimitiveType(Type.Base.VOID);
    }

    /**
     * Corps de fonction: visite toutes les instructions, évalue l’expression de return et restitue la portée.
     * @param ctx nœud ANTLR du corps de fonction
     * @return le type calculé de l’expression de retour
     */
    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        Map<String, Type> snapshot = new HashMap<>(symbolTable);
        for (grammarTCLParser.InstrContext instr : ctx.instr()) {
            visit(instr);
        }
        Type retType = visit(ctx.expr());
        symbolTable = snapshot;
        return retType;
    }

    /**
     * Instruction return: propage le type de l’expression renvoyée.
     * @param ctx nœud ANTLR du return
     * @return le type de l’expression de retour
     */
    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        return visit(ctx.expr());
    }

    /**
     * Appel de fonction: vérifie l’existence de la fonction, le nombre d’arguments, instancie
     * fraîchement la signature polymorphe (freshening) et unifie chaque argument réel avec le paramètre.
     * @param ctx nœud ANTLR de l’appel
     * @return le type de retour de l’instance appelée
     * @throws SemanticError si la cible n’est pas une fonction ou si l’arity est incorrecte
     */
    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        String name = ctx.VAR().getText();
        Type t = symbolTable.get(name);

        if (!(t instanceof FunctionType)) {
            throw new SemanticError(ctx, "Fonction inconnue ou variable utilisée comme fonction : " + name);
        }

        FunctionType fType = (FunctionType) t;

        if (ctx.expr().size() != fType.getNbArgs()) {
            throw new SemanticError(ctx, "Mauvais nombre d'arguments pour " + name + " (attendu: " + fType.getNbArgs() + ", reçu: " + ctx.expr().size() + ")");
        }

        fType = (FunctionType) fType.substituteAll(this.types);

        FunctionType instance = freshFunctionType(fType);

        for (int i = 0; i < ctx.expr().size(); i++) {
            Type argType = visit(ctx.expr(i));
            solve(argType, instance.getArgsType(i), ctx);
        }

        return instance.getReturnType();
    }

    /**
     * Point d’entrée main: enregistre une signature main(): INT, visite les autres fonctions,
     * visite le corps et unifie le type de retour obtenu avec INT.
     * @param ctx nœud ANTLR du main
     * @return la FunctionType de main
     * @throws SemanticError si le type renvoyé par le corps n’est pas INT
     */
    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        FunctionType mainType = new FunctionType(new PrimitiveType(Type.Base.INT), new ArrayList<>());
        symbolTable.put("main", mainType);

        for (int i = 0; i < ctx.decl_fct().size(); i++) {
            visit(ctx.decl_fct(i));
        }

        Type ret = visit(ctx.core_fct());
        // On passe 'ctx' au solveur, mais main n'a pas vraiment de ctx unique englobant,
        // on peut utiliser le core_fct si besoin, ou null si on est confiant.
        // Utilisons ctx.core_fct() pour la localisation.
        solve(ret, mainType.getReturnType(), ctx.core_fct());

        return mainType;
    }

    // --- POLYMORPHISME (Helpers) ---

    /**
     * Crée une instance fraîche d’un FunctionType en remplaçant chaque UnknownType par un nouveau UnknownType
     * cohérent via un mapping partagé, afin de modéliser une instanciation polymorphe indépendante.
     * @param f la signature de fonction d’origine (potentiellement polymorphe)
     * @return une nouvelle FunctionType où toutes les variables de type sont fraîchement renommées
     */
    private FunctionType freshFunctionType(FunctionType f) {
        Map<UnknownType, UnknownType> mapping = new HashMap<>();
        ArrayList<Type> newArgs = new ArrayList<>();
        for (Type arg : f.getArgsTypes()) {
            newArgs.add(freshType(arg, mapping));
        }
        Type newReturn = freshType(f.getReturnType(), mapping);
        return new FunctionType(newReturn, newArgs);
    }

    /**
     * Renomme fraîchement toutes les UnknownType contenues dans un type donné en utilisant un mapping partagé.
     * Gère récursivement les ArrayType et FunctionType.
     * @param t        le type source potentiellement contenant des UnknownType
     * @param mapping  correspondance entre anciennes et nouvelles UnknownType (mutualisée sur un appel)
     * @return un type équivalent structurellement où les UnknownType ont été remplacées par de nouvelles
     */
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
            return t;
        }
    }
    /**
     * Retourne la table des symboles avec tous les types résolus.
     */
    public Map<String, Type> getSymbolTable() {
        Map<String, Type> resolvedTable = new HashMap<>();

        for (Map.Entry<String, Type> entry : this.symbolTable.entrySet()) {
            // On applique toutes les substitutions connues pour avoir le vrai type (INT/BOOL)
            resolvedTable.put(entry.getKey(), entry.getValue().substituteAll(this.types));
        }

        return resolvedTable;
    }
}