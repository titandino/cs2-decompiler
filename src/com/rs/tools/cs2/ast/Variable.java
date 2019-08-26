package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.instructions.AbstractInstruction;

public interface Variable {

    String getName();
    CS2Type getType();
    AbstractInstruction generateStoreInstruction();
    AbstractInstruction generateLoadInstruction();

}
