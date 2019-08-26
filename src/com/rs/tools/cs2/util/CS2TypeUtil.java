package com.rs.tools.cs2.util;

import java.util.ArrayList;
import java.util.List;

import com.rs.cache.loaders.cs2.CS2Type;
import com.rs.tools.cs2.DecompilerException;
import com.rs.tools.cs2.ast.BooleanExpressionNode;
import com.rs.tools.cs2.ast.CastNode;
import com.rs.tools.cs2.ast.CharExpressionNode;
import com.rs.tools.cs2.ast.ExpressionNode;
import com.rs.tools.cs2.ast.IntExpressionNode;
import com.rs.tools.cs2.ast.NewColorNode;
import com.rs.tools.cs2.ast.NewLocationNode;
import com.rs.tools.cs2.ast.NewWidgetPointerNode;
import com.rs.tools.cs2.ast.NullableIntExpressionNode;
import com.rs.tools.cs2.ast.PlaceholderValueNode;

public class CS2TypeUtil {
	
	 public static CS2Type typeFor(List<ExpressionNode> list) {
	        List<CS2Type> result = new ArrayList<>();
	        for (ExpressionNode n : list) {
	            result.addAll(n.getType().composite);
	        }
	        if (result.size() == 1) {
	            return result.get(0);
	        }

	        return CS2Type.of(result);
	    }

	    /**
	     * Casts expression node to specific type.
	     * If expression type is same then returned value is expr,
	     * otherwise on most cases CastExpressionNode is returned with one child
	     * which is expr.
	     */
	    public static ExpressionNode cast(ExpressionNode expr, CS2Type type) {
	        if (expr.getType().equals(type))
	            return expr;
	        if (expr instanceof PlaceholderValueNode) {
	            throw new DecompilerException("Can't cast placeholder values!");
	        }

	        if (type.equals(CS2Type.LOCATION))
	            return new NewLocationNode(expr);
	        if (type.equals(CS2Type.ICOMPONENT))
	            return new NewWidgetPointerNode(expr);
	        if (type.equals(CS2Type.COLOR))
	            return new NewColorNode(expr instanceof NewWidgetPointerNode ? ((NewWidgetPointerNode) expr).getExpression() : expr);
	        if (type.equals(CS2Type.BOOLEAN) && expr instanceof IntExpressionNode) {
	            int val = ((IntExpressionNode) expr).getData();
	            if (val > 1 || val < -1) {
	                throw new DecompilerException("Cannot cast to boolean " + val);
	            }
	            if (val == -1) {
	                System.err.println("warning null boolean");
	                return new NullableIntExpressionNode(-1);
//	                throw new DecompilerException("-1 boolean?");
	            }
	            return new BooleanExpressionNode(val != 0); //not 1 is truthy //TODO: -1 is NULL? not 0 or 1 is faulty?
	        }
	        if (type.equals(CS2Type.CHAR) && expr instanceof IntExpressionNode)
	            return new CharExpressionNode((char) ((IntExpressionNode) expr).getData());

	        if ((expr.getType() == CS2Type.BOOLEAN || expr.getType() == CS2Type.COLOR) && type == CS2Type.INT) {
	            //allow implicit conversion from these types to int
	            return expr;
	        }
	        //Implicit coercion for this, but -1 represents nulls
	        if (expr instanceof IntExpressionNode && type.isCompatible(CS2Type.INT)) {//(type.equals(SPRITE) || type.equals(FONTMETRICS) || type.equals(ITEM) || type.equals(MODEL) || type.equals(MIDI) || type.equals(CONTAINER) || type.equals(IDENTIKIT) || type.equals(ANIM) || type.equals(MAPID) || type.equals(GRAPHIC) || type.equals(SKILL) || type.equals(NPCDEF) || type.equals(QCPHRASE) || type.equals(CHATCAT) || type.equals(TEXTURE) || type.equals(STANCE) || type.equals(SPELL) || type.equals(CATEGORY) || type.equals(SOUNDEFFECT))) {
	            return new NullableIntExpressionNode(((IntExpressionNode) expr).getData());
	        }
	        if (type.isCompatible(expr.getType())) {
	            return new CastNode(type, expr);
	        }

	        throw new DecompilerException("Incompatible cast " + expr.getType() + " to " + type);
	    }

}
