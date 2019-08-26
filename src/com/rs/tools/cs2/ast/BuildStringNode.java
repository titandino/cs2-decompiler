package com.rs.tools.cs2.ast;

import java.util.ArrayList;
import java.util.List;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;

public class BuildStringNode extends ExpressionNode {

    public final List<ExpressionNode> arguments;


    public BuildStringNode(List<ExpressionNode> arguments) {
        this.arguments = arguments;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.STRING;
    }

    @Override
    public int getPriority() {
        return PRIORITY_ADDSUB;
    }

    @Override
    public ExpressionNode copy() {
        List<ExpressionNode> argsCopy = new ArrayList<>(arguments.size());
        for (ExpressionNode argument : arguments) argsCopy.add(argument.copy());
        return new BuildStringNode(argsCopy);
    }

    @Override
    public void print(CodePrinter printer) {
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                printer.print(" + ");
            }
            if (arguments.get(i) instanceof CallExpressionNode) {
                CallExpressionNode call = (CallExpressionNode) arguments.get(i);
                //string conversion call, omit this, compiler can do this
                if (!call.info.isScript /*&& call.info.id == 4106*/) {
                    assert call.arguments.length == 1;
                    boolean paren = call.arguments[0].getPriority() >= getPriority();
                    if (paren) {
                        printer.print('(');
                    }
                    call.arguments[0].print(printer);
                    if (paren) {
                        printer.print(')');
                    }

                    continue;
                }
            }
            arguments.get(i).print(printer);
//            if ((i + 1) < arguments.size())
//                printer.print(" + ");
        }
    }

}
