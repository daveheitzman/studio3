/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.parsing.ast;

/**
 * @author Kevin Lindsey
 */
public class ParseNodeAttribute implements IParseNodeAttribute
{
	private IParseNode _parent;
	private String _name;
	private String _value;

	/**
	 * ParseNodeAttribute
	 * 
	 * @param parent
	 * @param name
	 * @param value
	 */
	public ParseNodeAttribute(IParseNode parent, String name, String value)
	{
		if (parent == null)
		{
			throw new IllegalArgumentException(Messages.ParseNodeAttribute_Undefined_Parent);
		}
		if (name == null)
		{
			throw new IllegalArgumentException(Messages.ParseNodeAttribute_Undefined_Name);
		}
		if (value == null)
		{
			throw new IllegalArgumentException(Messages.ParseNodeAttribute_Undefined_Value);
		}

		this._parent = parent;
		this._name = name;
		this._value = value;
	}

	/**
	 * @see com.aptana.ide.parsing.nodes.IParseNodeAttribute#getName()
	 */
	public String getName()
	{
		return this._name;
	}

	/**
	 * @see com.aptana.ide.parsing.nodes.IParseNodeAttribute#getValue()
	 */
	public String getValue()
	{
		return this._value;
	}

	/**
	 * @see com.aptana.ide.parsing.nodes.IParseNodeAttribute#getParent()
	 */
	public IParseNode getParent()
	{
		return this._parent;
	}
}
