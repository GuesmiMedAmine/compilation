package allocReg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AllocationReg {
    private static final int MAX_REGISTERS = 32;
    private static final int TEMP_REG = 30;
    private static final int ADDR_REG = 31;

    private String fileName;
    private ControlGraph cfg;
    private CalculLV calculLV;
    private ConflictGraph conflictGraph;
    private int nbColors;
    private HashMap<String, Integer> AffecterReg;
    private HashSet<String> varsEnMemoire;
    private HashMap<String, Integer> emplacementsMemoire;
    private HashSet<String> reservedRegs;

    public AllocationReg(String fileName) {
        this.fileName = fileName;
        this.varsEnMemoire = new HashSet<>();
        this.emplacementsMemoire = new HashMap<>();
        this.reservedRegs = new HashSet<>();

        reservedRegs.add("SP");
        reservedRegs.add("R30");
        reservedRegs.add("R31");

        this.cfg = new ControlGraph(fileName);
        this.cfg.getGraph();

        this.calculLV = new CalculLV(this.cfg);

        this.conflictGraph = new ConflictGraph(this.cfg, this.calculLV, this.varsEnMemoire, this.reservedRegs);
        this.conflictGraph.getGraph();

        this.nbColors = this.conflictGraph.color();

        if (this.nbColors > MAX_REGISTERS) {
            System.out.println("\nLe graphe nécessite " + this.nbColors + " couleurs.");
            System.out.println("Déplacement de variables en mémoire : \n");
            gererDepassementRegistres();
        }

        this.AffecterReg = new HashMap<>();
        for (String variable : this.conflictGraph.getVertices()) {
            this.AffecterReg.put(variable, this.conflictGraph.getColor(variable));
        }

        for (String reg : reservedRegs) {
            if (reg.equals("SP")) {
                AffecterReg.put("SP", 29);
            }
        }
    }

    private void gererDepassementRegistres() {
        int iteration = 0;

        while (nbColors > MAX_REGISTERS && iteration < 100) {
            iteration++;

            String varADeplacer = choisirVariableADeplacer();

            if (varADeplacer == null) {
                System.err.println("Erreur : impossible de trouver une variable à déplacer.");
                break;
            }

            varsEnMemoire.add(varADeplacer);
            System.out.println("  Déplacement de " + varADeplacer + " en mémoire (conflits: " + conflictGraph.getDegree(varADeplacer) + ")");

            this.conflictGraph = new ConflictGraph(this.cfg, this.calculLV, this.varsEnMemoire, this.reservedRegs);
            this.conflictGraph.getGraph();

            this.nbColors = this.conflictGraph.color();
        }

        if (nbColors <= MAX_REGISTERS) {
            System.out.println("\nRéussite : " + varsEnMemoire.size() + " variables en mémoire");
            System.out.println("  Nouvelles couleurs nécessaires : " + nbColors);

            int offset = 0;
            for (String var : varsEnMemoire) {
                emplacementsMemoire.put(var, offset);
                offset += 4;
            }
        } else {
            System.err.println("\nÉchec après " + iteration + " itérations");
        }
    }

    private String choisirVariableADeplacer() {
        String varMax = null;
        int conflitsMax = -1;

        for (String var : conflictGraph.getVertices()) {
            if (varsEnMemoire.contains(var) || reservedRegs.contains(var)) {
                continue;
            }

            int conflits = conflictGraph.getDegree(var);
            if (conflits > conflitsMax) {
                conflitsMax = conflits;
                varMax = var;
            }
        }

        return varMax;
    }

    public int getRegistre(String variable) {
        if (this.AffecterReg.containsKey(variable)) {
            return this.AffecterReg.get(variable);
        }
        return -1;
    }

    public boolean is32Colorable() {
        return this.nbColors <= MAX_REGISTERS;
    }

    public int getNbColors() {
        return this.nbColors;
    }

    public void afficherDebug() {
        System.out.println("GRAPHE DE CONTROLE");
        System.out.println(this.cfg);

        System.out.println("\nENSEMBLES LV");
        this.calculLV.afficherLVtab();

        System.out.println("\nGRAPHE DE CONFLITS");
        System.out.println(this.conflictGraph);

        System.out.println("\nCOLORATION");
        System.out.println("Nombre de couleurs utilisées : " + this.nbColors);

        if (!varsEnMemoire.isEmpty()) {
            System.out.println("\nVariables stockées en mémoire :");
            for (String var : varsEnMemoire) {
                System.out.println("  " + var + " -> @SP+" + emplacementsMemoire.get(var));
            }
        }

        System.out.println("\nAffectation des registres :");
        for (String variable : this.conflictGraph.getVertices()) {
            int reg = this.getRegistre(variable);
            System.out.println("  " + variable + " -> R" + reg);
        }

        if (this.is32Colorable()) {
            System.out.println("\nLe graphe est 32-colorable ! Allocation réussie.");
        } else {
            System.out.println("\nLe graphe nécessite " + this.nbColors + " couleurs.");
            System.out.println("  Allocation impossible même avec stockage mémoire.");
        }
    }

    public ControlGraph getControlGraph() {
        return this.cfg;
    }

    public CalculLV getCalculLV() {
        return this.calculLV;
    }

    public ConflictGraph getConflictGraph() {
        return this.conflictGraph;
    }

    public String reecriture() {
        ArrayList<String> texte = cfg.getInstructions();
        System.out.println("INSTRUCTIONS CFG :");
        for (String s : texte) {
            System.out.println("[" + s + "]");
        }
        StringBuilder reecriture = new StringBuilder();
        ArrayList<String> vertices = this.conflictGraph.getVertices();

        int spReg = AffecterReg.getOrDefault("SP", 29);

        if (!varsEnMemoire.isEmpty()) {
            reecriture.append("# Code avec variables en mémoire : ").append(varsEnMemoire.size()).append(" variable(s)\n");
            reecriture.append("# Variables concernées : ").append(varsEnMemoire).append("\n\n");
        }

        for (String instruction : texte) {
            String instructionTrim = instruction.trim();

            if (instructionTrim.isEmpty() || instructionTrim.startsWith("#")) {
                reecriture.append(instruction).append("\n");
                continue;
            }

            String[] words = instructionTrim.split("\\s+");

            int opIndex = words[0].endsWith(":") ? 1 : 0;
            if (opIndex < words.length && words[opIndex].equals("CALL")) {
                reecriture.append(genererSauvegardeCALL(instruction, spReg));
                continue;
            }

            if (opIndex < words.length && words[opIndex].equals("RET")) {
                reecriture.append(genererRestaurationRET(instruction));
                continue;
            }

            HashSet<String> varsMemUtilisees = new HashSet<>();
            for (String word : words) {
                if (varsEnMemoire.contains(word) && !varsMemUtilisees.contains(word)) {
                    varsMemUtilisees.add(word);
                    int offset = emplacementsMemoire.get(word);
                    reecriture.append("ADDi R").append(ADDR_REG).append(" R").append(spReg).append(" ").append(offset).append("\n");
                    reecriture.append("LD R").append(TEMP_REG).append(" R").append(ADDR_REG).append("\n");
                }
            }

            StringBuilder line = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                String word = words[i];

                if (i > 0 && (words[i-1].equals("JMP") || words[i-1].equals("CALL"))) {
                    line.append(word).append(" ");
                } else if (word.endsWith(":")) {
                    line.append(word).append(" ");
                } else if (word.equals("SP")) {
                    line.append("R").append(spReg).append(" ");
                } else if (varsEnMemoire.contains(word)) {
                    line.append("R").append(TEMP_REG).append(" ");
                } else if (vertices.contains(word)) {
                    int reg = getRegistre(word);
                    if (reg != -1) {
                        line.append("R").append(reg).append(" ");
                    } else {
                        line.append(word).append(" ");
                    }
                } else if (reservedRegs.contains(word) && !word.startsWith("R")) {
                    int reg = getRegistre(word);
                    if (reg != -1) {
                        line.append("R").append(reg).append(" ");
                    } else {
                        line.append(word).append(" ");
                    }
                } else {
                    line.append(word).append(" ");
                }
            }

            reecriture.append(line.toString().trim()).append("\n");

            HashSet<String> killed = calculLV.kill(instruction);
            for (String var : killed) {
                if (varsEnMemoire.contains(var)) {
                    int offset = emplacementsMemoire.get(var);
                    reecriture.append("ADDi R").append(ADDR_REG).append(" R").append(spReg).append(" ").append(offset).append("\n");
                    reecriture.append("ST R").append(TEMP_REG).append(" R").append(ADDR_REG).append("\n");
                }
            }
        }

        return reecriture.toString();
    }

    private String genererSauvegardeCALL(String callInstruction, int spReg) {
        StringBuilder code = new StringBuilder();

        HashSet<String> vivantes = calculLV.getLVentry(callInstruction);

        ArrayList<Integer> regsToSave = new ArrayList<>();

        for (String var : vivantes) {
            if (AffecterReg.containsKey(var)) {
                int reg = AffecterReg.get(var);
                if (reg >= 0 && reg <= 15 && !regsToSave.contains(reg)) {
                    regsToSave.add(reg);
                }
            }
        }

        for (int reg : regsToSave) {
            code.append("SUBi R").append(spReg).append(" R").append(spReg).append(" 4\n");
            code.append("ST R").append(reg).append(" R").append(spReg).append("\n");
        }

        String[] words = callInstruction.trim().split("\\s+");
        StringBuilder callLine = new StringBuilder();
        for (String word : words) {
            if (word.equals("SP")) {
                callLine.append("R").append(spReg).append(" ");
            } else if (AffecterReg.containsKey(word)) {
                int reg = AffecterReg.get(word);
                callLine.append("R").append(reg).append(" ");
            } else {
                callLine.append(word).append(" ");
            }
        }
        code.append(callLine.toString().trim()).append("\n");

        for (int i = regsToSave.size() - 1; i >= 0; i--) {
            int reg = regsToSave.get(i);
            code.append("LD R").append(reg).append(" R").append(spReg).append("\n");
            code.append("ADDi R").append(spReg).append(" R").append(spReg).append(" 4\n");
        }

        return code.toString();
    }

    private String genererRestaurationRET(String retInstruction) {
        return retInstruction + "\n";
    }

    public void reecritureOutput() {
        String texte = reecriture();
        Path output = Path.of("prog.asm");

        try {
            BufferedWriter writer = Files.newBufferedWriter(output);
            writer.write(texte);
            writer.close();
            System.out.println("\nFichier prog.asm généré avec succès.");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
