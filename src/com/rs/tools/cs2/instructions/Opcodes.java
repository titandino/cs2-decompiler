package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.tools.cs2.util.FunctionDatabase;

public class Opcodes {

    public static FunctionDatabase opcodesDb = null;

    public static String getOpcodeName(CS2Instruction opcode) {
        return opcode.name();
    }

    public static CS2Instruction oppositeJump(CS2Instruction op) {
        switch (op) {
            case INT_EQ:
                return CS2Instruction.INT_NE;
            case INT_NE:
                return CS2Instruction.INT_EQ;
            case INT_GE:
                return CS2Instruction.INT_LT;
            case INT_LT:
                return CS2Instruction.INT_GE;
            case INT_GT:
                return CS2Instruction.INT_LE;
            case INT_LE:
                return CS2Instruction.INT_GT;
                default:
                	return null;
        }
    }

    public static boolean isAssign(CS2Instruction op) {
        switch (op) {
            case POP_INT:
            case POP_STRING:
            case POP_LONG:
            case STORE_INT:
            case STORE_STRING:
            case STORE_LONG:
            case STORE_VARP:
            case STORE_VARPBIT:
            case STORE_VARC:
            case STORE_VARC_STRING:
//            case ARRAY_STORE:
                return true;
            default:
            	return false;
        }
    }

}
