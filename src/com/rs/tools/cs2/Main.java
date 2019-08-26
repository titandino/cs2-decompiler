package com.rs.tools.cs2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.instructions.Opcodes;
import com.rs.tools.cs2.util.FunctionDatabase;
import com.rs.tools.cs2.util.FunctionInfo;

public class Main {

    public static int version = 727;
    public static boolean GENERATE_SCRIPTS_DB = true;

    //This was used to generate the scripts db and find broken scripts when starting with a new revision where a lot of the opcodes are still wrong/missing in the master file

    public static void main(String[] args) throws Throwable {
        Map<Integer, Integer> scramble = new HashMap<>();
        Map<Integer, Integer> unscramble = new HashMap<>();
        Unscramble.read(version, scramble, unscramble);
        FunctionDatabase opcodesDatabase = Opcodes.opcodesDb = new FunctionDatabase(new File("opcodes_db.ini"), false, scramble);
        FunctionDatabase scriptsDatabase = new FunctionDatabase(new File("scripts_db_" + version + ".ini"), true, null);
//3062
        //1025 reverse args, all multiple return args are prob reverserd
        int id = 299;// 1182;// 1163;
        // TODO 3067
        if (!GENERATE_SCRIPTS_DB)
//            for (id = 0; id <= 7000; id++)
        {
//                if(id == 1613 || id == 3239 || id == 4344 || id == 4508 || id == 5311 || id == 6000) continue;
//            if (id == 427 || id == 837 || id == 1456 || id == 254 || id == 6 || id == 656 || id == 168 || id == 252 || id == 253 || id == 2046)  continue; //takes a long time

            try {
                CS2 cs2 = CS2Reader.readCS2ScriptNewFormat(new File("binary/" + version + "/" + id), id);
//                    if (cs2 == null) continue;
                int maxl = String.valueOf(cs2.getInstructions().length).length();
                String format = "[%0" + maxl + "d]: %s" + System.lineSeparator();
                for (int i = 0; i < cs2.getInstructions().length; i++) {
                    System.err.printf(format, i, cs2.getInstructions()[i]);
                }
                CS2Decompiler decompiler = new CS2Decompiler(cs2, opcodesDatabase, scriptsDatabase);
                decompiler.decompile();
                decompiler.optimize();

                CodePrinter printer = new CodePrinter();
                decompiler.getFunction().print(printer);
                System.err.println(printer.toString());
                FileWriter fw = new FileWriter("sources/" + version + "/" + id + ".cs2");
                fw.write(printer.toString());
                fw.write("\r\n\r\n\r\n");
                for (int i = 0; i < cs2.getInstructions().length; i++) {
                    fw.write(String.format(format, i, cs2.getInstructions()[i]));
                }
                fw.flush();
                fw.close();
            } catch (Throwable e) {
                e.printStackTrace();
                System.err.println("could not decompile " + id);
//                System.exit(1);
            }
        }

        //multiple passes for type analysis
        if (GENERATE_SCRIPTS_DB)
            for (int i = 0; i < 5; i++)
                generateScriptsDB(scramble, unscramble);

    }


    public static void generateScriptsDB(Map<Integer, Integer> scramble, Map<Integer, Integer> unscramble) throws Throwable {
        FunctionDatabase opcodesDatabase = new FunctionDatabase(new File("opcodes_db.ini"), false, scramble);
//        FunctionDatabase scriptsDatabase = new FunctionDatabase(); //Create one from scratch
        FunctionDatabase scriptsDatabase = new FunctionDatabase(new File("scripts_db_" + version + ".ini"), true, null); //infer types based on infered types based on infered types...

        int decompiledScripts = 0;
        int lastDecompiledScripts = 0;
        int errors = 0;
        boolean[] decompiled = new boolean[7000];
        do {
            lastDecompiledScripts = decompiledScripts;
            decompiledScripts = 0;
            errors = 0;


            for (int i = 0; i < 7000; i++) {
                if (!decompiled[i]) {
                    try {
                        CS2 script = CS2Reader.readCS2ScriptNewFormat(new File("binary/" + version + "/" + i), i);
                        if (script == null) continue;
                        CS2Decompiler dec = new CS2Decompiler(script, opcodesDatabase, scriptsDatabase);
                        System.out.println("script: " + i);
                        try {
                            dec.decompile();
                        } catch (Exception ex) {
                            System.err.println("error: " + i);
                            ex.printStackTrace();
                            errors++;
                        }
                        if (dec.getFunction().getReturnType() != CS2Type.UNKNOWN) {
                            FunctionInfo info = scriptsDatabase.getInfo(CS2Instruction.getByOpcode(i));
                            info.name = dec.getFunction().getName();
                            if (info.returnType == CS2Type.UNKNOWN)
                                info.returnType = dec.getFunction().getReturnType();
                            for (int a = 0; a < dec.getFunction().getArgumentLocals().length; a++) {
                                info.argumentTypes[a] = dec.getFunction().getArgumentLocals()[a].getType();
                                info.argumentNames[a] = dec.getFunction().getArgumentLocals()[a].getName();
                            }
                            decompiled[script.getScriptID()] = true;
                            decompiledScripts++;
                        }
                    } catch (Exception e) {
                        //unknown scrambling etc
                    }
                } else {
                    decompiledScripts++;
                }
            }

            System.err.println("Last:" + lastDecompiledScripts);
            System.err.println("Decompiled:" + decompiledScripts);
            System.err.println("Errors:" + errors);
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("scripts_db_" + version + ".ini")));
            for (int i = 0; i < 7000; i++) {
                FunctionInfo info = scriptsDatabase.getInfo(CS2Instruction.getByOpcode(i));
                if (info == null || info.getReturnType() == null) continue;
                if (info.getReturnType().isStructure()) {
                    writer.write(i + " " + info.getName() + " {" + info.getReturnType().toString().replaceAll(" ", "") + "}");
                } else {
                    writer.write(i + " " + info.getName() + " " + info.getReturnType().toString());
                }
                for (int a = 0; a < info.getArgumentTypes().length; a++)
                    writer.write(" " + info.getArgumentTypes()[a].toString() + " " + info.getArgumentNames()[a]);
                writer.write("\r\n");
            }
            writer.close();
        }
        while (decompiledScripts > lastDecompiledScripts);


    }

}
