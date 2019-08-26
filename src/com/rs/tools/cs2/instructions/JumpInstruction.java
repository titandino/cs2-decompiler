package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;

public class JumpInstruction extends AbstractInstruction {

	private Label target;

	public JumpInstruction(CS2Instruction opcode, Label target) {
		super(opcode);
		this.target = target;
//		target.getJumpers().add(this);
	}

	public Label getTarget() {
		return target;
	}

	public void setTarget(Label target) {
		this.target = target;
	}

	@Override
	public String toString() {
		return String.format("%-16.16s %s", Opcodes.getOpcodeName(this.getOpcode()), target.toString());
	}

}
