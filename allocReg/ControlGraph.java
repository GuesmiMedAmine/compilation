package allocReg;

import Graph.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlGraph extends OrientedGraph<String> {
    private String fileName;
    private ArrayList<String> instructions;
    private ArrayList<String> ops = new ArrayList<>(List.of("JMP","JINF","JEQU","JSUP","JNEQ","JIEQ","JSEQ"));
    private HashMap<String, String> labelMap;

    public ControlGraph(String fileName){
        this.fileName = fileName;
        this.instructions = new ArrayList<>();
        this.labelMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    this.instructions.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getInstructions(){
        return this.instructions;
    }

    public OrientedGraph<String> getGraph() {
        // Premier passage : construire la map des labels
        for (String instruction : instructions) {
            String[] words = instruction.trim().split("\\s+");
            if (words.length > 0 && words[0].endsWith(":")) {
                String label = words[0].substring(0, words[0].length() - 1);
                labelMap.put(label, instruction);
            }
        }

        // Second passage : construire le graphe
        for (int i = 0; i < instructions.size(); i++) {
            String instruction = instructions.get(i).trim();

            if (instruction.equals("STOP")) {
                continue; // Pas de successeur
            }

            String[] words = instruction.split("\\s+");
            int opIndex = 0;

            if (words[0].endsWith(":")) {
                opIndex = 1;
            }

            if (opIndex >= words.length) continue;

            String opcode = words[opIndex];

            // JUMP inconditionnel
            if (opcode.equals("JMP")) {
                String targetLabel = words[opIndex + 1];
                if (labelMap.containsKey(targetLabel)) {
                    this.addEdge(instruction, labelMap.get(targetLabel));
                }
            }
            // CALL : appel de fonction
            else if (opcode.equals("CALL")) {
                String targetLabel = words[opIndex + 1];
                if (labelMap.containsKey(targetLabel)) {
                    this.addEdge(instruction, labelMap.get(targetLabel));
                }
                // Après le CALL, on continue à l'instruction suivante
                if (i + 1 < instructions.size()) {
                    this.addEdge(instruction, instructions.get(i + 1));
                }
            }
            // RET : retour de fonction (pas de successeur direct, géré par la pile)
            else if (opcode.equals("RET")) {
                // Pas d'arc ajouté, le retour est dynamique
                continue;
            }
            // Branchements conditionnels
            else if (ops.contains(opcode)) {
                String targetLabel = words[opIndex + 3];

                if (i + 1 < instructions.size()) {
                    this.addEdge(instruction, instructions.get(i + 1));
                }

                if (labelMap.containsKey(targetLabel)) {
                    this.addEdge(instruction, labelMap.get(targetLabel));
                }
            }
            // Instruction séquentielle normale
            else {
                if (i + 1 < instructions.size()) {
                    this.addEdge(instruction, instructions.get(i + 1));
                }
            }
        }

        return this;
    }

    public ArrayList<String> getOps(){
        return ops;
    }
}