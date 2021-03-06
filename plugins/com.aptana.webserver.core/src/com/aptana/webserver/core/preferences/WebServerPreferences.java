/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.webserver.core.preferences;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.SocketUtil;
import com.aptana.webserver.core.WebServerCorePlugin;

/**
 * @author Max Stepanov
 *
 */
public class WebServerPreferences {

	private static final String PORTS_PATTERN = "^(\\d+)(-(\\d+))?$"; //$NON-NLS-1$
	
	private WebServerPreferences() {
	}

	/**
	 * Returns preferences-specified local webserver address
	 * 
	 * @return
	 */
	public static InetAddress getServerAddress() {
		IEclipsePreferences node = EclipseUtil.defaultScope().getNode(WebServerCorePlugin.PLUGIN_ID);
		String address = node.get(IWebServerPreferenceConstants.PREF_HTTP_SERVER_ADDRESS, null);
		for(InetAddress i : SocketUtil.getLocalAddresses()) {
			if(i.getHostAddress().equals(address)) {
				return i;
			}
		}
		try {
			return InetAddress.getByName(IWebServerPreferenceConstants.DEFAULT_HTTP_SERVER_ADDRESS);
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	/**
	 * Returns preferences-specified local webserver port range
	 * @return
	 */
	public static int[] getPortRange() {
		IEclipsePreferences node = EclipseUtil.defaultScope().getNode(WebServerCorePlugin.PLUGIN_ID);
		int portsStart = IWebServerPreferenceConstants.DEFAULT_HTTP_SERVER_PORTS_RANGE[0];
		int portsEnd = IWebServerPreferenceConstants.DEFAULT_HTTP_SERVER_PORTS_RANGE[1];
		String portsString = node.get(IWebServerPreferenceConstants.PREF_HTTP_SERVER_PORTS, null);
		if (portsString != null && portsString.length() > 0) {
			Matcher matcher = Pattern.compile(PORTS_PATTERN).matcher(portsString); 
			if (matcher.matches()) {
				try {
					int start = Integer.parseInt(matcher.group(1));
					int end = start;
					if ( matcher.group(2) != null ) {
						end = Integer.parseInt(matcher.group(3));
					}
					if ( start < end ) {
						portsStart = start;
						portsEnd = end;
					}
				} catch (NumberFormatException e) {
					IdeLog.logWarning(WebServerCorePlugin.getDefault(), e);
				}				
			}
		}
		return new int[] { portsStart, portsEnd };
	}

}
