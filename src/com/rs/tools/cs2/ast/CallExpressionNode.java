package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;
import com.rs.tools.cs2.util.FunctionInfo;

public class CallExpressionNode extends ExpressionNode {

    public FunctionInfo info;
    public ExpressionNode[] arguments;
    public boolean invokeOnLastArg;


    public CallExpressionNode(FunctionInfo info, ExpressionNode[] arguments, boolean invokeOnLastArg) {
        this.info = info;
        this.arguments = arguments;
        this.invokeOnLastArg = invokeOnLastArg;
    }

    @Override
    public CS2Type getType() {
        return info.getReturnType();
    }

    @Override
    public CallExpressionNode copy() {
        ExpressionNode[] argsCopy = new ExpressionNode[arguments.length];
        for (int i = 0; i < arguments.length; i++)
            argsCopy[i] = arguments[i].copy();
        return new CallExpressionNode(this.info, argsCopy, invokeOnLastArg);
    }

    @Override
    public void print(CodePrinter printer) {
        if (invokeOnLastArg) {
            arguments[arguments.length - 1].print(printer);
            printer.print(".");
        }
        printer.print(info.getName());
        printer.print('(');
        int max = arguments.length - (invokeOnLastArg ? 1 : 0);
        for (int i = 0; i < max; i++) {
            if (arguments[i] != null) { //this argument is fulfilled by another argument that is actually multiple return values
                arguments[i].print(printer);
            } else {
//                printer.print("/* <-- multiple values */ ");
            }
            if ((i + 1) < max && arguments[i + 1] != null)
                printer.print(", ");
        }
        printer.print(')');
    }

}
