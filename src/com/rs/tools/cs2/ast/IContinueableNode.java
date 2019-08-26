package com.rs.tools.cs2.ast;


public interface IContinueableNode extends IControllableFlowNode {
	public FlowBlock getStart();
	public boolean canContinue();
}
