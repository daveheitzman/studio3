package com.aptana.syncing.core.tests;

import java.io.File;

import org.eclipse.core.runtime.Path;

import com.aptana.filesystem.ftp.FTPConnectionPoint;
import com.aptana.ide.core.io.LocalConnectionPoint;

public class FTPLargeSampleSyncingTests extends LargeSampleSyncingTests
{

	@Override
	protected void setUp() throws Exception
	{
		File baseTempFile = File.createTempFile("test", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		baseTempFile.deleteOnExit();
		
		File baseDirectory = baseTempFile.getParentFile();
		
		LocalConnectionPoint lcp = new LocalConnectionPoint();
		lcp.setPath(new Path(baseDirectory.getAbsolutePath()));
		clientManager = lcp;

		FTPConnectionPoint ftpcp = new FTPConnectionPoint();
		ftpcp.setHost(getConfig().getProperty("ftp.host", "10.10.1.60")); //$NON-NLS-1$ //$NON-NLS-2$
		ftpcp.setLogin(getConfig().getProperty("ftp.username", "ftpuser")); //$NON-NLS-1$ //$NON-NLS-2$
		ftpcp.setPassword(getConfig().getProperty("ftp.password",	//$NON-NLS-1$
				String.valueOf(new char[] { 'l', 'e', 't', 'm', 'e', 'i', 'n'})).toCharArray());
		serverManager = ftpcp;

		super.setUp();		
	}

	@Override
	protected void tearDown() throws Exception
	{
		// TODO Auto-generated method stub
		super.tearDown();
	}

}