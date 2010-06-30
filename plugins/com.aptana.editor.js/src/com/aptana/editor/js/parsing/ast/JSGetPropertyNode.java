package com.aptana.editor.js.parsing.ast;

import beaver.Symbol;

import com.aptana.editor.js.contentassist.LocationType;
import com.aptana.parsing.ast.IParseNode;

public class JSGetPropertyNode extends JSBinaryOperatorNode
{
	/**
	 * JSGetPropertyOperatorNode
	 * 
	 * @param left
	 * @param right
	 */
	public JSGetPropertyNode(JSNode left, Symbol operator, JSNode right)
	{
		super(left, operator, right);
		
		this.setType(JSNodeTypes.GET_PROPERTY);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSNode#accept(com.aptana.editor.js.parsing.ast.JSTreeWalker)
	 */
	@Override
	public void accept(JSTreeWalker walker)
	{
		walker.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSNode#getLocationType(int)
	 */
	@Override
	LocationType getLocationType(int offset)
	{
		LocationType result = LocationType.UNKNOWN;

		if (this.contains(offset))
		{
			IParseNode lhs = this.getLeftHandSide();
			Symbol operator = this.getOperator();

			if (lhs.contains(offset) || lhs.getEndingOffset() <= offset && offset < operator.getStart())
			{
				result = ((JSNode) lhs).getLocationType(offset);
			}
			else
			{
				result = LocationType.IN_PROPERTY_NAME;
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSBinaryOperatorNode#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder text = new StringBuilder();

		text.append(this.getLeftHandSide());
		text.append("."); //$NON-NLS-1$
		text.append(this.getRightHandSide());

		this.appendSemicolon(text);

		return text.toString();
	}
}