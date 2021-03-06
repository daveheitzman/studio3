/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.scripting.snippets;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;

import com.aptana.scripting.model.SnippetElement;

public class SnippetTemplateProposalTest extends TestCase
{

	public void testReplacesCorrectRegionWhenPortionOfFullPrefixMatches()
	{
		Mockery context = new Mockery()
		{
			{
				setImposteriser(ClassImposteriser.INSTANCE);
			}
		};
		// Create the snippet we want to apply
		SnippetElement se = new SnippetElement("");
		se.setDisplayName("something");
		se.setExpansion("Yahoo!");
		SnippetTemplate template = new SnippetTemplate(se, "echo", "whatever");

		// Set up the document we're operating on
		final IDocument document = new Document("<div>echo\n");
		final ITextViewer viewer = context.mock(ITextViewer.class);

		context.checking(new Expectations()
		{
			{
				oneOf(viewer).getDocument();
				will(returnValue(document));
			}
		});

		// Create snippet proposal, then apply it to the document
		DocumentSnippetTemplateContext tc = new DocumentSnippetTemplateContext(new SnippetTemplateContextType("scope"),
				document, 0, 9);
		SnippetTemplateProposal p = new SnippetTemplateProposal(template, tc, new Region(0, 0), null, 0);
		p.apply(viewer, '\t', 0, 9);

		// Now make sure the snippet got applied correctly
		assertEquals("<div>Yahoo!\n", document.get());
		context.assertIsSatisfied();
	}

	public void testAPSTUD2445()
	{
		Mockery context = new Mockery()
		{
			{
				setImposteriser(ClassImposteriser.INSTANCE);
			}
		};
		// Create the snippet we want to apply
		SnippetElement se = new SnippetElement("/some/fake/path.rb");
		se.setDisplayName(".add");
		se.setExpansion(".add('selector')");
		SnippetTemplate template = new SnippetTemplate(se, ".add", "source.js");

		// Set up the document we're operating on
		final IDocument document = new Document("$(selector).add");
		final ITextViewer viewer = context.mock(ITextViewer.class);

		context.checking(new Expectations()
		{
			{
				oneOf(viewer).getDocument();
				will(returnValue(document));
			}
		});

		// Create snippet proposal, then apply it to the document
		DocumentSnippetTemplateContext tc = new DocumentSnippetTemplateContext(new SnippetTemplateContextType(
				"source.js"), document, 0, 15);
		SnippetTemplateProposal p = new SnippetTemplateProposal(template, tc, new Region(0, 15), null, 0);

		// Make sure the snippet validates
		DocumentEvent event = new DocumentEvent(document, 15, 0, "");
		assertTrue("Snippet proposal incorrectly failed validation!", p.validate(document, 15, event));

		// Now make sure the snippet gets applied correctly
		p.apply(viewer, '\t', 0, 15);
		assertEquals("$(selector).add('selector')", document.get());
		context.assertIsSatisfied();
	}
}
