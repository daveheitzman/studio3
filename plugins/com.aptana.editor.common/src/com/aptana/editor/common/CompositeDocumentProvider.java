/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.common;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import com.aptana.editor.common.text.rules.CompositePartitionScanner;

/**
 * @author Max Stepanov
 *
 */
public class CompositeDocumentProvider extends TextFileDocumentProvider {

	private String documentContentType;
	private IPartitioningConfiguration defaultPartitioningConfiguration;
	private IPartitioningConfiguration primaryPartitioningConfiguration;
	private IPartitionerSwitchStrategy partitionerSwitchStrategy;
	
	/**
	 * @param documentContentType
	 * @param defaultPartitioningConfiguration
	 * @param primaryPartitioningConfiguration
	 * @param partitionerSwitchStrategy
	 */
	protected CompositeDocumentProvider(
			String documentContentType,
			IPartitioningConfiguration defaultPartitioningConfiguration,
			IPartitioningConfiguration primaryPartitioningConfiguration,
			IPartitionerSwitchStrategy partitionerSwitchStrategy) {
		super();
		this.documentContentType = documentContentType;
		this.defaultPartitioningConfiguration = defaultPartitioningConfiguration;
		this.primaryPartitioningConfiguration = primaryPartitioningConfiguration;
		this.partitionerSwitchStrategy = partitionerSwitchStrategy;
	}

	@Override
	public void connect(Object element) throws CoreException {
	    super.connect(element);

		IDocument document = getDocument(element);
		if (document != null) {			
			CompositePartitionScanner partitionScanner = new CompositePartitionScanner(
					defaultPartitioningConfiguration.createSubPartitionScanner(),
					primaryPartitioningConfiguration.createSubPartitionScanner(),
					partitionerSwitchStrategy);
			IDocumentPartitioner partitioner = new ExtendedFastPartitioner(partitionScanner,
					TextUtils.combine(new String[][] {
							CompositePartitionScanner.SWITCHING_CONTENT_TYPES,
							defaultPartitioningConfiguration.getContentTypes(),
							primaryPartitioningConfiguration.getContentTypes()
					}));
			partitionScanner.setPartitioner((IExtendedPartitioner) partitioner);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
			DocumentContentTypeManager.getInstance().setDocumentContentType(document, documentContentType);
			DocumentContentTypeManager.getInstance().registerConfigurations(document,
					new IPartitioningConfiguration[] { defaultPartitioningConfiguration, primaryPartitioningConfiguration });
		}
	}
}
