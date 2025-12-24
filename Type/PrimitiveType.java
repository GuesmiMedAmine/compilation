package Type;
import java.util.Map;

public  class PrimitiveType extends Type {
    private Type.Base type; 
    
    /**
     * Constructeur
     * @param type type de base
     */
    public PrimitiveType(Type.Base type) {
        this.type = type;
    }

    /**
     * Getter du type
     * @return type
     */
    public Type.Base getType() {
        return type;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {

        //si c unkowntype on laisse gérer, si c le meme => succées map vide
        if (t instanceof UnknownType) {
            return t.unify(this); // Délégation
        }
        if (t instanceof PrimitiveType && this.type == ((PrimitiveType)t).type) {
            return new java.util.HashMap<>(); // Map vide = Succès sans substitution
        }
        return null; // Impossible d'unifier (ex: INT vs BOOL)
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
         return this;
    }

    @Override
    public boolean contains(UnknownType v) {
        return false; //un type primitif ne contient pas d'UnknownType'
    }

    @Override
    public boolean equals(Object t) {
        //comparer 2 types ex int = int
        if (this == t) return true;
        if (!(t instanceof PrimitiveType)) return false;
        return this.type == ((PrimitiveType) t).type;
    }

    @Override
    public String toString() {
        return type.toString().toLowerCase();
    }

}
