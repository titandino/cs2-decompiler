package com.rs.tools.cs2.util;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.tools.cs2.ast.Operator;

public class OpcodeUtils {
	public static int getTwoConditionsJumpStackType(CS2Instruction opcode) {
		if (opcode == CS2Instruction.INT_NE || opcode == CS2Instruction.INT_LT || opcode == CS2Instruction.INT_GT || opcode == CS2Instruction.INT_LE || opcode == CS2Instruction.INT_GE)
			return 0;
		else if (opcode == CS2Instruction.LONG_NE || opcode == CS2Instruction.LONG_LT || opcode == CS2Instruction.LONG_GT || opcode == CS2Instruction.LONG_LE || opcode == CS2Instruction.LONG_GE)
			return 2;
		else
			return -1;
	}
	
	public static Operator getTwoConditionsJumpConditional(CS2Instruction opcode) {
		switch (opcode) {
			case INT_NE:
			case LONG_NE:
				return Operator.NEQ; // !=
			case INT_EQ:
			case LONG_EQ:
				return Operator.EQ; // ==
			case INT_LT:
			case LONG_LT:
				return Operator.LT; // <
			case INT_GT:
			case LONG_GT:
				return Operator.GT; // >
			case INT_LE:
			case LONG_LE:
				return Operator.LE; // <=
			case INT_GE:
			case LONG_GE:
				return Operator.GE; // >=
			default:
				return null;
		}
	}
	
}
