package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.DecompilerException;
import com.rs.tools.cs2.instructions.AbstractInstruction;
import com.rs.tools.cs2.instructions.IntInstruction;

public class GlobalVariable implements Variable {

    public static GlobalVariable find(String name, int idx, CS2Type type) {
        return new GlobalVariable(name, idx, type);
    }

    public static GlobalVariable VARP(int idx, CS2Type type) {
        return new GlobalVariable("VARP", idx, type);
    }

    public static GlobalVariable VARPBIT(int idx, CS2Type type) {
        return new GlobalVariable("VARPBIT", idx, type);
    }

    public static GlobalVariable VARC(int idx, CS2Type type) {
        return new GlobalVariable("VARC", idx, type);
    }

    public static GlobalVariable VARC_STRING(int idx) {
        return new GlobalVariable("STRING", idx, CS2Type.STRING);
    }

    public static GlobalVariable parse(String n) {
        int idx = Integer.parseInt(n.substring(n.indexOf('[') + 1, n.indexOf(']')));
        String prefix = n.substring(0, n.indexOf('['));
        switch (prefix) {
            case "VARP":
            case "VARPBIT":
            case "VARC":
            case "CLAN":
            case "CLANBIT":
            case "CLANDEF112":
            case "CLANDEF113":
                return find(prefix, idx, CS2Type.INT);
            case "STRING":
            case "CLANDEF_STRING":
            case "CLANDEF_STRING115":
                return find(prefix, idx, CS2Type.STRING);
            case "CLANDEF_LONG":
            case "CLANDEF_LONG114":
                return find(prefix, idx, CS2Type.LONG);
            default:
                throw new DecompilerException("I don't know how to parse this");
        }
    }

    private final String name;
    private final int idx;
    private CS2Type type;

    private GlobalVariable(String name, int idx, CS2Type type) {
        this.name = name;
        this.idx = idx;
        this.type = type;
    }

    @Override
    public String getName() {
        return name + "[" + idx + "]";
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    @Override
    public AbstractInstruction generateStoreInstruction() {
        switch (name) {
            case "VARP":
                return new IntInstruction(CS2Instruction.STORE_VARP, idx);
            case "VARPBIT":
                return new IntInstruction(CS2Instruction.STORE_VARPBIT, idx);
            case "VARC":
                return new IntInstruction(CS2Instruction.STORE_VARC, idx);
            case "STRING":
                return new IntInstruction(CS2Instruction.STORE_VARC_STRING, idx);
            default:
                throw new DecompilerException("This global is read-only");
        }
    }

    @Override
    public AbstractInstruction generateLoadInstruction() {
    	CS2Instruction op = null;
        switch (name) {
            case "VARP":
                op = CS2Instruction.LOAD_VARP;
                break;
            case "VARPBIT":
                op = CS2Instruction.LOAD_VARPBIT;
                break;
            case "VARC":
                op = CS2Instruction.LOAD_VARC;
                break;
            case "STRING":
                op = CS2Instruction.LOAD_VARC_STRING;
                break;
            //These are READONLY, some are not even used
            case "CLANDEF_STRING115":
                op = CS2Instruction.LOAD_CLAN_SETTING_VAR_STRING;
                break;
            case "CLANDEF_LONG114":
                op = CS2Instruction.LOAD_CLAN_SETTING_VAR_LONG;
                break;
            case "CLANDEF113":
                op = CS2Instruction.LOAD_CLAN_SETTING_VARBIT;
                break;
            case "CLANDEF112":
                op = CS2Instruction.LOAD_CLAN_VAR;
                break;
            case "CLANDEF_STRING":
                op = CS2Instruction.LOAD_CLAN_VAR_STRING;
                break;
            case "CLANDEF_LONG":
                op = CS2Instruction.LOAD_CLAN_VAR_LONG;
                break;
            case "CLANBIT":
                op = CS2Instruction.LOAD_CLAN_VARBIT;
                break;
            case "CLAN":
                op = CS2Instruction.LOAD_CLAN_VAR;
                break;
            default:
                throw new DecompilerException("I don't know how to load this");
        }
        return new IntInstruction(op, idx);

    }
}
