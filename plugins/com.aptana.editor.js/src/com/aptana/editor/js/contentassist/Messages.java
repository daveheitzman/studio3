/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.editor.js.contentassist.messages"; //$NON-NLS-1$
	public static String JSContentAssistProcessor_KeywordDescription;
	public static String JSContentAssistProcessor_KeywordLocation;
	public static String JSModelFormatter_Defined_Section_Header;
	public static String JSModelFormatter_Exampes_Section_Header;
	public static String JSModelFormatter_Specification_Header;
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
