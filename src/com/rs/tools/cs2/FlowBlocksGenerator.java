package com.rs.tools.cs2;

import static com.rs.tools.cs2.ast.LocalVariable.CHILD;
import static com.rs.tools.cs2.ast.LocalVariable._CHILD;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.ast.ArrayLoadNode;
import com.rs.tools.cs2.ast.ArrayStoreNode;
import com.rs.tools.cs2.ast.BooleanConditionalExpressionNode;
import com.rs.tools.cs2.ast.BuildStringNode;
import com.rs.tools.cs2.ast.CallExpressionNode;
import com.rs.tools.cs2.ast.CallbackExpressionNode;
import com.rs.tools.cs2.ast.CastNode;
import com.rs.tools.cs2.ast.CommentNode;
import com.rs.tools.cs2.ast.ConditionalExpressionNode;
import com.rs.tools.cs2.ast.ConditionalFlowBlockJump;
import com.rs.tools.cs2.ast.ExpressionList;
import com.rs.tools.cs2.ast.ExpressionNode;
import com.rs.tools.cs2.ast.FlowBlock;
import com.rs.tools.cs2.ast.FunctionNode;
import com.rs.tools.cs2.ast.GlobalVariable;
import com.rs.tools.cs2.ast.IntExpressionNode;
import com.rs.tools.cs2.ast.LocalVariable;
import com.rs.tools.cs2.ast.LongExpressionNode;
import com.rs.tools.cs2.ast.MathExpressionNode;
import com.rs.tools.cs2.ast.NewArrayNode;
import com.rs.tools.cs2.ast.Operator;
import com.rs.tools.cs2.ast.PlaceholderValueNode;
import com.rs.tools.cs2.ast.PopableNode;
import com.rs.tools.cs2.ast.ReturnNode;
import com.rs.tools.cs2.ast.StringExpressionNode;
import com.rs.tools.cs2.ast.SwitchFlowBlockJump;
import com.rs.tools.cs2.ast.UnconditionalFlowBlockJump;
import com.rs.tools.cs2.ast.Underscore;
import com.rs.tools.cs2.ast.Variable;
import com.rs.tools.cs2.ast.VariableAssignationNode;
import com.rs.tools.cs2.ast.VariableLoadNode;
import com.rs.tools.cs2.instructions.AbstractInstruction;
import com.rs.tools.cs2.instructions.BooleanInstruction;
import com.rs.tools.cs2.instructions.IntInstruction;
import com.rs.tools.cs2.instructions.JumpInstruction;
import com.rs.tools.cs2.instructions.Label;
import com.rs.tools.cs2.instructions.LongInstruction;
import com.rs.tools.cs2.instructions.Opcodes;
import com.rs.tools.cs2.instructions.StringInstruction;
import com.rs.tools.cs2.instructions.SwitchInstruction;
import com.rs.tools.cs2.util.CS2TypeUtil;
import com.rs.tools.cs2.util.FunctionInfo;
import com.rs.tools.cs2.util.IOUtils;
import com.rs.tools.cs2.util.OpcodeUtils;

public class FlowBlocksGenerator {

    private CS2Decompiler decompiler;
    private CS2 cs2;
    private FunctionNode function;
    private List<FlowBlock> blocks = new ArrayList<>();
    private Queue<FlowBlock> unprocessedBlocks = new LinkedList<>();
    private Map<Integer, FlowBlock> byAddress = new HashMap<>();
    private int blockCount = 1;

    public FlowBlocksGenerator(CS2Decompiler decompiler) {
        this.decompiler = decompiler;
        this.cs2 = decompiler.getCs2();
        this.function = decompiler.getFunction();
        //Block starting at address 0, with an empty stack
        unprocessedBlocks.add(new FlowBlock(0, 0, new CS2Stack()));
    }


    public void generate() throws DecompilerException {
        while (unprocessedBlocks.size() > 0) {
//            System.out.println("Remaining "+unprocessedBlocks.size());
            FlowBlock next = unprocessedBlocks.poll();
            processFlowBlock(next);
            blocks.add(next);
        }
    }

    private void processFlowBlock(FlowBlock block) {
        int ptr = block.getStartAddress();
        CS2Stack stack = block.getStack().copy();

        try {
            for (; ; ptr++) {
                if (ptr >= cs2.getInstructions().length)
                    throw new DecompilerException("Error:Code out bounds.");
                AbstractInstruction instruction = cs2.getInstructions()[ptr];
                CS2Instruction opcode = instruction.getOpcode();
                if (instruction instanceof Label) {
                    // new flow block
                    FlowBlock target = this.generateFlowBlock((Label) instruction, stack.copy());
                    target.parents.add(block);
                    block.children.add(target);
                    block.write(new UnconditionalFlowBlockJump(target)); //This is natural flow, but insert a GOTO, this makes the control flow patterns which we are are looking for much easier to detect later
                    break;
                } else if (instruction instanceof JumpInstruction) {
                    JumpInstruction jmp = (JumpInstruction) instruction;
                    if (OpcodeUtils.getTwoConditionsJumpStackType(opcode) != -1) {
                        ExpressionNode uncasted1 = stack.pop();
                        ExpressionNode uncasted2 = stack.pop();
                        ConditionalExpressionNode conditional = new ConditionalExpressionNode(uncasted2, uncasted1, OpcodeUtils.getTwoConditionsJumpConditional(opcode));
                        FlowBlock target = generateFlowBlock(jmp.getTarget(), stack.copy());
                        target.parents.add(block);
                        block.children.add(target);
                        block.write(new ConditionalFlowBlockJump(conditional, target));
                    } else if (opcode == CS2Instruction.BRANCH_EQ1 || opcode == CS2Instruction.BRANCH_EQ0) { //EQ1/0
                        ExpressionNode left = stack.pop();
                        FlowBlock target = generateFlowBlock(jmp.getTarget(), stack.copy());
                        ExpressionNode conditional;
                        if (left.getType() == CS2Type.BOOLEAN) {
                            conditional = new BooleanConditionalExpressionNode(opcode == CS2Instruction.BRANCH_EQ0, left);
                        } else {
                            conditional = new ConditionalExpressionNode(left, new IntExpressionNode(opcode == CS2Instruction.BRANCH_EQ0 ? 0 : 1), Operator.EQ); //Operator ==
                        }
                        target.parents.add(block);
                        block.children.add(target);
                        block.write(new ConditionalFlowBlockJump(conditional, target));
                    } else if (opcode == CS2Instruction.GOTO) {
                        FlowBlock target = generateFlowBlock(jmp.getTarget(), stack.copy());
                        target.parents.add(block);
                        block.children.add(target);
                        block.write(new UnconditionalFlowBlockJump(target));
                        break;
                    } else {
                        throw new DecompilerException("Unknown jump instruction");
                    }
//                    break;
                } else if (instruction instanceof IntInstruction) {
                    IntInstruction intInstr = (IntInstruction) instruction;
                    int val = intInstr.getConstant();

                    if (Opcodes.isAssign(opcode)) {
                        LinkedList<Variable> vars = new LinkedList<>();
                        LinkedList<ExpressionNode> assignments = new LinkedList<>();
                        LinkedList<CS2Type> stackTypes = new LinkedList<>();
                        ExpressionNode expr_ = null;
                        do {
                            Variable var;
                            if (stackTypes.isEmpty()) {
                                expr_ = stack.pop();
                                assignments.addFirst(expr_);
                                stackTypes.addAll(expr_.getType().composite);
                            }
                            CS2Type currentType = stackTypes.pollLast();

                            if (opcode == CS2Instruction.POP_INT || opcode == CS2Instruction.POP_STRING || opcode == CS2Instruction.POP_LONG) {
                                var = opcode == CS2Instruction.POP_INT ? Underscore.UNDERSCORE_I : opcode == CS2Instruction.POP_STRING ? Underscore.UNDERSCORE_S : Underscore.UNDERSCORE_L;
                            } else if (opcode == CS2Instruction.STORE_VARP) {
                                var = GlobalVariable.VARP(intInstr.getConstant(), currentType);
                            } else if (opcode == CS2Instruction.STORE_VARPBIT) {
                                var = GlobalVariable.VARPBIT(intInstr.getConstant(), currentType);
                            } else if (opcode == CS2Instruction.STORE_VARC) {
                                var = GlobalVariable.VARC(intInstr.getConstant(), currentType);
                            } else if (opcode == CS2Instruction.STORE_VARC_STRING) {
                                var = GlobalVariable.VARC_STRING(intInstr.getConstant());
                            } else if (opcode == CS2Instruction.STORE_INT || opcode == CS2Instruction.STORE_STRING || opcode == CS2Instruction.STORE_LONG) {
                                int stackType = opcode == CS2Instruction.STORE_INT ? 0 : (opcode == CS2Instruction.STORE_LONG ? 2 : 1);
                                var = function.getMainScope().getLocalVariable(LocalVariable.makeIdentifier(intInstr.getConstant(), stackType));
                                //dont do boolean?
                                if (var.getType() == CS2Type.INT && currentType != CS2Type.INT && currentType != CS2Type.UNKNOWN) {
                                    ((LocalVariable) var).changeType(currentType);
                                }
                                if (expr_.getType() == CS2Type.INT && var.getType() != CS2Type.INT) {
                                    if (expr_ instanceof VariableLoadNode && ((VariableLoadNode) expr_).getVariable() instanceof LocalVariable) {
                                        ((LocalVariable) ((VariableLoadNode) expr_).getVariable()).changeType(var.getType());
                                    } else if (expr_ instanceof CallExpressionNode) {
                                        if (((CallExpressionNode) expr_).info.isScript)
                                            ((CallExpressionNode) expr_).info.returnType = var.getType();
                                    }
                                }
                            } else {
                                throw new DecompilerException("Someone messed up the list");
                            }
                            //When result is ignored pop instructions are in reverse on complex type..., so ignore underscore type check
                            if (!(var instanceof Underscore || var.getType().isCompatible(currentType))) {
                                throw new DecompilerException("Incompatible");
                            }
                            vars.addFirst(var);


                            if (Opcodes.isAssign(cs2.getInstructions()[ptr + 1].getOpcode())) {
                                intInstr = (IntInstruction) cs2.getInstructions()[++ptr];
                                opcode = intInstr.getOpcode();
                            } else {
                                break;
                            }
                        } while (true);
                        //
                        assert stack.getSize() == 0 : "Whole stack should be popped after assign";
                        assert stackTypes.size() == 0 : "Only half result of expression used";
                        //TODO: if all vars are underscores, we could make normal expressions out of this and remove the underscore part (eg _, _ = get2ValuesAndSomeSideEffects()). Doesn't appear to be used much though, so i think its fine for clarity
                        block.write(new PopableNode(new VariableAssignationNode(vars, assignments.size() == 1 ? assignments.get(0) : new ExpressionList(assignments))));
                    } else if (opcode == CS2Instruction.PUSH_INT) { // push int.
                        //TODO: Technically only in delegate methods
                        if (val == -2147483647) {
                            stack.push(new PlaceholderValueNode("MOUSE_X", val, CS2Type.INT));
                        } else if (val == -2147483646) {
                            stack.push(new PlaceholderValueNode("MOUSE_Y", val, CS2Type.INT));//Either mouse y pos, or scroll ticks
                        } else if (val == -2147483645) {
                            stack.push(new PlaceholderValueNode("CTX_WIDGET", val, CS2Type.ICOMPONENT));
                        } else if (val == -2147483644) {
                            stack.push(new PlaceholderValueNode("CTX_MENU_OPTION", val, CS2Type.INT));
                        } else if (val == -2147483643) {
                            stack.push(new PlaceholderValueNode("CTX_WIDGET_CHILD", val, CS2Type.INT));
                        } else if (val == -2147483642) {
                            stack.push(new PlaceholderValueNode("DRAG_WIDGET", val, CS2Type.ICOMPONENT));
                        } else if (val == -2147483641) {
                            stack.push(new PlaceholderValueNode("DRAG_WIDGET_CHILD", val, CS2Type.INT));
                        } else if (val == -2147483640) {
                            stack.push(new PlaceholderValueNode("KEY_TYPED", val, CS2Type.INT));
                        } else if (val == -2147483639) {
                            stack.push(new PlaceholderValueNode("KEY_PRESSED", val, CS2Type.CHAR));
                        } else {
                            stack.push(new IntExpressionNode(val));
                        }
                    } else if (opcode == CS2Instruction.LOAD_VARP) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("VARP", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.RETURN) {
                        if (stack.getSize() <= 0) {
                            block.write(new ReturnNode(null));
                            assert CS2Type.VOID.isCompatible(this.function.getReturnType());
                            this.function.setReturnType(CS2Type.VOID);
                        } else if (stack.getSize() == 1) {
                            if (stack.peek().getType() == CS2Type.UNKNOWN) throw new DecompilerException("Unknown return type "+stack.peek());
                            if (this.function.getReturnType() != CS2Type.UNKNOWN && stack.peek().getType() == CS2Type.INT) {
                                stack.push(CS2TypeUtil.cast(stack.pop(), this.function.getReturnType()));
                            }
                            block.write(new ReturnNode(stack.peek()));

                            if (this.function.getReturnType() == CS2Type.UNKNOWN || this.function.getReturnType() == CS2Type.INT) {
                                assert stack.peek().getType().isCompatible(this.function.getReturnType());
                                this.function.setReturnType(stack.pop().getType());
                            } else {
                                ExpressionNode ret = stack.pop();
                                assert this.function.getReturnType().isCompatible(ret.getType());
                            }
                        } else {
                            LinkedList<ExpressionNode> expressions = new LinkedList<>();
                            while (stack.getSize() > 0) {
                                ExpressionNode expr = stack.pop();
                                if (expr.getType() == CS2Type.UNKNOWN) throw new DecompilerException("Unknown return type");
//                                assert !expr.getType().isStructure() : "no support yet for returning structs together with other values";
                                expressions.addFirst(expr);
                            }
                            block.write(new ReturnNode(new ExpressionList(expressions)));
                            this.function.setReturnType(CS2TypeUtil.typeFor(expressions));
                        }
//                        if ((ptr + 1) >= cs2.getInstructions().length) //uncomment this to decompile some dead code
                        break;
                    } else if (opcode == CS2Instruction.LOAD_VARPBIT) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("VARPBIT", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.LOAD_INT || opcode == CS2Instruction.LOAD_STRING || opcode == CS2Instruction.LOAD_LONG) {
                        int stackType = opcode == CS2Instruction.LOAD_INT ? 0 : (opcode == CS2Instruction.LOAD_LONG ? 2 : 1);
                        LocalVariable var = function.getMainScope().getLocalVariable(LocalVariable.makeIdentifier(intInstr.getConstant(), stackType));
                        stack.push(new VariableLoadNode(var));
                    } else if (opcode == CS2Instruction.MERGE_STRINGS) {
                        int amount = intInstr.getConstant();
                        LinkedList<ExpressionNode> exprs = new LinkedList<>();
                        for (int i = amount - 1; i >= 0; i--)
                            exprs.addFirst(CS2TypeUtil.cast(stack.pop(), CS2Type.STRING));
                        stack.push(new BuildStringNode(exprs));
                    } else if (opcode == CS2Instruction.CALL_CS2) {
                        FunctionInfo info = decompiler.getScriptsDatabase().getInfo(CS2Instruction.getByOpcode(intInstr.getConstant()));
                        if (info == null)
                            throw new DecompilerException("No documentation for:" + instruction);
                        analyzeActualArgOrder(info, stack.copy());
                        int ret = this.analyzeCall(info, block, stack, ptr, true, false, false, intInstr.getConstant(), false);
                        if (ret != -1)
                            ptr = ret;
                    } else if (opcode == CS2Instruction.LOAD_VARC) {
                        stack.push(new VariableLoadNode(GlobalVariable.VARC(intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.ARRAY_NEW) {
                        int arrayID = intInstr.getConstant() >> 16;
                        char type = (char) (intInstr.getConstant() & 0xFFFF);
                        CS2Type array = CS2Type.forJagexDesc(type);//.getArrayType();
                        ExpressionNode length = CS2TypeUtil.cast(stack.pop(), CS2Type.INT);
                        block.write(new PopableNode(new NewArrayNode(arrayID, length, array)));
                    } else if (opcode == CS2Instruction.ARRAY_LOAD) {
                        stack.push(new ArrayLoadNode(intInstr.getConstant(), CS2Type.INT/*.getArrayType()*/, CS2TypeUtil.cast(stack.pop(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.ARRAY_STORE) {
                        ExpressionNode value = stack.pop();
                        ExpressionNode index = CS2TypeUtil.cast(stack.pop(), CS2Type.INT);
                        block.write(new PopableNode(new ArrayStoreNode(intInstr.getConstant(), index, value)));
                    } else if (opcode == CS2Instruction.LOAD_VARC_STRING) {
                        stack.push(new VariableLoadNode(GlobalVariable.VARC_STRING(intInstr.getConstant())));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_VAR) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLAN", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_VARBIT) {
                        //TODO: THIS COULD BE 2 INTS FOR SOME OPERANDS!!!!
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLANBIT", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_VAR_LONG) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLAN_LONG", intInstr.getConstant(), CS2Type.LONG)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_VAR_STRING) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLAN_STRING", intInstr.getConstant(), CS2Type.STRING)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_SETTING_VAR) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLANDEF112", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_SETTING_VARBIT) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLANDEF113", intInstr.getConstant(), CS2Type.INT)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_SETTING_VAR_LONG) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLANDEF_LONG114", intInstr.getConstant(), CS2Type.LONG)));
                    } else if (opcode == CS2Instruction.LOAD_CLAN_SETTING_VAR_STRING) {
                        stack.push(new VariableLoadNode(GlobalVariable.find("CLANDEF_STRING115", intInstr.getConstant(), CS2Type.STRING)));
                    } else {
                        throw new DecompilerException("Unknown opcode:" + opcode);
                    }
                } else if (instruction instanceof StringInstruction) {
                    stack.push(new StringExpressionNode(((StringInstruction) instruction).getConstant()));
                } else if (instruction instanceof LongInstruction) {
                    stack.push(new LongExpressionNode(((LongInstruction) instruction).getConstant()));
                } else if (instruction instanceof BooleanInstruction) {
                    if (opcode == CS2Instruction.ADD || opcode == CS2Instruction.SUBTRACT || opcode == CS2Instruction.MULTIPLY || opcode == CS2Instruction.DIVIDE) {
                        ExpressionNode r = stack.pop();
                        stack.push(new MathExpressionNode(stack.pop(), r, opcode == CS2Instruction.ADD ? Operator.PLUS : opcode == CS2Instruction.SUBTRACT ? Operator.MINUS : opcode == CS2Instruction.MULTIPLY ? Operator.MUL : Operator.DIV).simplify());
                    } else if (opcode == CS2Instruction.MODULO) { //mod
                        ExpressionNode r = stack.pop();
                        stack.push(new MathExpressionNode(stack.pop(), r, Operator.REM));
//                    } else if (opcode == 4018) { //a * c / b
//                        ExpressionNode c = stack.pop();
//                        ExpressionNode b = stack.pop();
//                        ExpressionNode a = stack.pop();
//                        stack.push(new MathExpressionNode(new MathExpressionNode(a, c, Operator.MUL), b, Operator.DIV));
                    } else {
                        boolean dynamicArgTypes = false;
                        boolean dynamicResultType = false;
                        FunctionInfo info = decompiler.getOpcodesDatabase().getInfo(instruction.getOpcode());
//                        if ((opcode >= 1400 && opcode < 1499) || (opcode >= 2400 && opcode < 2499)) {
//                            analyzeDelegate(stack, opcode);
//                            dynamicArgTypes = true; //TODO commented out
//                        }
                        if (info.getReturnType() == CS2Type.UNKNOWN) {
//                            int[] specialOpcodes = CS2.OSRS ? new int[]{1613, 3408, 4019, 4500, 6804, 4300, 4400, 6513, 6514, 6515, 6516} : new int[]{1613, 3408, 4019, 4208, 4500, 6804, 4300, 4400, /*6509, 6701*/};
//                            for (int i = 0; i < specialOpcodes.length; i++)
//                                if (opcode == specialOpcodes[i]) {
                            info = analyzeSpecialCall(block, stack.copy(), opcode, info);
                            dynamicArgTypes = true;
                            dynamicResultType = true;
//                                    break;
//                                }
                        }
                        if (info == null)
                            throw new DecompilerException("No documentation for:" + instruction);

//                        dynamicArgTypes |= opcode == 3400 || opcode == 3409 || opcode == 3412 || opcode == 3414; //datamap contains/lookup value checks
//                        if (!dynamicArgTypes) {
//                            analyzeActualArgOrder(info, stack.copy());
//                        }
//                        int ret = this.analyzeCall(info, block, stack, ptr, !dynamicArgTypes, dynamicResultType, true, opcode, ((BooleanInstruction) instruction).getConstant());
//                        if (ret != -1) //TODO commented out
//                            ptr = ret;
                    }
                } else if (instruction instanceof SwitchInstruction) {
                    SwitchInstruction sw = (SwitchInstruction) instruction;
                    ExpressionNode value = stack.pop();
                    List<Integer> cases = sw.cases;
                    List<FlowBlock> targets = new ArrayList<>(cases.size());
                    for (int i = 0; i < cases.size(); i++) {
                        FlowBlock b = generateFlowBlock(sw.targets.get(i), stack.copy());
                        b.parents.add(block);
                        targets.add(b);
                        block.children.add(b);
                    }
                    block.write(new SwitchFlowBlockJump(value, cases, targets));

                } else
                    throw new DecompilerException("Error:Unknown instruction type:" + instruction.getClass().getName());
            }
        } catch (Throwable ex) {
            block.write(new CommentNode(IOUtils.getStackTrace(ex)));
            System.err.println("error in scriptid: " + this.cs2.scriptID);
            //TODO: Add quickfail option, no need to stop, we can just not decompile this FLOW, may just be an if branch of a script
            if (Main.GENERATE_SCRIPTS_DB) { //Probably won't be able to determine return type (maybe in another flow though), so just wait untill we have more info
                throw ex;
            } else {
                ex.printStackTrace();
            }
        }
    }

    /**
     * From script definition we only know the number of arguments, not the actual order of types, eg (int, int, str) vs (int, str, int)
     * If we look at the types on the stack, we can determine the ACTUAL order (because their compiler enforces the actual order)
     */
    public static boolean SKIP_ARG_ORDER_ANALYSIS = false;

    private void analyzeActualArgOrder(FunctionInfo info, CS2Stack stack) {
        if (info == null || SKIP_ARG_ORDER_ANALYSIS) return; //Callback is not decompiled yet
        boolean mod = false;
        next:
        for (int i = info.getArgumentTypes().length - 1; i >= 0; i--) {
            ExpressionNode top = stack.pop();
            if (top.getType().isStructure()) {
                //unroll multiple return value call into dummy expression nodes of all its types
                for (CS2Type c : top.getType().composite) {
                    stack.push(new ExpressionNode() {

                        @Override
                        public CS2Type getType() {
                            return c;
                        }

                        @Override
                        public ExpressionNode copy() {
                            return null;
                        }

                        @Override
                        public void print(CodePrinter printer) {
                        }
                    });

                }
                continue;
            }

            if (!info.getArgumentTypes()[i].isCompatible(top.getType())) {
                for (int j = i - 1; j >= 0; j--) {
                    if (info.getArgumentTypes()[j].isCompatible(top.getType())) {
                        mod = true;
                        //shift all args
                        CS2Type ftype = info.getArgumentTypes()[j];
                        String fname = info.getArgumentNames()[j];
                        System.arraycopy(info.getArgumentNames(), j + 1, info.getArgumentNames(), j, i - j);
                        System.arraycopy(info.getArgumentTypes(), j + 1, info.getArgumentTypes(), j, i - j);
                        info.getArgumentTypes()[i] = ftype;
                        info.getArgumentNames()[i] = fname;
                        continue next;
                    }
                }
                throw new DecompilerException("bad stack");
            }
        }
        if (mod) {
            System.out.println("Shuffled args of " + info.toString());
            try {
                FileWriter fw = new FileWriter("argorder.txt", true);
                fw.write(info.toString() + "\r\n");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Some opcodes modify the stack in a way that can only be determined by looking at the arguments.
     * This method inspects the stack and returns a FunctionInfo with return/argument types that represent what this call will actually do
     */
    private FunctionInfo analyzeSpecialCall(FlowBlock block, CS2Stack stack, CS2Instruction opcode, FunctionInfo fi) {
//        if (opcode == 3408) {
//            //DataMap<K, V> lookup, this is a 'generic' method where the returned type depends on the map definition.
//            //2 chars are also supplied to this call representing the expected Key and Value types
//            //If these do not match the DataMap definition a RuntimeException will be thrown on execution
//            ExpressionNode[] args = new ExpressionNode[4];
//            for (int i = 3; i >= 0; i--)
//                args[i] = stack.pop();
//            if (!(args[1] instanceof IntExpressionNode) || !(args[0] instanceof IntExpressionNode))
//                throw new DecompilerException("Dynamic type");
//            return new FunctionInfo(fi.getName(), opcode, new CS2Type[]{CS2Type.CHAR, CS2Type.CHAR, CS2Type.ENUM, CS2Type.forJagexDesc((char) ((IntExpressionNode) args[0]).getData())}, CS2Type.forJagexDesc((char) ((IntExpressionNode) args[1]).getData()), fi.getArgumentNames(), false);
//        } else if (opcode == 4019) { //math function
//            ExpressionNode[] args = new ExpressionNode[2];
//            for (int i = 1; i >= 0; i--)
//                args[i] = stack.pop();
//            if (!(args[0] instanceof IntExpressionNode) || !(args[1] instanceof IntExpressionNode))
//                throw new DecompilerException("Dynamic type");
//            if (((IntExpressionNode) args[0]).getData() > 700 || ((IntExpressionNode) args[1]).getData() > 700) //shouldn't happen actually i guess
//                throw new DecompilerException("random err: max value 700");
////                return new FunctionInfo(fi.getName(), opcode, new CS2Type[]{CS2Type.INT, CS2Type.INT}, CS2Type.forDesc("ii"), new String[]{"arg0", "arg1"}, false);
//            return new FunctionInfo(fi.getName(), opcode, new CS2Type[]{CS2Type.INT, CS2Type.INT}, CS2Type.INT, new String[]{"arg0", "arg1"}, false);
//        } else if (opcode == 1613) {
//            ExpressionNode arg = stack.pop();
//            if (!(arg instanceof IntExpressionNode))
//                throw new DecompilerException("Dynamic type");
//            return new FunctionInfo(fi.getName(), opcode, fi.getArgumentTypes(), CS2Type.attrTypes.get(((IntExpressionNode) arg).getData()), fi.getArgumentNames(), false);
//        } else if (opcode == 4208 || opcode == 4300 || opcode == 4400 || opcode == 4500 || opcode == 6804) {
//            ExpressionNode arg = stack.pop();
//            stack.pop(); //
//            if (!(arg instanceof IntExpressionNode))
//                throw new DecompilerException("Dynamic type");
//
//            return new FunctionInfo(fi.getName(), opcode, fi.getArgumentTypes(), CS2Type.attrTypes.get(((IntExpressionNode) arg).getData()), fi.getArgumentNames(), false);
//        }
//        //Just gonna assume normal flow... calling these opcodes might do unknown things to the stack!!
////        else if (opcode == 6509)
////            throw new DecompilerException("opcode 6509 not decompileable");
////        else if (opcode == 6701)
////            throw new DecompilerException("opcode 6701 not decompileable");
//        else if (opcode == 26513 || opcode == 26514 || opcode == 26515 || opcode == 26516) {
//            //return unknown type, there is only 1 stack anyway.
//            return new FunctionInfo(fi.getName(), opcode, new CS2Type[]{CS2Type.INT, CS2Type.INT}, CS2Type.UNKNOWN, new String[]{"arg0", "arg1"}, false);
//        }

        throw new DecompilerException("Unhandled special opcode:" + opcode);

    }

    /**
     * Analyze a callback passed to a hook.
     * Removes all items on the stack describing the callback, and replace it with a AST node representing the 'callback object'
     */
    private void analyzeDelegate(CS2Stack stack, CS2Instruction opcode) {
        ExpressionNode invokeOn = null;
        ExpressionList trigger = null;

//        if (opcode >= 2000) {
//            invokeOn = stack.pop();
//        }

        ExpressionNode stringExpr = stack.pop();
        if (!(stringExpr instanceof StringExpressionNode))
            throw new DecompilerException("Dynamic delegate - impossible to decompile.");
        String descriptor = ((StringExpressionNode) stringExpr).getData();
        if (descriptor.length() > 0 && descriptor.charAt(descriptor.length() - 1) == 'Y') {
            ExpressionNode length = stack.pop();
            if (!(length instanceof IntExpressionNode))
                throw new DecompilerException("Dynamic delegate - impossible to decompile.");
            int len = ((IntExpressionNode) length).getData();
            LinkedList<ExpressionNode> triggers = new LinkedList<>();
            while (len-- > 0) {
                ExpressionNode expr = stack.pop();
                triggers.addFirst(expr);
                assert expr.getType().isCompatible(CS2Type.INT); //Should all be ints
            }
            descriptor = descriptor.substring(0, descriptor.length() - 1);
            trigger = new ExpressionList(triggers);
        }
        CS2Stack argsCopy = stack.copy(); //Now the actual arguments of the callback are on top of the stack, create a copy for arg order analysis
        ExpressionNode[] callBackArgs = new ExpressionNode[descriptor.length()];
        for (int argument = descriptor.length() - 1; argument >= 0; argument--) {
            ExpressionNode arg = stack.pop();
            if (arg.getType().isStructure()) {
                argument -= arg.getType().composite.size() - 1;
                callBackArgs[argument] = arg;
            } else {
                callBackArgs[argument] = CS2TypeUtil.cast(arg, CS2Type.forJagexDesc(descriptor.charAt(argument)));
            }
        }
        ExpressionNode scriptIdExpr = stack.pop();

        int scriptId = ((IntExpressionNode) scriptIdExpr).data;
        FunctionInfo callBackInfo = null;
        if (scriptId != -1) {
            callBackInfo = decompiler.getScriptsDatabase().getInfo(CS2Instruction.getByOpcode(scriptId));
            analyzeActualArgOrder(callBackInfo, argsCopy);
        }

        if (callBackInfo != null) {
            //This is the most accurate source of type information. As their compiler generated it!
            for (int argument = 0; argument < callBackInfo.getArgumentTypes().length; argument++) {
                callBackInfo.getArgumentTypes()[argument] = CS2Type.forJagexDesc(descriptor.charAt(argument));
            }
        }


        stack.push(new CallbackExpressionNode(callBackInfo == null ? null : new CallExpressionNode(callBackInfo, callBackArgs, false), trigger));
        if (invokeOn != null) {
            stack.push(invokeOn);
        }
    }

    private int analyzeCall(FunctionInfo info, FlowBlock block, CS2Stack stack, int ptr, boolean staticArgTypes, boolean dynamicResultType, boolean isOpCodeCall, int callId, boolean useReg1) {
        CS2Type returnType = info.getReturnType();

        boolean objCall = false;

//        if (!CS2.OSRS) {
        if (isOpCodeCall && (callId == 150 || callId == 151 || callId == 200 || callId == 201 || callId == 204 || callId == 205 || callId == 3109 || (callId >= 1000 && callId < 2000) || (callId >= 21000 && callId < 22000))) {
            objCall = true;
        }
//        } else {
//            if (isOpCodeCall && (callId == 100 || callId == 101 || callId == 200 || callId == 201 || callId == 3109 || (callId >= 1000 && callId < 1600))) {
//                objCall = true;
//            }
//        }

        ExpressionNode[] args = new ExpressionNode[info.getArgumentTypes().length + (objCall ? 1 : 0)];
        if (objCall) {
            args[info.getArgumentTypes().length] = new VariableLoadNode(useReg1 ? _CHILD : CHILD);
        }
//        objCall = false; //TODO: DISABLED THIS. not sure if its a good idea or not to have this. widget1.setText("test") vs setText("test", widget1) Should refactor this if gonna use this!
        for (int i = info.getArgumentTypes().length - 1; i >= 0; i--) {
            CS2Type type = info.getArgumentTypes()[i];
            ExpressionNode expr;
            expr = stack.pop();
            if (expr.getType().isStructure()) {
                i++;
                for (CS2Type t : expr.getType().composite) {
                    assert info.getArgumentTypes()[i - 1].isCompatible(t);
                    i--;
                }
                args[i] = new CastNode(expr.getType(), expr); //add explicit cast for clarity eg: script_3387(cs2method6131(), getDisplayMode(), (int, int)script_2692(cs2method6131()), 0);
                continue;
            }

            //Passing a local to a non int call? Change the locals type to the required call type (these will be compatible types)
            if (type != CS2Type.INT && expr instanceof VariableLoadNode && ((VariableLoadNode) expr).getVariable() instanceof LocalVariable) {
                ((LocalVariable) ((VariableLoadNode) expr).getVariable()).changeType(type);
            }
            //Passing a int call to a call? Change the return type of the call to the required call type (only for script calls, cause dynamic types in opcodes setWidget(getDynamicInt())
            if (type != CS2Type.INT && expr.getType() == CS2Type.INT && expr instanceof CallExpressionNode) {
//                    if (((CallExpressionNode) expr).info.isScript)
                ((CallExpressionNode) expr).info.returnType = type;
//                    if (!((CallExpressionNode) expr).info.isScript) {
//                        try {
//                            FileWriter fw = new FileWriter("opcodehint.txt", true);
//                            fw.write(info.toString() + "\r\n");
//                            fw.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
            }
            if (staticArgTypes && type == CS2Type.INT && expr.getType() != CS2Type.BOOLEAN && type != expr.getType()) {
                //infer (change) definition, cant do this for ALL opcode calls because some are dynamic though!!! eg datamap... script hooks, but could for most
                //TODO: Don't infer boolean args? they are sometimes passed to function that also take int
                assert info.getArgumentTypes()[i].isCompatible(expr.getType());
//                if (!CS2.OSRS)
                info.getArgumentTypes()[i] = expr.getType();
                type = expr.getType();
                if (isOpCodeCall) {
                    try {
                        FileWriter fw = new FileWriter("opcodehint.txt", true);
                        fw.write(info.toString() + "\r\n");
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            args[i] = CS2TypeUtil.cast(expr, type);
        }

//        if (!CS2.OSRS) {

        objCall |= isOpCodeCall && (callId == 152 || callId == 202 || callId == 203 || (callId >= 2000 && callId < 3000 && callId != 2704 && callId != 2705) || (callId >= 22000 && callId < 23000));
//        } else {
//        objCall |= isOpCodeCall && (callId == 102 || (callId >= 2000 && callId < 2600));
//        }


        if (returnType == CS2Type.VOID) { // void
            block.write(new PopableNode(new CallExpressionNode(info, args, objCall)));
        } else {
            stack.push(new CallExpressionNode(info, args, objCall));
        }
        return -1;
    }


    private FlowBlock generateFlowBlock(Label label, CS2Stack variableStack) {
        FlowBlock same = byAddress.get(label.getAddress() + 1);
        if (same != null) {
            if (checkMerging(same.getStack(), variableStack)) {
                return same;
            }
        }

        int blockID = blockCount++;
        FlowBlock b = new FlowBlock(blockID, label.getAddress() + 1, variableStack);
        unprocessedBlocks.add(b);
        if (same == null) {
            byAddress.put(label.getAddress() + 1, b);
        } else {
            //its possible to enter a label with a different stack, original code would have looked different though (eg inline conditionals)
//            System.out.println("Warning: Duplicating block to blockid " + blockID + " (different stack)! " + this.cs2.scriptID);
            throw new DecompilerException("bad flow");
        }
        return b;

    }

    /**
     * Check's if two stacks can be merged.
     * false is returned if stack sizes doesn't match or
     * one of the elements in the first or second stack is not dumped
     * to same local variables.
     */
    private boolean checkMerging(CS2Stack v0, CS2Stack v1) {
//        if (v0.getSize() != v1.getSize()) {
//            return false;
//        }
        CS2Stack c0 = v0.copy();
        CS2Stack c1 = v1.copy();
        while (c0.getSize() > 0 || c1.getSize() > 0) {
            if (c0.getSize() > 0 && c0.peek().getType() == CS2Type.UNKNOWN) {
                c0.pop();
                continue;
            }
            if (c1.getSize() > 0 && c1.peek().getType() == CS2Type.UNKNOWN) {
                c1.pop();
                continue;
            }
            if (c0.getSize() == 0 || c1.getSize() == 0) {
                return false;
            }
            ExpressionNode e0 = c0.pop();
            ExpressionNode e1 = c1.pop();
            if (!(e0 instanceof VariableLoadNode) || !(e1 instanceof VariableLoadNode))
                return false;
            if (((VariableLoadNode) e0).getVariable() != ((VariableLoadNode) e1).getVariable())
                return false;
        }
        return true;
    }

    public List<FlowBlock> getBlocks() {
        return blocks;
    }

}
