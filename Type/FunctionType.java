package Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FunctionType extends Type {
    private Type returnType;
    private ArrayList<Type> argsTypes;
    
    /**
     * Constructeur
     * @param returnType type de retour
     * @param argsTypes liste des types des arguments
     */
    public FunctionType(Type returnType, ArrayList<Type> argsTypes) {
        this.returnType = returnType;
        this.argsTypes = argsTypes;
    }

    /**
     * Getter du type de retour
     * @return type de retour
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Getter du type du i-eme argument
     * @param i entier
     * @return type du i-eme argument
     */
    public Type getArgsType(int i) {
        return argsTypes.get(i);
    }

    /**
     * Getter du nombre d'arguments
     * @return nombre d'arguments
     */
    public int getNbArgs() {
        return argsTypes.size();
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        // Identité
        if (this.equals(t)) return new HashMap<>();

        // t est inconnu
        if (t instanceof UnknownType) {
            UnknownType u = (UnknownType) t;
            if (this.contains(u)) return null; // Occurs Check
            return Map.of(u, this);
        }

        // Composite : doit être FunctionType
        if (!(t instanceof FunctionType)) return null;
        FunctionType ft = (FunctionType) t;

        // Vérifier le nombre d'arguments
        if (argsTypes.size() != ft.argsTypes.size()) return null;

        Map<UnknownType, Type> subst = new HashMap<>();

        // Unifier les arguments un par un
        for (int i = 0; i < argsTypes.size(); i++) {
            Type arg1 = argsTypes.get(i).substituteAll(subst);
            Type arg2 = ft.argsTypes.get(i).substituteAll(subst);

            Map<UnknownType, Type> s = arg1.unify(arg2);
            if (s == null) return null;

            // Composer les substitutions accumulées
            subst = compose(subst, s);
        }

        // Unifier le type de retour
        Type ret1 = returnType.substituteAll(subst);
        Type ret2 = ft.returnType.substituteAll(subst);

        Map<UnknownType, Type> sret = ret1.unify(ret2);
        if (sret == null) return null;

        subst = compose(subst, sret);

        return subst;
    }
    // Utilitaire : compose deux substitutions
    private Map<UnknownType, Type> compose(Map<UnknownType, Type> s1, Map<UnknownType, Type> s2) {
        Map<UnknownType, Type> result = new HashMap<>(s1);
        for (var e : s2.entrySet()) {
            Type t = e.getValue().substituteAll(s1);
            result.put(e.getKey(), t);
        }
        return result;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        ArrayList<Type> newArgs = new ArrayList<>();
        for (Type arg : argsTypes) {
            newArgs.add(arg.substitute(v, t));
        }
        return new FunctionType(returnType.substitute(v, t), newArgs);
    }

    @Override
    public boolean contains(UnknownType v) {
        if (returnType.contains(v)) return true;
        for (Type arg : argsTypes) {
            if (arg.contains(v)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionType)) return false;
        FunctionType ft = (FunctionType) o;
        if (argsTypes.size() != ft.argsTypes.size()) return false;
        if (!returnType.equals(ft.returnType)) return false;
        for (int i = 0; i < argsTypes.size(); i++) {
            if (!argsTypes.get(i).equals(ft.argsTypes.get(i))) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < argsTypes.size(); i++) {
            sb.append(argsTypes.get(i));
            if (i < argsTypes.size() - 1) sb.append(", ");
        }
        sb.append(") -> ").append(returnType);
        return sb.toString();
    }

}
