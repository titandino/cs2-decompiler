package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;

public class CallbackExpressionNode extends ExpressionNode {

    public final CallExpressionNode call;
    public final ExpressionList trigger;

    public CallbackExpressionNode(CallExpressionNode call, ExpressionList trigger) {
        this.call = call;
        this.trigger = trigger;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.CALLBACK;
    }

    @Override
    public ExpressionNode copy() {
        return new CallbackExpressionNode(call.copy(), trigger != null ? trigger.copy() : null);
    }

    @Override
    public void print(CodePrinter printer) {
        if (call == null) {
            printer.print("null");
        } else {
            printer.print("&");
            call.print(printer);
            if (trigger != null) {
                printer.print(", ");
                trigger.print(printer);
            }
        }
    }

}
