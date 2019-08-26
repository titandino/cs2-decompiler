package com.rs.tools.cs2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.rs.cache.loaders.cs2.CS2Instruction;
import com.rs.io.InputStream;

public class Unscramble {

    //    public static HashMap<Integer, Integer> unscrambled = new HashMap<>();
//    public static HashMap<Integer, Integer> reverse = new HashMap<>();
//    public static Set<Integer> unsure = new HashSet<>();
//

    public static void read(int version, Map<Integer, Integer> scramble, Map<Integer, Integer> unscramble) {
        try {
            for (String line : Files.readAllLines(Paths.get(version + ".txt"))) {
                String parts[] = line.split(" ");
                if (parts.length >= 2) {
                    try {
                        int master = Integer.parseInt(parts[0]);
                        int scrambled = Integer.parseInt(parts[1]);
                        if (scramble.put(master, scrambled) != null) {
                            throw new RuntimeException("Bad config, duplicate scramble " + master + " ->  ...");
                        }
                        if (unscramble.put(scrambled, master) != null) {
                            throw new RuntimeException("Bad config, duplicate scramble ... -> " + scrambled);
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Map<CS2Instruction, Integer> scramble = new HashMap<>();
    static Map<Integer, CS2Instruction> unscramble = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int version = 727;
        System.out.println("Unscrambling...");
        List<Integer> src = Files.readAllLines(Paths.get("Instructions718.java")).stream().filter(l -> l.contains("new Instruction")).map(l -> Integer.parseInt(l.substring(l.indexOf("new Instruction")).replaceAll("[^\\d]", ""))).collect(Collectors.toList());
        List<Integer> opdb = Files.readAllLines(Paths.get("opcodes_db.ini")).stream().filter(l -> !l.startsWith("#") && !l.trim().isEmpty()).map(l -> Integer.parseInt(l.split(" ")[0])).sorted().collect(Collectors.toList());
        System.out.println(opdb);
//        System.out.println(opdb);
//        List<Integer> known = Files.readAllLines(Paths.get("718.txt")).stream().map(l -> Integer.parseInt(l.split(" ")[0])).collect(Collectors.toList());
        Map<Integer, Integer> known = new HashMap<>();
        Map<Integer, Integer> knownr = new HashMap<>();
        List<Integer> matched = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get("727.txt"))) {
            known.put(Integer.parseInt(line.split(" ")[1]), Integer.parseInt(line.split(" ")[0]));
            knownr.put(Integer.parseInt(line.split(" ")[0]), Integer.parseInt(line.split(" ")[1]));

            matched.add(Integer.parseInt(line.split(" ")[0]));
        }
        Iterator<Integer> order = src.iterator();
        int toFind = order.next();
        int i = 0;
//        int lastFound = -1;
        l:
        for (Iterator<Integer> it = opdb.iterator(); it.hasNext(); ) {
            Integer op = it.next();
//            if (op >= matched.get(i)) {
            while (op >= matched.get(i)) {
                if (!known.containsKey(toFind)) {
                    System.out.println("? " + toFind + " #TODO");
                    toFind = order.next();
                } else {
                    System.out.println(known.get(toFind) + " " + toFind);
                    while (known.get(toFind) >= op) {
                        if (known.get(toFind) > op) {
                            System.out.println("# rip? " + op);
                        }
                        op = it.next();
                    }
                    while (known.get(toFind) >= matched.get(i)) {
                        i++;
//                        System.out.println("next=" + matched.get(i));
                    }
                    toFind = order.next();
                }
            }
//                op = it.next();
//            }
            while (toFind == knownr.get(matched.get(i)) && op != (int) matched.get(i)) {
                System.out.println("# missing? " + op);
                continue l;

            }
//            if (op != (int) matched.get(i)) {
            System.out.println(op + " " + toFind + " #guessed! " + matched.get(i) + " " + knownr.get(matched.get(i)));
            toFind = order.next();
//            }

//            if (!Objects.equals(op, matched.get(i))) {
//                System.out.println(op + " " + toFind + " #guessed");
//                toFind = order.next();
//            }
        }
//        Iterator<Integer> opdbit = opdb.iterator();
//        int currdb = opdbit.next();
//        loop:
//        for (String line : Files.readAllLines(Paths.get("718.txt"))) {
//            int unscrambled = Integer.parseInt(line.split(" ")[0]);
//            int scrambled = Integer.parseInt(line.split(" ")[1]);
//            int curr = order.next();
////            while (unscrambled > currdb && opdbit.hasNext()) {
////                currdb = opdbit.next();
////            }
//            while (scrambled != curr) {
//                boolean contains = known.contains(currdb);
//                if (currdb > unscrambled || contains) {
//                    System.out.println("? " + curr);
//                } else {
//                    System.out.println(currdb + " " + curr + " #unverified");
//                    if (opdbit.hasNext()) {
//                        currdb = opdbit.next();
//                    } else {
//                        currdb = -1;
//                    }
//                    break;
//                }
//                curr = order.next();
//            }
//            System.out.println(line);
//            if (currdb == unscrambled) {
//                currdb = opdbit.next();
//            }
//            while (unscrambled >= currdb && opdbit.hasNext()) {
//                System.out.println("#skip? " + currdb + " ?");
//                currdb = opdbit.next();
//
//            }
//        }
//        while (order.hasNext()) {
//            System.out.println("? " + order.next());
//        }


//        Opcodes.opcodesDb = new FunctionDatabase(new File("opcodes_db.ini"), false);
//        int i = 60;
//        for (int i = 0; i <= 5376; i++) {
//            try {
//                unscramble(i, Files.readAllBytes(Paths.get("binary", "667", "" + i)), Files.readAllBytes(Paths.get("binary", "" + version, "" + i)));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        FileWriter fw = new FileWriter("718.txt");
//        for (Entry<Integer, Integer> entry : scramble.entrySet().stream().sorted((e, e2) -> e.getKey() - e2.getKey()).collect(Collectors.toList())) {
//            fw.write(entry.getKey() + " " + entry.getValue() + "\r\n");
//        }
//        fw.close();
//        System.out.println("Done: " + Opcodes.unscramble.size());
    }

    private static void unscramble(int scriptID, byte[] data, byte[] scrambledData) throws IOException {
        if (data.length != scrambledData.length) {
            return;
        }
        InputStream buffer = new InputStream(data);
        InputStream scrambled = new InputStream(scrambledData);
        boolean hasSwitches = true; //old OSRS doesnt have switches
        boolean newFormat = true;

        buffer.setOffset(data.length - 2);
        scrambled.setOffset(scrambledData.length - 2);
        int switchBlocksSize = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != switchBlocksSize) {
            return;
        }

        int codeBlockEnd = data.length - switchBlocksSize - 16 - 2;
        buffer.setOffset(codeBlockEnd);
        scrambled.setOffset(codeBlockEnd);

        int codeSize = buffer.readInt();
        if (scrambled.readInt() != codeSize) {
            return;
        }
        int intLocalsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != intLocalsCount) {
            return;
        }
        int stringLocalsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != stringLocalsCount) {
            return;
        }
        int longLocalsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != longLocalsCount) {
            return;
        }
        int intArgsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != intArgsCount) {
            return;
        }
        int stringArgsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != stringArgsCount) {
            return;
        }
        int longArgsCount = buffer.readUnsignedShort();
        if (scrambled.readUnsignedShort() != longArgsCount) {
            return;
        }
        int switchesCount = buffer.readUnsignedByte();
        if (scrambled.readUnsignedByte() != switchesCount) {
            return;
        }
        Map[] switches = new HashMap[switchesCount];
        for (int i = 0; i < switchesCount; i++) {
            int numCases = buffer.readUnsignedShort();
            if (scrambled.readUnsignedShort() != numCases) {
                return;
            }
            switches[i] = new HashMap<Integer, Integer>(numCases);
            while (numCases-- > 0) {
                switches[i].put(buffer.readInt(), buffer.readInt());
            }
        }
        buffer.setOffset(0);
        scrambled.setOffset(0);
        String scriptName = buffer.readNullString();
        scrambled.readNullString();
        Map<CS2Instruction, Integer> s = new HashMap<>();
        Map<Integer, CS2Instruction> r = new HashMap<>();
        while (buffer.getOffset() < codeBlockEnd) {
            CS2Instruction opcode = CS2Instruction.getByOpcode(buffer.readUnsignedShort());
            int scrambledOp = scrambled.readUnsignedShort();
            if (opcode == CS2Instruction.PUSH_STRING) {
                if (!scrambled.readString().equals(buffer.readString())) {
//                   throw new RuntimeException("Script different");
                }
            } else if (opcode == CS2Instruction.PUSH_LONG) {
                long i = buffer.readLong();
                long j = scrambled.readLong();
                if (i != j) {
                    return;
                }
            } /*else if (opcode == Opcodes.SWITCH) { // switch
                buffer.readInt();
                scrambled.readInt();
            }*/ else if (opcode == CS2Instruction.RETURN || opcode == CS2Instruction.POP_INT || opcode == CS2Instruction.POP_STRING || opcode.ordinal() >= 150) {
                int i = buffer.readUnsignedByte();
                int j = scrambled.readUnsignedByte();
                if (i != j) {
                    return;
                }
            }/* else if (opcode == Opcodes.GOTO || opcode == Opcodes.INT_NE || opcode == Opcodes.INT_EQ || opcode == Opcodes.INT_LT || opcode == Opcodes.INT_GT || opcode == Opcodes.INT_LE || opcode == Opcodes.INT_GE || opcode == Opcodes.LONG_NE || opcode == Opcodes.LONG_EQ || opcode == Opcodes.LONG_LT || opcode == Opcodes.LONG_GT || opcode == Opcodes.LONG_LE || opcode == Opcodes.LONG_GE || opcode == Opcodes.EQ1 || opcode == Opcodes.EQ0) {
                buffer.readInt();
                scrambled.readInt();
            }*/ else {
                int i = buffer.readInt();
                int j = scrambled.readInt();
                if (i != j) {
                    return;
                }
            }
            s.put(opcode, scrambledOp);
            r.put(scrambledOp, opcode);
//            if (unsure.contains(opcode)) {
//
//            }
//            if (unscrambled.containsKey(opcode)) {
//                if (unscrambled.get(opcode) != scrambledOp) {
//                    System.err.println("Duplicate scrambling found!!!");
//                }
//            } else if (unscrambled.containsValue(scrambledOp)) {
//                System.err.println("Duplicate value " + opcode + " & .. -> " + scrambledOp);
//            } else {
//                unscrambled.put(opcode, scrambledOp);
//            }
        }
        scramble.putAll(s);
        unscramble.putAll(r);
//        System.out.println("Script ok");
    }

}

