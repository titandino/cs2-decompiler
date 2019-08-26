package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;
import com.rs.tools.cs2.util.TextUtils;

public class CharExpressionNode extends ExpressionNode implements IIntConstantNode {

    private char data;

    public CharExpressionNode(char data) {
        this.data = data;
    }

    public char getData() {
        return data;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.CHAR;
    }

    @Override
    public Integer getConst() {
        return (int) this.data;
    }

    @Override
    public ExpressionNode copy() {
        return new CharExpressionNode(this.data);
    }

    @Override
    public void print(CodePrinter printer) {
        String escaped = TextUtils.quote(data);
        if (escaped.length() == 3) {
            printer.print(escaped);
        } else {
            printer.print("" + (int) data);
        }
    }

}
