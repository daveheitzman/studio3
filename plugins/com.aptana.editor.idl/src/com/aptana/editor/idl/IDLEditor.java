/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.idl;

import org.eclipse.jface.preference.IPreferenceStore;

import com.aptana.editor.common.AbstractThemeableEditor;

public class IDLEditor extends AbstractThemeableEditor
{

	@Override
	protected void initializeEditor()
	{
		super.initializeEditor();

		this.setSourceViewerConfiguration(new IDLSourceViewerConfiguration(this.getPreferenceStore(), this));
		this.setDocumentProvider(IDLPlugin.getDefault().getIDLDocumentProvider());
	}

	@Override
	protected IPreferenceStore getPluginPreferenceStore()
	{
		return IDLPlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected String getFileServiceContentTypeId()
	{
		return IIDLConstants.CONTENT_TYPE_IDL;
	}
}
