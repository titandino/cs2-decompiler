package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;

public class LongInstruction extends AbstractInstruction {

    private long constant;

    public LongInstruction(CS2Instruction opcode, long constant) {
        super(opcode);
        this.constant = constant;
    }

    public long getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        return String.format("%-16.16s %d", Opcodes.getOpcodeName(this.getOpcode()), constant);
    }

}
