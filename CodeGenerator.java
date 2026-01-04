import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import Asm.*;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Type.Type;
import Type.ArrayType;
import Type.UnknownType;

public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private int nbRegister = 1;
    private int nbLabels = 3;
    private int stackPointer = 0;
    private Dictionary<String, Integer> varToReg = new Hashtable<>();
    //  nomFormat=fctName-NbParam -> paramName
    private Dictionary<String, String> paramToVar = new Hashtable<>();
    private Map<String, Type> types;


    /**
     * Constructeur
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<String, Type> types) {
        this.types = types;
    }

    public int getNewRegister() {
        nbRegister++;
        return nbRegister;
    }

    public String getNewLabel() {
        nbLabels++;
        return "label" + nbLabels;
    }

    public Instruction getLabelInstruction(String labelName) {
        // Renvoie une instruction vide qui permet de sauter à un endroit spécifique
        return new UAL(labelName, UAL.Op.XOR, 0, 0, 0);
    }

    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        Program pCtx = visit(ctx.getChild(1));
        int addr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pCtx);
        p.addInstruction(new UALi(UALi.Op.SUB, getNewRegister(), addr, 1));
        int tempAddr = this.nbRegister;
        p.addInstruction(new UAL(UAL.Op.SUB, getNewRegister(), 0, tempAddr));
        return p;
    }

    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);

        String trueLabel = getNewLabel();
        String falseLabel = getNewLabel();
        String endLabel = getNewLabel();

        String ope = ctx.getChild(1).getText();

        if(ope.equals(">")) {
            p.addInstruction(new CondJump(CondJump.Op.JSUP, leftAddr, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        } else if(ope.equals("<")) {
            p.addInstruction(new CondJump(CondJump.Op.JINF, leftAddr, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        } else if(ope.equals(">=")) {
            int newReg = getNewRegister();
            p.addInstruction(new UALi(UALi.Op.SUB, newReg, leftAddr, 1));
            p.addInstruction(new CondJump(CondJump.Op.JSUP, newReg, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        } else {
            // Cas ope == '<='
            int newReg = getNewRegister();
            p.addInstruction(new UALi(UALi.Op.ADD, newReg, leftAddr, 1));
            p.addInstruction(new CondJump(CondJump.Op.JSUP, newReg, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        }

        // Bloc qui renvoie 1
        p.addInstruction(new UALi(trueLabel, UALi.Op.ADD, getNewRegister(), 0, 1));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, endLabel));

        // Bloc qui renvoie 0
        p.addInstruction(new UAL(falseLabel, UAL.Op.XOR, getNewRegister(), 0, 0));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, endLabel));

        p.addInstruction(getLabelInstruction(endLabel));
        return p;
    }

    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);
        p.addInstruction(new UALi(UALi.Op.OR, getNewRegister(), leftAddr, rightAddr));
        return p;
    }

    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Program pCtx = visit(ctx.getChild(1));
        int addr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pCtx);
        p.addInstruction(new UAL(UAL.Op.SUB, getNewRegister(), 0, addr));
        return p;
    }

    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        int value = Integer.parseInt(ctx.getText());
        Program p = new Program();
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), 0, value));
        return p;
    }

    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Program p = new Program();

        Program pVar = visit(ctx.getChild(0));
        int addrTaille = this.nbRegister;

        Program pInd = visit(ctx.getChild(2));
        int addrInd = this.nbRegister;

        p.addInstructions(pVar);
        p.addInstructions(pInd);

        int addrPntr = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrPntr, addrTaille, 0));
        // On saute la case de la taille du tab
        p.addInstruction(new UALi(UALi.Op.ADD, addrPntr, addrPntr, 1));

        // Cte = à 10
        int addrVal10 = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrVal10, 0, 10));

        String debLoopLabel = getNewLabel();
        String finLoopLabel = getNewLabel();

        // Boucle pour mettre l'indice à une val <10
        // Concrétement on cherche le chunk où elle se trouve
        p.addInstruction(getLabelInstruction(debLoopLabel));

        p.addInstruction(new CondJump(CondJump.Op.JINF, addrInd, addrVal10, finLoopLabel));

        // ===== LOOP =====
        p.addInstruction(new UALi(UALi.Op.SUB, addrInd, addrInd, 10));
        // On change de chunk pour accéder au suivant
        int addrNextChunk = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrNextChunk, addrPntr, 10));
        p.addInstruction(new Mem(Mem.Op.LD, addrPntr, addrNextChunk));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, debLoopLabel));
        // ================

        p.addInstruction(getLabelInstruction(finLoopLabel));

        // On y stock l'adresse dans la variable dans la pile
        p.addInstruction(new UAL(UAL.Op.ADD, addrPntr, addrPntr, addrInd));

        // On la renvoie dans un nouveau registre
        p.addInstruction(new Mem(Mem.Op.LD, getNewRegister(), addrPntr));

        return p;
    }

    @Override
    public Program visitBrackets(grammarTCLParser.BracketsContext ctx) {
        return visit(ctx.getChild(1));
    }

    @Override
    public Program visitCall(grammarTCLParser.CallContext ctx) {
        String fctName = ctx.getChild(0).getText();
        Program p = new Program();

        int nbParam = 0;
        for(int i=2; i<ctx.getChildCount()-1; i+=2) {
            // On stock la var de chaque param dans le reg alloué lors de la decl
            Program pParam = visit(ctx.getChild(i));
            int addrParam = this.nbRegister;
            p.addInstructions(pParam);
            p.addInstruction(new UALi(UALi.Op.ADD, varToReg.get(paramToVar.get(fctName+"-"+nbParam)), addrParam, 0));
            nbParam++;
        }

        p.addInstruction(new JumpCall(JumpCall.Op.CALL, fctName));

        // On ajoute la valeur de retour dans un nouveau reg
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), varToReg.get(fctName+"-ret"), 0));

        return p;
    }

    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        String boolVal = ctx.getChild(0).getText();
        int value = 0;
        if(boolVal.equals("true")) {
            value = 1;
        }
        Program p = new Program();
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), 0, value));
        return p;
    }

    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);
        p.addInstruction(new UAL(UAL.Op.AND, getNewRegister(), leftAddr, rightAddr));
        return p;
    }

    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        String varName = ctx.getChild(0).getText();
        Program p = new Program();
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), varToReg.get(varName), 0));
        return p;
    }

    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;

        String ope = ctx.getChild(1).getText();

        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);

        if(ope.equals("*")) {
            p.addInstruction(new UAL(UAL.Op.MUL, getNewRegister(), leftAddr, rightAddr));
        } else if(ope.equals("/")) {
            p.addInstruction(new UAL(UAL.Op.DIV, getNewRegister(), leftAddr, rightAddr));
        } else {
            p.addInstruction(new UAL(UAL.Op.MOD, getNewRegister(), leftAddr, rightAddr));
        }

        return p;
    }

    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);

        String trueLabel = getNewLabel();
        String falseLabel = getNewLabel();
        String endLabel = getNewLabel();

        String ope = ctx.getChild(1).getText();

        if(ope.equals("==")) {
            p.addInstruction(new CondJump(CondJump.Op.JEQU, leftAddr, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        } else {
            // Cas ope == '!='
            p.addInstruction(new CondJump(CondJump.Op.JNEQ, leftAddr, rightAddr, trueLabel));
            p.addInstruction(new JumpCall(JumpCall.Op.JMP, falseLabel));
        }

        // Bloc qui renvoie 1
        p.addInstruction(new UALi(trueLabel, UALi.Op.ADD, getNewRegister(), 0, 1));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, endLabel));

        // Bloc qui renvoie 0
        p.addInstruction(new UAL(falseLabel, UAL.Op.XOR, getNewRegister(), 0, 0));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, endLabel));

        p.addInstruction(getLabelInstruction(endLabel));
        return p;
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        Program p = new Program();

        int nbElt = (ctx.getChildCount()-1)/2;

        int addrTaille = getNewRegister();
        int pntrTab = stackPointer;

        p.addInstruction(new UALi(UALi.Op.ADD, addrTaille, 0, nbElt));

        int addrNewSP = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrNewSP, 0, stackPointer++));
        p.addInstruction(new Mem(Mem.Op.ST, addrTaille, addrNewSP));

        // On ajoute les valeurs du tableau par chunk
        int nbEltLeft = nbElt;
        int nextElt = 1;
        while(nbEltLeft > 0) {
            for(int i=0; i<10; i++) {
                if(nbEltLeft>0) {
                    Program pElt = visit(ctx.getChild(nextElt));
                    int addrElt = this.nbRegister;
                    p.addInstructions(pElt);

                    // On met la valeur dans la pile
                    int addrNewSP2 = getNewRegister();
                    p.addInstruction(new UALi(UALi.Op.ADD, addrNewSP2, 0, stackPointer++));
                    p.addInstruction(new Mem(Mem.Op.ST, addrElt, addrNewSP2));

                    nbEltLeft--;
                    // Par deux car y'a la virgule
                    nextElt += 2;
                }
            }

            // On ajoute au bout du chunk l'adresse du prochain
            int addrPntrVersNextChunk = getNewRegister();
            p.addInstruction(new UALi(UALi.Op.ADD, addrPntrVersNextChunk, 0, stackPointer++));

            int addrTeteNextChunk = getNewRegister();
            if(nbEltLeft > 0) {
                p.addInstruction(new UALi(UALi.Op.ADD, addrTeteNextChunk, addrPntrVersNextChunk, 1));
            } else {
                // Pour signifier que c'est le dernier chunk
                p.addInstruction(new UALi(UALi.Op.ADD, addrTeteNextChunk, 0, -1));
            }

            p.addInstruction(new Mem(Mem.Op.ST, addrTeteNextChunk, addrPntrVersNextChunk));


        }

        // On renvoie l'adresse du premier elt du tableau
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), 0, pntrTab));

        return p;
    }

    @Override
    public Program visitAddition(grammarTCLParser.AdditionContext ctx) {
        Program pLeft = visit(ctx.getChild(0));
        int leftAddr = this.nbRegister;
        Program pRight = visit(ctx.getChild(2));
        int rightAddr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pLeft);
        p.addInstructions(pRight);
        String ope = ctx.getChild(1).getText();
        if(ope.equals("+")) {
            p.addInstruction(new UAL(UAL.Op.ADD, getNewRegister(), leftAddr, rightAddr));
        } else {
            // ope == "-"
            p.addInstruction(new UAL(UAL.Op.SUB, getNewRegister(), leftAddr, rightAddr));
        }
        return p;
    }

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // TODO Auto-generated method stub
        return new Program();
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        return new Program();
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        Program p = new Program();
        int varReg = getNewRegister();
        varToReg.put(ctx.getChild(1).getText(), varReg);

        if(ctx.getChildCount() > 3) {
            // Si on assigne une valeur à la variable
            Program pCtx = visit(ctx.getChild(3));
            int resAddr = this.nbRegister;
            p.addInstructions(pCtx);
            p.addInstruction(new UALi(UALi.Op.ADD, varReg, resAddr, 0));
        }

        return p;
    }

    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        Program p = new Program();
        String varName = ctx.getChild(2).getText();
        int regVar = varToReg.get(varName);

        if(types.get(varName) instanceof ArrayType) {
            // La variable a affiché est un tableau
            p.addInstructions(printArray(regVar));
        } else {
            // La bariable n'est pas un tableau
            p.addInstruction(new Asm.IO(Asm.IO.Op.PRINT, regVar));
        }

        return p;
    }

    // Fonction récursive pour afficher les tableaux
    private Program printArray(int addrArray) {
        Program p = new Program();

        // Lire la taille du chunk
        int addrSize = getNewRegister();
        p.addInstruction(new Mem(Mem.Op.LD, addrSize, addrArray));
        p.addInstruction(new UALi(UALi.Op.ADD, addrArray, addrArray, 1));

        int addrIndex = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrIndex, 0, 0));

        String labelDebLoop = getNewLabel();
        String labelEndLoop = getNewLabel();

        // Afficher [
        int regBracket = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, regBracket, 0, (int)'['));
        p.addInstruction(new Asm.IO(Asm.IO.Op.OUT, regBracket));

        p.addInstruction(getLabelInstruction(labelDebLoop));

        // Fin si index >= taille
        p.addInstruction(new CondJump(CondJump.Op.JSUP, addrIndex, addrSize, labelEndLoop));

        // Calcul adresse de l'élément
        int addrElem = getNewRegister();
        p.addInstruction(new UAL(UAL.Op.ADD, addrElem, addrArray, addrIndex));

        // Lire la valeur
        int addrValue = getNewRegister();
        p.addInstruction(new Mem(Mem.Op.LD, addrValue, addrElem));
        p.addInstruction(new Asm.IO(Asm.IO.Op.PRINT, addrValue));

        // Virgule sauf dernier élément
        int addrNextElt = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrNextElt, addrIndex, 1));
        p.addInstruction(new CondJump(CondJump.Op.JEQU, addrNextElt, addrSize, labelEndLoop));

        int regComma = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, regComma, 0, (int)','));
        p.addInstruction(new Asm.IO(Asm.IO.Op.OUT, regComma));
        int regSpace = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, regSpace, 0, (int)' '));
        p.addInstruction(new Asm.IO(Asm.IO.Op.OUT, regSpace));

        // incrémenter index et boucle
        p.addInstruction(new UALi(UALi.Op.ADD, addrIndex, addrIndex, 1));

        int addrVal10 = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, addrVal10, 0, 10));
        p.addInstruction(new CondJump(CondJump.Op.JNEQ, addrIndex, addrVal10, labelDebLoop));

        int regSep = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, regSep, 0, (int)'|'));
        p.addInstruction(new Asm.IO(Asm.IO.Op.OUT, regSep));

        // Changement de chunk
        int addrLastElem = getNewRegister();
        // Charger l'adresse de la dernière case du chunk actuel
        p.addInstruction(new UALi(UALi.Op.ADD, addrLastElem, addrArray, addrSize)); // addrArray + taille du chunk
        int addrNextChunk = getNewRegister();
        // Charger l'adresse du prochain chunk
        p.addInstruction(new Mem(Mem.Op.LD, addrNextChunk, addrLastElem));
        // Remplacer addrArray par le nouveau chunk
        p.addInstruction(new UAL(UAL.Op.ADD, addrArray, addrNextChunk, 0));
        // Réinitialiser index
        p.addInstruction(new UAL(UAL.Op.XOR, addrIndex, addrIndex, addrIndex));
        // Décrémenter taille restante du chunk
        p.addInstruction(new UALi(UALi.Op.SUB, addrSize, addrSize, 10));
        // Reboucler
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelDebLoop));

        p.addInstruction(getLabelInstruction(labelEndLoop));

        // Afficher ]
        int regBracketEnd = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, regBracketEnd, 0, (int)']'));
        p.addInstruction(new Asm.IO(Asm.IO.Op.OUT, regBracketEnd));
        return p;
    }

    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        Program p = new Program();

        int varReg = varToReg.get(ctx.getChild(0).getText());

        if(ctx.getChildCount() <= 4) {
            // Variable classique (pas un tableau)
            Program pCtx = visit(ctx.getChild(2));
            int addr = this.nbRegister;
            p.addInstructions(pCtx);
            p.addInstruction(new UALi(UALi.Op.ADD, varReg, addr, 0));
        } else {
            // Variable tableau
            // Cte = à 10
            int addrVal10 = getNewRegister();
            p.addInstruction(new UALi(UALi.Op.ADD, addrVal10, 0, 10));

            // On stock l'addr pour pouvoir l'utiliser et la décaler plus tard
            int addrPntr = getNewRegister();
            p.addInstruction(new UAL(UAL.Op.ADD, addrPntr, varReg, 0));

            // Pour chaque dimension du tableau on récupère l'adresse du ss-tabl
            for(int i=2; i<ctx.getChildCount()-4 ; i+=3) {

                Program pInd = visit(ctx.getChild(i));
                int addrInd = this.nbRegister;
                p.addInstructions(pInd);

                int addrTaille = getNewRegister();
                p.addInstruction(new Mem(Mem.Op.LD, addrTaille, addrPntr));

                // On aggrandit la taille si besoin
                String labelSizeOk = getNewLabel();
                p.addInstruction(new CondJump(CondJump.Op.JINF, addrInd, addrTaille, labelSizeOk));

                int addrNewTaille = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrNewTaille, addrInd, 1));

                // chunksExistants = (size + 9) / 10
                int addrChunksExist = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrChunksExist, addrTaille, 9));
                p.addInstruction(new UAL(UAL.Op.DIV, addrChunksExist, addrChunksExist, addrVal10));

                // chunksVoulus = (newSize + 9) / 10
                int addrChunksVoulus = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrChunksVoulus, addrNewTaille, 9));
                p.addInstruction(new UAL(UAL.Op.DIV, addrChunksVoulus, addrChunksVoulus, addrVal10));

                // chunksACreer = chunksVoulus - chunksExistants
                int addrChunksACreer = getNewRegister();
                p.addInstruction(new UAL(UAL.Op.SUB, addrChunksACreer, addrChunksVoulus, addrChunksExist));

                int addrCurrChunk = getNewRegister();
                p.addInstruction(new UAL(UAL.Op.ADD, addrCurrChunk, addrPntr, 0));
                p.addInstruction(new UALi(UALi.Op.ADD, addrCurrChunk, addrCurrChunk, 1));

                // Init bouble for
                int addrILoop = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrILoop, 0, 0));
                String labelDebForLoop = getNewLabel();
                String labelCorpForLoop = getNewLabel();
                String labelFinForLoop = getNewLabel();
                p.addInstruction(getLabelInstruction(labelDebForLoop));

                // Vérif ind
                p.addInstruction(new CondJump(CondJump.Op.JINF, addrILoop, addrChunksExist, labelCorpForLoop));
                p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelFinForLoop));

                // Corp boucle
                p.addInstruction(getLabelInstruction(labelCorpForLoop));
                int tmp = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, tmp, addrCurrChunk, 10));
                p.addInstruction(new Mem(Mem.Op.LD, addrCurrChunk, tmp));

                // Fin de boucle
                p.addInstruction(new UALi(UALi.Op.ADD, addrILoop, addrILoop, 1));
                p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelDebForLoop));
                p.addInstruction(getLabelInstruction(labelFinForLoop));

                String debLoopAlloc = getNewLabel();
                String finLoopAlloc = getNewLabel();

                p.addInstruction(getLabelInstruction(debLoopAlloc));
                p.addInstruction(new CondJump(CondJump.Op.JEQU, addrChunksACreer, 0, finLoopAlloc));

                // Création d'un chunk
                int addrNewChunk = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrNewChunk, 0, stackPointer));

                // Init des 10 cases à 0
                for(int j=0; j<10; j++) {
                    p.addInstruction(new Mem(Mem.Op.ST, 0, stackPointer++));
                }

                // Ptr vers le chunk suivant = 0
                p.addInstruction(new Mem(Mem.Op.ST, 0, stackPointer++));

                // Chaînage du nouveau chunk a l'ancien
                int addrPtrNext = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrPtrNext, addrCurrChunk, 10));
                p.addInstruction(new Mem(Mem.Op.ST, addrNewChunk, addrPtrNext));

                p.addInstruction(new UAL(UAL.Op.ADD, addrCurrChunk, addrNewChunk, 0));
                p.addInstruction(new UALi(UALi.Op.SUB, addrChunksACreer, addrChunksACreer, 1));
                p.addInstruction(new JumpCall(JumpCall.Op.JMP, debLoopAlloc));

                p.addInstruction(getLabelInstruction(finLoopAlloc));

                // Mise à jour de la taille du tableau dans la prem addr
                p.addInstruction(new Mem(Mem.Op.ST, addrNewTaille, addrPntr));
                // Aggrandissement du tableau fini

                p.addInstruction(getLabelInstruction(labelSizeOk));

                // Décalage pour sauter la taille du tab
                p.addInstruction(new UALi(UALi.Op.ADD, addrPntr, addrPntr, 1));

                String debLoopLabel = getNewLabel();
                String finLoopLabel = getNewLabel();

                // Boucle pour mettre l'indice à une val <10
                // Concrétement on cherche le chunk où elle se trouve
                p.addInstruction(getLabelInstruction(debLoopLabel));
                p.addInstruction(new CondJump(CondJump.Op.JINF, addrInd, addrVal10, finLoopLabel));
                p.addInstruction(new UALi(UALi.Op.SUB, addrInd, addrInd, 10));

                int addrNextChunk = getNewRegister();
                p.addInstruction(new UALi(UALi.Op.ADD, addrNextChunk, addrPntr, 10));

                // On change de chunk pour accéder au suivant
                p.addInstruction(new Mem(Mem.Op.LD, addrPntr, addrNextChunk));
                p.addInstruction(new JumpCall(JumpCall.Op.JMP, debLoopLabel));

                p.addInstruction(getLabelInstruction(finLoopLabel));

                p.addInstruction(new UAL(UAL.Op.ADD, addrPntr, addrPntr, addrInd));


            }

            Program pVal = visit(ctx.getChild(ctx.getChildCount()-2));
            int addrVal = this.nbRegister;
            p.addInstructions(pVal);

            // On la renvoie dans un nouveau registre
            p.addInstruction(new Mem(Mem.Op.ST, addrVal, addrPntr));
        }

        return p;
    }

    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        Program p = new Program();

        for(int i=1; i<ctx.getChildCount()-1; i++) {
            Program pInstr = visit(ctx.getChild(i));
            p.addInstructions(pInstr);
        }

        return p;
    }

    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        Program pCond = visit(ctx.getChild(2));
        int addr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pCond);

        String labelElse = getNewLabel();
        String labelFinInstr = getNewLabel();

        int valUn = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, valUn, 0, 1));
        // Si condition pas validee on dodge les instructions du if
        p.addInstruction(new CondJump(CondJump.Op.JINF, addr, valUn, labelElse));

        // Corp du if
        Program pCorp = visit(ctx.getChild(4));
        p.addInstructions(pCorp);
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelFinInstr));

        p.addInstruction(getLabelInstruction(labelElse));

        if(ctx.getChildCount() > 5) {
            Program pElse = visit(ctx.getChild(6));
            p.addInstructions(pElse);
        }

        // Instr utile juste pour le label
        p.addInstruction(getLabelInstruction(labelFinInstr));

        return p;
    }

    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        Program pCond = visit(ctx.getChild(2));
        int addrCond = this.nbRegister;
        Program pCorp = visit(ctx.getChild(4));

        String labelDebWhile = getNewLabel();
        String labelFin = getNewLabel();

        Program p = new Program();
        // Pour pouvoir y retourner en fin de boucle
        p.addInstruction(getLabelInstruction(labelDebWhile));
        p.addInstructions(pCond);

        // Vérif de la condition
        int valUn = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, valUn, 0, 1));
        // Si condition pas validee on dodge les instructions du if
        p.addInstruction(new CondJump(CondJump.Op.JINF, addrCond, valUn, labelFin));

        p.addInstructions(pCorp);
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelDebWhile));
        p.addInstruction(getLabelInstruction(labelFin));

        return p;
    }

    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        Program pInit = visit(ctx.getChild(2));
        Program pCond = visit(ctx.getChild(4));
        int addrCond = this.nbRegister;
        Program pIncr = visit(ctx.getChild(6));
        Program pCorp = visit(ctx.getChild(8));

        String labelDebFor = getNewLabel();
        String labelFin = getNewLabel();

        Program p = new Program();
        p.addInstructions(pInit);
        // Pour pouvoir y retourner en fin de boucle
        p.addInstruction(getLabelInstruction(labelDebFor));
        p.addInstructions(pCond);

        // Vérif de la condition
        int valUn = getNewRegister();
        p.addInstruction(new UALi(UALi.Op.ADD, valUn, 0, 1));
        // Si condition pas validee on dodge les instructions du if
        p.addInstruction(new CondJump(CondJump.Op.JINF, addrCond, valUn, labelFin));

        p.addInstructions(pCorp);
        p.addInstructions(pIncr);
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, labelDebFor));

        p.addInstruction(getLabelInstruction(labelFin));

        return p;
    }

    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {
        Program pCtx = visit(ctx.getChild(1));
        int addr = this.nbRegister;
        Program p = new Program();
        p.addInstructions(pCtx);
        // On transmet la derniere valeur calculée
        p.addInstruction(new UALi(UALi.Op.ADD, getNewRegister(), addr, 0));
        p.addInstruction(new Ret());
        return p;
    }

    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        Program p = new Program();

        for(int i=1; i<ctx.getChildCount()-4; i++) {
            Program pCtx = visit(ctx.getChild(i));
            p.addInstructions(pCtx);
        }

        Program pExpr = visit(ctx.getChild(ctx.getChildCount()-3));
        p.addInstructions(pExpr);

        return p;
    }

    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        Program p = new Program();

        // Ajout du label qui correspond au nom de la ftc
        p.addInstruction(getLabelInstruction(ctx.getChild(1).getText()));

        // Ajout des params comme var
        int nbParam = 0;
        for(int i=4; i<ctx.getChildCount()-2; i+=3) {
            // Encodage de la forme nom_fct+"-"+ind_param
            // Pour ne pas écraser de potentiel variable hors de la fct
            // Car un nom de var ne peut pas contenir '-'
            varToReg.put(ctx.getChild(i).getText(), getNewRegister());
            paramToVar.put(ctx.getChild(1).getText()+"-"+nbParam, ctx.getChild(i).getText());
            nbParam++;
        }

        Program pCorpFct = visit(ctx.getChild(ctx.getChildCount()-1));
        p.addInstructions(pCorpFct);
        // Stock le registre où est stockée la val de retour de la fct
        int addrRet = this.nbRegister;
        varToReg.put(ctx.getChild(1).getText()+"-ret", addrRet);

        p.addInstruction(new Ret());

        return p;
    }

    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        Program p = new Program();

        String mainLabel = getNewLabel();

        p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
        p.addInstruction(new JumpCall(JumpCall.Op.JMP, mainLabel));

        for(int i=0; i<ctx.getChildCount()-3; i++) {
            Program pFct = visit(ctx.getChild(i));
            p.addInstructions(pFct);
        }

        p.addInstruction(getLabelInstruction(mainLabel));
        Program pCorp = visit(ctx.getChild(ctx.getChildCount()-2));
        p.addInstructions(pCorp);

        p.addInstruction(new Stop());

        return p;
    }


}