package Type;
import java.util.Map;

public class ArrayType extends Type{
    private Type tabType;
    
    /**
     * Constructeur
     * @param t type des éléments du tableau
     */
    public ArrayType(Type t) {
        this.tabType = t;
    }

    /**
     * Getter du type des éléments du tableau
     * @return type des éléments du tableau
     */
    public Type getTabType() {
       return tabType;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        if (t instanceof ArrayType) {
            return tabType.unify(((ArrayType) t).tabType);
        }
        if (t instanceof UnknownType) {
            return Map.of((UnknownType) t, this);
        }
        return null;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        return new ArrayType(tabType.substitute(v, t));
    }

    @Override
    public boolean contains(UnknownType v) {
        return tabType.contains(v);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ArrayType && tabType.equals(((ArrayType) o).tabType);
    }

    @Override
    public String toString() {
        return tabType + "[]";
    }

    
}