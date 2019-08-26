package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.tools.cs2.util.TextUtils;

public class StringInstruction extends AbstractInstruction {

    private String constant;

    public StringInstruction(CS2Instruction opcode, String constant) {
        super(opcode);
        this.constant = constant;
    }

    public String getConstant() {
        return constant;
    }

    @Override
    public String toString() {
        return String.format("%-16.16s %s", Opcodes.getOpcodeName(this.getOpcode()), TextUtils.quote(constant));
    }

}
