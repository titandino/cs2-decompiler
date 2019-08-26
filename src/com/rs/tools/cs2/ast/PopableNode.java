package com.rs.tools.cs2.ast;

import com.rs.tools.cs2.CodePrinter;

public class PopableNode extends AbstractCodeNode {

    private ExpressionNode expression;

    public PopableNode(ExpressionNode expression) {
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
        if (expression != null)
            expression.print(printer);
        printer.print(';');
    }


}
