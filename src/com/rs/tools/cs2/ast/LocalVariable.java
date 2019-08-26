package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.DecompilerException;
import com.rs.tools.cs2.instructions.AbstractInstruction;
import com.rs.tools.cs2.instructions.IntInstruction;

public class LocalVariable implements Variable {

    public static LocalVariable CHILD = new LocalVariable("CHILD", null, false);
    public static LocalVariable _CHILD = new LocalVariable("_CHILD", null, false);

    private String name;
    private CS2Type type;
    private int identifier = -1;
    private boolean isArgument;

    public LocalVariable(String name, CS2Type type) {
        this(name, type, false);
    }

    public LocalVariable(String name, CS2Type type, boolean isArgument) {
        this.name = name;
        this.type = type;
        this.isArgument = isArgument;
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    public void changeType(CS2Type type) {
        if (!type.isCompatible(this.type)) {
            throw new DecompilerException("incompatible");
        }
        this.type = type;
        if (type != CS2Type.INT) {
            name = name.replace("arg", type.getName().toLowerCase()).replace("int", type.getName().toLowerCase());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isArgument() {
        return isArgument;
    }

    @Override
    public String toString() {
        return type + " " + name;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public int getIdentifier() {
        return identifier;
    }

    public static int makeIdentifier(int index, int stackType) {
        return index | stackType << 16;
    }

    @Override
    public AbstractInstruction generateStoreInstruction() {
        int type = getIdentifier() >> 16;
        int id = getIdentifier() & 0xffff;
        if (type == 0) {
            return new IntInstruction(CS2Instruction.STORE_INT, id);
        } else if (type == 1) {
            return new IntInstruction(CS2Instruction.STORE_STRING, id);
        } else if (type == 2) {
            return new IntInstruction(CS2Instruction.STORE_LONG, id);
        }
        assert false : this;
        return null;
    }

    @Override
    public AbstractInstruction generateLoadInstruction() {
        int type = getIdentifier() >> 16;
        int id = getIdentifier() & 0xffff;
        if (type == 0) {
            return new IntInstruction(CS2Instruction.LOAD_INT, id);
        } else if (type == 1) {
            return new IntInstruction(CS2Instruction.LOAD_STRING, id);
        } else if (type == 2) {
            return new IntInstruction(CS2Instruction.LOAD_LONG, id);
        }
        //??
        throw new DecompilerException("unhandled var type" + this);
    }
}
