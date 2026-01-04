package allocReg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Graph.*;

public class CalculLV {

    private HashMap<Integer, ArrayList<HashSet<String>>> LVtab = new HashMap<>();
    private HashMap<String, Integer> blocToId = new HashMap<>();
    private ArrayList<String> idToBloc = new ArrayList<>();

    public HashSet<String> getLVentry(String bloc) {
        return this.LVtab.get(this.blocToId.get(bloc.trim())).get(0);
    }

    public HashSet<String> getLVexit(String bloc) {
        return this.LVtab.get(this.blocToId.get(bloc.trim())).get(1);
    }

    public CalculLV(ControlGraph cfg) {
        this.LVtab = initTab(new HashMap<>(), cfg);
        this.LVtab = remplirLVliste(cfg, this.LVtab);
    }

    public void afficherLVtab() {
        for (int id : this.LVtab.keySet()) {
            ArrayList<HashSet<String>> coupleLV = this.LVtab.get(id);
            String key = idToBloc.get(id);
            System.out.println(key + " :");
            System.out.println("\tLVentry = " + afficherHashSet(coupleLV.get(0)));
            System.out.println("\tLVexit  = " + afficherHashSet(coupleLV.get(1)));
        }
    }

    private String afficherHashSet(HashSet<String> set) {
        return "{" + String.join(", ", set) + "}";
    }

    public static ArrayList<String> getOpUAL() {
        ArrayList<String> opUAL = new ArrayList<>();
        opUAL.add("ADD");
        opUAL.add("SUB");
        opUAL.add("OR");
        opUAL.add("AND");
        opUAL.add("XOR");
        opUAL.add("SL");
        opUAL.add("SR");
        opUAL.add("MUL");
        opUAL.add("DIV");
        opUAL.add("MOD");
        return opUAL;
    }

    public static ArrayList<String> getOpUALi() {
        ArrayList<String> opUALi = new ArrayList<>();
        opUALi.add("ADDi");
        opUALi.add("SUBi");
        opUALi.add("ORi");
        opUALi.add("ANDi");
        opUALi.add("XORi");
        opUALi.add("SLi");
        opUALi.add("SRi");
        opUALi.add("MULi");
        opUALi.add("DIVi");
        opUALi.add("MODi");
        return opUALi;
    }

    public static ArrayList<String> getOpJMPC(ControlGraph cfg) {
        return cfg.getOps();
    }

    private int getOpcodeIndex(String[] words) {
        return (words[0].endsWith(":")) ? 1 : 0;
    }

    public ArrayList<HashSet<String>> remplirBloc(ControlGraph cfg, String bloc, HashMap<Integer, ArrayList<HashSet<String>>> LVtableau) {
        HashSet<String> LVentryBloc = new HashSet<>();
        HashSet<String> LVexitBloc = new HashSet<>();
        HashSet<String> ancienLVentryBloc;
        HashSet<String> ancienLVexitBloc;

        do {
            ancienLVentryBloc = new HashSet<>(LVentryBloc);
            ancienLVexitBloc = new HashSet<>(LVexitBloc);
            LVexitBloc = calculNewLVexit(LVtableau, cfg, bloc);
            LVentryBloc = calculNewLVentry(kill(bloc), gen(bloc, cfg), LVexitBloc);
        } while (!LVexitBloc.equals(ancienLVexitBloc) || !LVentryBloc.equals(ancienLVentryBloc));

        ArrayList<HashSet<String>> res = new ArrayList<>();
        res.add(new HashSet<>(LVentryBloc));
        res.add(new HashSet<>(LVexitBloc));
        return res;
    }

    public HashMap<Integer, ArrayList<HashSet<String>>> remplirLVliste(ControlGraph cfg, HashMap<Integer, ArrayList<HashSet<String>>> LVtableau) {
        boolean modifie;
        do {
            modifie = false;
            for (String bloc : cfg.getVertices()) {
                int blocID = blocToId.get(bloc);
                ArrayList<HashSet<String>> ancien = LVtableau.get(blocID);
                ArrayList<HashSet<String>> nouveau = remplirBloc(cfg, bloc, LVtableau);

                if (!ancien.get(0).equals(nouveau.get(0)) || !ancien.get(1).equals(nouveau.get(1))) {
                    LVtableau.replace(blocID, nouveau);
                    modifie = true;
                }
            }
        } while (modifie);

        return LVtableau;
    }

    public HashSet<String> calculNewLVentry(HashSet<String> kill, HashSet<String> gen, HashSet<String> LVexit) {
        HashSet<String> res = new HashSet<>();
        res.addAll(LVexit);
        res.removeAll(kill);
        res.addAll(gen);
        return res;
    }

    public HashSet<String> calculNewLVexit(HashMap<Integer, ArrayList<HashSet<String>>> LVtableau, ControlGraph cfg, String bloc) {
        HashSet<String> res = new HashSet<>();
        ArrayList<String> neighbors = cfg.getOutNeighbors(bloc);

        if (neighbors != null) {
            for (String neighbor : neighbors) {
                Integer neighborId = blocToId.get(neighbor);
                if (neighborId != null) {
                    ArrayList<HashSet<String>> tableau = LVtableau.get(neighborId);
                    if (tableau != null) {
                        res.addAll(tableau.get(0));
                    }
                }
            }
        }
        return res;
    }

    public HashMap<Integer, ArrayList<HashSet<String>>> initTab(HashMap<Integer, ArrayList<HashSet<String>>> LVtableau, ControlGraph cfg) {
        cfg.getGraph();
        int id = 0;

        for (int i = 0; i < cfg.getVertices().size(); i++) {
            ArrayList<HashSet<String>> tab = new ArrayList<>();
            tab.add(new HashSet<>());
            tab.add(new HashSet<>());
            LVtableau.put(i, tab);
            String bloc = cfg.getVertices().get(i);
            blocToId.put(bloc, id);
            idToBloc.add(bloc);
            id++;
        }
        return LVtableau;
    }

    public HashSet<String> kill(String bloc) {
        HashSet<String> res = new HashSet<>();
        String[] mot = bloc.trim().split("\\s+");

        if (mot.length == 0) return res;

        int i = getOpcodeIndex(mot);

        if (i >= mot.length) return res;

        String opcode = mot[i];

        if (opcode.equals("IN") || opcode.equals("READ") || opcode.equals("LD")) {
            if (i + 1 < mot.length) {
                res.add(mot[i + 1]);
            }
        } else if (getOpUAL().contains(opcode) || getOpUALi().contains(opcode)) {
            if (i + 1 < mot.length) {
                res.add(mot[i + 1]);
            }
        }
        // CALL ne tue aucun registre explicitement
        // RET ne tue rien non plus

        return res;
    }

    public HashSet<String> gen(String bloc, ControlGraph cfg) {
        HashSet<String> res = new HashSet<>();
        String[] mot = bloc.trim().split("\\s+");

        if (mot.length == 0) return res;

        int i = getOpcodeIndex(mot);

        if (i >= mot.length) return res;

        String opcode = mot[i];

        if (getOpUAL().contains(opcode)) {
            if (i + 2 < mot.length) {
                res.add(mot[i + 2]);
            }
            if (!opcode.equals("SL") && !opcode.equals("SR") && i + 3 < mot.length) {
                res.add(mot[i + 3]);
            }
        }
        else if (getOpUALi().contains(opcode)) {
            if (i + 2 < mot.length) {
                res.add(mot[i + 2]);
            }
        }
        else if (opcode.equals("LD")) {
            if (i + 2 < mot.length) {
                res.add(mot[i + 2]);
            }
        }
        else if (opcode.equals("ST")) {
            if (i + 1 < mot.length) {
                res.add(mot[i + 1]);
            }
            if (i + 2 < mot.length) {
                res.add(mot[i + 2]);
            }
        }
        else if (getOpJMPC(cfg).contains(opcode)) {
            if (i + 1 < mot.length) {
                res.add(mot[i + 1]);
            }
            if (i + 2 < mot.length) {
                res.add(mot[i + 2]);
            }
        }
        else if (opcode.equals("OUT") || opcode.equals("PRINT")) {
            if (i + 1 < mot.length) {
                res.add(mot[i + 1]);
            }
        }
        // CALL : on ne marque rien comme "gen" ici car c'est géré dans l'allocation
        // RET : potentiellement utilise un registre de retour, mais dépend de la convention

        return res;
    }
}