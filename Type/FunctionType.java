package Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe représentant un type fonction.
 * Une fonction a un type de retour (returnType) et une liste de types d'arguments (argsTypes).
 */
public class FunctionType extends Type {
    // Type de retour de la fonction
    private Type returnType;
    // Liste des types des arguments de la fonction
    private ArrayList<Type> argsTypes;

    /**
     * Constructeur
     * @param returnType type de retour de la fonction
     * @param argsTypes liste des types des arguments
     */
    public FunctionType(Type returnType, ArrayList<Type> argsTypes) {
        this.returnType = returnType; // initialisation du type de retour
        this.argsTypes = argsTypes;   // initialisation de la liste des types d'arguments
    }

    // Getter pour le type de retour
    public Type getReturnType() {
        return returnType;
    }

    // Getter pour le type d'un argument spécifique
    public Type getArgsType(int i) {
        return argsTypes.get(i);
    }

    // Retourne le nombre d'arguments de la fonction
    public int getNbArgs() {
        return argsTypes.size();
    }

    // Getter pour la liste complète des types d'arguments
    public ArrayList<Type> getArgsTypes() {
        return argsTypes;
    }

    /**
     * Méthode d'unification des types.
     * Essaie de faire correspondre cette fonction avec un autre type.
     * @param t type à unifier
     * @return map des substitutions de types inconnus ou null si échec
     */
    @Override
    public Map<UnknownType, Type> unify(Type t) {
        if (!(t instanceof FunctionType)) {
            // Si t n'est pas une fonction mais un type inconnu, on peut le substituer
            if (t instanceof UnknownType) return Map.of((UnknownType) t, this);
            return null; // sinon l'unification échoue
        }

        FunctionType ft = (FunctionType) t;
        if (argsTypes.size() != ft.argsTypes.size()) return null; // nombres d'arguments différents

        Map<UnknownType, Type> subst = new HashMap<>();

        // Unification des types des arguments un par un
        for (int i = 0; i < argsTypes.size(); i++) {
            // applique les substitutions déjà trouvées pour les types précédents
            Type a1 = argsTypes.get(i).substituteAll(subst);
            Type a2 = ft.argsTypes.get(i).substituteAll(subst);

            // unifie les types des arguments
            Map<UnknownType, Type> s = a1.unify(a2);
            if (s == null) return null; // échec si un argument ne peut être unifié
            subst.putAll(s); // ajoute les nouvelles substitutions
        }

        // Unification des types de retour
        Type ret1 = returnType.substituteAll(subst);
        Type ret2 = ft.returnType.substituteAll(subst);
        Map<UnknownType, Type> sret = ret1.unify(ret2);
        if (sret == null) return null; // échec si types de retour incompatibles
        subst.putAll(sret);

        return subst; // retourne la map des substitutions trouvées
    }

    /**
     * Substitution d'un type inconnu par un type concret.
     */
    @Override
    public Type substitute(UnknownType v, Type t) {
        ArrayList<Type> newArgs = new ArrayList<>();
        for (Type arg : argsTypes) {
            newArgs.add(arg.substitute(v, t)); // applique la substitution à chaque argument
        }
        // applique la substitution au type de retour
        return new FunctionType(returnType.substitute(v, t), newArgs);
    }

    /**
     * Vérifie si cette fonction contient un type inconnu spécifique.
     */
    @Override
    public boolean contains(UnknownType v) {
        if (returnType.contains(v)) return true; // vérifie le type de retour
        for (Type arg : argsTypes) if (arg.contains(v)) return true; // vérifie chaque argument
        return false;
    }

    /**
     * Vérifie l'égalité avec un autre objet.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionType)) return false;
        FunctionType ft = (FunctionType) o;
        if (!returnType.equals(ft.returnType)) return false; // types de retour différents
        if (argsTypes.size() != ft.argsTypes.size()) return false; // nombre d'arguments différent
        for (int i = 0; i < argsTypes.size(); i++)
            if (!argsTypes.get(i).equals(ft.argsTypes.get(i))) return false; // un argument diffère
        return true;
    }

    /**
     * Représentation sous forme de chaîne.
     * Exemple : (int, bool) -> int
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < argsTypes.size(); i++) {
            sb.append(argsTypes.get(i));
            if (i < argsTypes.size() - 1) sb.append(", "); // séparateur entre arguments
        }
        sb.append(") -> ").append(returnType); // ajoute le type de retour
        return sb.toString();
    }
}
