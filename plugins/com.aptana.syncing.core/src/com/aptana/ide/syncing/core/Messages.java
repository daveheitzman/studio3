/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.syncing.core;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 */
public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "com.aptana.ide.syncing.core.messages"; //$NON-NLS-1$

	public static String NaturePropertyTester_ERR_WhileTestingProjectNature;

	public static String SiteConnection_LBL_NoDestination;
	public static String SiteConnection_LBL_NoSource;

	public static String SiteConnectionManager_ERR_FailedToLoadConnections;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
