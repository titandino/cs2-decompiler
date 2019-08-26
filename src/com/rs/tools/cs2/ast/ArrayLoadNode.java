package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;

public class ArrayLoadNode extends ExpressionNode {


    public final int arrayId;
    private ExpressionNode index;
    private CS2Type type;

    public ArrayLoadNode(int arrayId, CS2Type type, ExpressionNode index) {
        this.arrayId = arrayId;
        this.type = type;
        this.index = index;
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    public ExpressionNode getIndex() {
        return index;
    }

    @Override
    public ExpressionNode copy() {
        return new ArrayLoadNode(arrayId, type, this.index.copy());
    }

    @Override
    public void print(CodePrinter printer) {
        printer.print("ARRAY" + arrayId + "[");
        index.print(printer);
        printer.print(']');
    }

}
