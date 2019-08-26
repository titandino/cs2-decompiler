package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;

public class IntInstruction extends AbstractInstruction {

    private int constant;

    public IntInstruction(CS2Instruction opcode, int constant) {
        super(opcode);
        this.constant = constant;
    }

    public int getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        String name = Opcodes.getOpcodeName(this.getOpcode());
        if (name.equals("CALL_CS2")) {
//            if (Opcodes.scriptsDb != null) {
//                FunctionInfo f = Opcodes.scriptsDb.getInfo(constant);
//                if (f != null) {
//                    return String.format("%-16.16s %s", "CALL_" + constant, f.toString());
//                }
//            }
        }
        return String.format("%-16.16s %d", name, constant);
    }

}
