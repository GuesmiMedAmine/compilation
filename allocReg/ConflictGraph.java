package allocReg;

import Graph.UnorientedGraph;
import java.util.ArrayList;
import java.util.HashSet;

public class ConflictGraph extends UnorientedGraph<String> {
    private ControlGraph cfg;
    private CalculLV calculLV;
    private ArrayList<String> variables;
    private HashSet<String> varsEnMemoire;
    private HashSet<String> reservedRegs;

    public ConflictGraph(ControlGraph cfg, CalculLV calculLV) {
        this(cfg, calculLV, new HashSet<>(), new HashSet<>());
    }

    public ConflictGraph(ControlGraph cfg, CalculLV calculLV, HashSet<String> varsEnMemoire, HashSet<String> reservedRegs) {
        this.cfg = cfg;
        this.calculLV = calculLV;
        this.varsEnMemoire = varsEnMemoire;
        this.reservedRegs = reservedRegs;
        this.variables = new ArrayList<>();
    }

    public ArrayList<String> listeVariable(ControlGraph cfg) {
        HashSet<String> varSet = new HashSet<>();

        for (String instruction : cfg.getVertices()) {
            String[] words = instruction.trim().split("\\s+");

            for (String word : words) {
                // Un registre commence par R suivi de chiffres
                if (word.matches("R\\d+")) {
                    // Ne pas inclure les variables en mémoire ni les registres réservés
                    if (!varsEnMemoire.contains(word) && !reservedRegs.contains(word)) {
                        varSet.add(word);
                    }
                }
            }
        }

        return new ArrayList<>(varSet);
    }

    public UnorientedGraph<String> getGraph() {
        ArrayList<String> instructions = this.cfg.getVertices();
        this.variables = listeVariable(this.cfg);

        // Ajouter tous les sommets
        for (String variable : this.variables) {
            this.addVertex(variable);
        }

        // Pour chaque instruction, ajouter les arêtes de conflit
        for (String instruction : instructions) {
            HashSet<String> tuees = calculLV.kill(instruction);
            HashSet<String> vivantes = calculLV.getLVexit(instruction);

            // Retirer les variables en mémoire et réservées
            tuees.removeAll(varsEnMemoire);
            tuees.removeAll(reservedRegs);

            vivantes.removeAll(varsEnMemoire);
            vivantes.removeAll(reservedRegs);

            vivantes.removeAll(tuees);

            for (String varTuee : tuees) {
                for (String varVivante : vivantes) {
                    if (!varTuee.equals(varVivante)) {
                        this.addEdge(varTuee, varVivante);
                    }
                }
            }
        }

        return this;
    }

    // Retourne le degré (nombre de conflits) d'une variable
    public int getDegree(String var) {
        ArrayList<String> neighbors = this.getNeighbors(var);
        return neighbors != null ? neighbors.size() : 0;
    }
}
