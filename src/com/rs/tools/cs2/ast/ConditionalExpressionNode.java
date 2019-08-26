package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;
import com.rs.tools.cs2.util.CS2TypeUtil;

public class ConditionalExpressionNode extends ExpressionNode {

    /**
     * Contains left expression node.
     */
    private ExpressionNode left;
    /**
     * Contains right expression node.
     */
    private ExpressionNode right;
    public final Operator conditional;

    public ConditionalExpressionNode(ExpressionNode left, ExpressionNode right, Operator conditional) {
        this.left = left;
        this.right = right;
        this.conditional = conditional;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.BOOLEAN;
    }

    @Override
    public int getPriority() {
        return conditional.priority;
    }

    @Override
    public void print(CodePrinter printer) {
        if (left.getType() != right.getType()) {
            if (left.getType() != CS2Type.INT) {
                right = CS2TypeUtil.cast(right, left.getType());
            } else {
                left = CS2TypeUtil.cast(left, right.getType());
            }
        }

        boolean needsLeftParen = left.getPriority() > this.getPriority();
        boolean needsRightParen = right.getPriority() > this.getPriority();

        if (needsLeftParen)
            printer.print("(");


        this.left.print(printer);
        if (needsLeftParen)
            printer.print(")");
        printer.print(" " + this.conditional.text + " ");

        if (needsRightParen)
            printer.print("(");
        this.right.print(printer);

        if (needsRightParen)
            printer.print(")");
    }

    @Override
    public ExpressionNode copy() {
        return new ConditionalExpressionNode(left.copy(), right.copy(), conditional);
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

}
