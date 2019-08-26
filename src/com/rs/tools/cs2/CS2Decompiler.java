package com.rs.tools.cs2;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.ast.FunctionNode;
import com.rs.tools.cs2.ast.LocalVariable;
import com.rs.tools.cs2.util.FunctionDatabase;

public class CS2Decompiler {

    private CS2 cs2;
    private FunctionNode function;
    private FunctionDatabase opcodesDatabase;
    private FunctionDatabase scriptsDatabase;

//    public CS2Decompiler(CS2 cs2) {
//        this(cs2, new FunctionDatabase(new File("opcodes_db.ini"), false), new FunctionDatabase(new File("scripts_db.ini"), true));
//    }

    public CS2Decompiler(CS2 cs2, FunctionDatabase opcodesDatabase, FunctionDatabase scriptsDatabase) {
        this.opcodesDatabase = opcodesDatabase;
        this.scriptsDatabase = scriptsDatabase;
        this.cs2 = cs2;
//        FunctionInfo info = scriptsDatabase.getInfo(cs2.getScriptID());
////        if (info == null) throw new DecompilerException("no script def in scripts db " + cs2.getScriptID());
//        if (info == null) {
//            info = new FunctionInfo("script_" + cs2.scriptID, cs2.getScriptID(), cs2.getArguments(), CS2Type.UNKNOWN, IntStream.range(0, cs2.getArguments().length).mapToObj(i -> "arg" + i).toArray(String[]::new), true);
//            scriptsDatabase.putInfo(cs2.getScriptID(), info);
//        }
//        this.function = new FunctionNode(info.getName(), info.getArgumentTypes(), info.getReturnType(), new ScopeNode());
    }


    public void decompile() throws DecompilerException {
        this.declareAllVariables();
        FlowBlocksGenerator generator = new FlowBlocksGenerator(this);
        generator.generate();

        ControlFlowSolver main = new ControlFlowSolver(this, function.getMainScope(), generator.getBlocks());
        main.solve();
    }

    public void optimize() {

    }

    private void declareAllVariables() {
        int ic = 0, sc = 0, lc = 0;
        for (int i = 0; i < function.getArguments().length; i++) {
            CS2Type argument = function.getArguments()[i];
            if (argument.isCompatible(CS2Type.INT)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(ic++, 0));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else if (argument.isCompatible(CS2Type.STRING)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(sc++, 1));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else if (argument.isCompatible(CS2Type.LONG)) {
                LocalVariable var = new LocalVariable("arg" + i, argument, true);
                var.setIdentifier(LocalVariable.makeIdentifier(lc++, 2));
                this.function.getMainScope().declare(this.function.getArgumentLocals()[i] = var);
            } else {
                throw new RuntimeException("structs in args?");
            }
        }
        if (ic != cs2.getIntArgumentsCount() || sc != cs2.getStringArgumentsCount() || lc != cs2.getLongArgumentsCount())
            throw new RuntimeException("Expected signature " + function.toString()+" binary args i"+cs2.getIntArgumentsCount()+" s"+cs2.getStringArgumentsCount()+" l"+cs2.getLongArgumentsCount());

        for (int i = cs2.getIntArgumentsCount(); i < cs2.getIntLocalsSize(); i++) {
            LocalVariable var = new LocalVariable("int" + i, CS2Type.INT);
            var.setIdentifier(LocalVariable.makeIdentifier(i, 0));
            this.function.getMainScope().declare(var);
        }
        for (int i = cs2.getStringArgumentsCount(); i < cs2.getStringLocalsSize(); i++) {
            LocalVariable var = new LocalVariable("str" + i, CS2Type.STRING);
            var.setIdentifier(LocalVariable.makeIdentifier(i, 1));
            this.function.getMainScope().declare(var);
        }
        for (int i = cs2.getLongArgumentsCount(); i < cs2.getLongLocalsSize(); i++) {
            LocalVariable var = new LocalVariable("long" + i, CS2Type.LONG);
            var.setIdentifier(LocalVariable.makeIdentifier(i, 2));
            this.function.getMainScope().declare(var);
        }
    }


    public CS2 getCs2() {
        return cs2;
    }

    public FunctionNode getFunction() {
        return function;
    }

    public FunctionDatabase getOpcodesDatabase() {
        return opcodesDatabase;
    }

    public FunctionDatabase getScriptsDatabase() {
        return scriptsDatabase;
    }


}
