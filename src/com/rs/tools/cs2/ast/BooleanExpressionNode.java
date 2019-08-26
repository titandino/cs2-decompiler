package com.rs.tools.cs2.ast;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.CodePrinter;

public class BooleanExpressionNode extends ExpressionNode implements IIntConstantNode {

    private boolean data;
    
    public BooleanExpressionNode(boolean data) {
    	this.data = data;
    }
    
    public boolean getData() {
    	return data;
    }

    @Override
    public CS2Type getType() {
    	return CS2Type.BOOLEAN;
    }

	@Override
	public ExpressionNode copy() {
		return new BooleanExpressionNode(data);
	}

	@Override
	public Integer getConst() {
		return this.data ? 1 : 0;
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print("" + this.data + "");
	}



}
