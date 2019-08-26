package com.rs.tools.cs2.instructions;

import com.rs.cache.loaders.cs2.CS2Instruction;

public class AbstractInstruction {

	private CS2Instruction opcode;
	private int address;

	public AbstractInstruction(CS2Instruction opcode) {
		this.opcode = opcode;
		this.address = -1;
	}

	public void setOpcode(CS2Instruction opcode) {
		this.opcode = opcode;
	}

	public CS2Instruction getOpcode() {
		return opcode;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getAddress() {
		return address;
	}

}
