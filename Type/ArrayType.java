package Type;

import java.util.Map;

/**
 * Classe représentant un type tableau.
 * Un ArrayType contient un type des éléments du tableau (tabType).
 */
public class ArrayType extends Type {
    // Le type des éléments contenus dans le tableau
    private Type tabType;
    
    /**
     * Constructeur
     * @param t type des éléments du tableau
     */
    public ArrayType(Type t) {
        this.tabType = t; // Initialisation du type des éléments
    }

    /**
     * Getter du type des éléments du tableau
     * @return type des éléments du tableau
     */
    public Type getTabType() {
        return tabType; // Retourne le type des éléments
    }

    /**
     * Méthode de unification des types.
     * Essaie de faire correspondre ce type avec un autre type.
     * @param t type à unifier avec ce tableau
     * @return une Map associant les UnknownType à leur substitution, ou null si unification impossible
     */
    @Override
    public Map<UnknownType, Type> unify(Type t) {
        if (t instanceof ArrayType) { 
            // Si l'autre type est aussi un tableau, on unifie les types des éléments
            return tabType.unify(((ArrayType) t).tabType);
        }
        if (t instanceof UnknownType) {
            // Si l'autre type est inconnu, on peut le substituer par ce type de tableau
            return Map.of((UnknownType) t, this);
        }
        // Sinon, l'unification échoue
        return null;
    }

    /**
     * Substitue un type inconnu par un type concret dans ce tableau.
     * @param v type inconnu à remplacer
     * @param t type concret de substitution
     * @return un nouveau ArrayType avec le type substitué
     */
    @Override
    public Type substitute(UnknownType v, Type t) {
        return new ArrayType(tabType.substitute(v, t));
        // On crée un nouveau ArrayType avec le type des éléments mis à jour
    }

    /**
     * Vérifie si ce tableau contient un type inconnu spécifique.
     * @param v type inconnu à rechercher
     * @return true si tabType contient v, false sinon
     */
    @Override
    public boolean contains(UnknownType v) {
        return tabType.contains(v); // Délégation au type des éléments
    }

    /**
     * Vérifie l'égalité entre ce type et un autre objet.
     * @param o objet à comparer
     * @return true si o est un ArrayType avec le même type d'éléments
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof ArrayType && tabType.equals(((ArrayType) o).tabType);
    }

    /**
     * Représentation sous forme de chaîne.
     * Exemple : si tabType est int, toString renvoie "int[]"
     */
    @Override
    public String toString() {
        return tabType + "[]";
    }
}