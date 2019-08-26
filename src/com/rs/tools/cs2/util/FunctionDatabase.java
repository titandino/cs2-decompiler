package com.rs.tools.cs2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.cache.loaders.cs2.CS2Type;

public class FunctionDatabase {

    private File file;
    private HashMap<CS2Instruction, FunctionInfo> info;
    private Map<String, List<FunctionInfo>> lookup = new HashMap<>();

    public FunctionDatabase(File file, boolean isScript, Map<Integer, Integer> scramble) {
        this.file = file;
        this.info = new HashMap<>();
        this.readDatabase(isScript, scramble);
    }

    public FunctionDatabase() {
        this.info = new HashMap<>();
    }

    public FunctionDatabase copy() {
        FunctionDatabase cpy = new FunctionDatabase();
        System.arraycopy(info, 0, cpy.info, 0, info.size());
        for (Entry<String, List<FunctionInfo>> entry : lookup.entrySet()) {
            cpy.lookup.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return cpy;
    }

    private void readDatabase(boolean isScript, Map<Integer, Integer> scramble) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            int linesCount = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine(), linesCount++) {
                if (line.length() <= 0 || line.startsWith(" ") || line.startsWith("//") || line.startsWith("#"))
                    continue;
                try {
                    String[] split = line.split(" ");
                    int opcode = Integer.parseInt(split[0]);
                    if(scramble != null && !scramble.containsKey(opcode)) {
                        continue;
                    }
                    String name = split[1];
                    CS2Type returnType = CS2Type.forDesc(split[2]);
                    CS2Type[] argTypes = new CS2Type[(split.length - 2) / 2];
                    String[] argNames = new String[(split.length - 2) / 2];
                    int write = 0;
                    for (int i = 3; i < split.length; i += 2) {
                        argTypes[write] = CS2Type.forDesc(split[i]);
                        argNames[write++] = split[i + 1];
                    }
                    //putInfo(opcode, new FunctionInfo(name, opcode, argTypes, returnType, argNames, isScript));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("Error parsing function database file " + this.file + " on line:" + (linesCount + 1));
                }
            }
            reader.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    public FunctionInfo getInfo(CS2Instruction opcode) {
        return info.get(opcode);
    }


    public void putInfo(CS2Instruction opcode, FunctionInfo f) {
    	this.info.put(opcode, f);
        lookup.computeIfAbsent(f.getName(), n -> new ArrayList<>());
        lookup.get(f.getName()).add(f);
    }

    public List<FunctionInfo> getByName(String symbol) {
        return lookup.getOrDefault(symbol, Collections.emptyList());
    }

}
