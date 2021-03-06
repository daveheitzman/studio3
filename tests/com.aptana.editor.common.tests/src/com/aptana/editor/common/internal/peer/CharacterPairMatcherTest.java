/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.internal.peer;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

public class CharacterPairMatcherTest extends TestCase
{
	private static final char[] pairs = new char[] { '(', ')', '{', '}', '[', ']', '`', '`', '\'', '\'', '"', '"' };
	private ICharacterPairMatcher matcher;

	@Override
	protected void setUp() throws Exception
	{
		matcher = new CharacterPairMatcher(pairs)
		{
			protected String getScopeAtOffset(IDocument doc, int charOffset) throws BadLocationException
			{
				return "source.ruby";
			};
		};
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception
	{
		if (matcher != null)
		{
			matcher.dispose();
		}
		matcher = null;
		super.tearDown();
	}

	public void testPairMatching()
	{
		String source = "( { [ `ruby command`, 'single quoted string', \"double quoted string\" ] } )";
		IDocument document = new Document(source);
		assertMatch(document, source, 0); // ()
		assertMatch(document, source, 2); // {}
		assertMatch(document, source, 4); // []
		assertMatch(document, source, 6, 19); // ``
		assertMatch(document, source, 22, 43); // ''
		assertMatch(document, source, 46, 67); // ""
	}

	public void testPairMatching2()
	{
		String source = "<?xml version=\"1.0\"\n encoding=\"ISO-8859-1\"?>";
		IDocument document = new Document(source);
		assertMatch(document, source, 14, 18);
		assertMatch(document, source, 30, 41);
	}

	public void testMatchesCharToRightIfNothingOnLeft()
	{
		String source = "( )";
		IDocument document = new Document(source);
		assertRawMatch(document, 0, 2, 0, 3);
	}

	public void testMatchesCharToRightIfNothingOnLeft2()
	{
		String source = "( { [ `ruby command`, 'single quoted string', \"double quoted string\" ] } )";
		IDocument document = new Document(source);
		assertRawMatch(document, 0, source.length() - 1, 0, source.length()); // ()
		assertRawMatch(document, 2, source.length() - 3, 2, source.length() - 4); // {}
		assertRawMatch(document, 4, source.length() - 5, 4, source.length() - 8); // []
		assertRawMatch(document, 6, 19, 6, 14); // ``
		assertRawMatch(document, 22, 43, 22, 22); // ''
		assertRawMatch(document, 46, 67, 46, 22); // ""
	}

	public void testDoesntPairMatchInComments()
	{
		String source = "# ( { [ `ruby command`, 'single quoted string', \"double quoted string\" ] } )";
		IDocument document = new Document(source);
		matcher = new CharacterPairMatcher(pairs)
		{
			protected String getScopeAtOffset(IDocument doc, int charOffset)
					throws org.eclipse.jface.text.BadLocationException
			{
				return "source.ruby comment.line.hash";
			};
		};
		assertNull(matcher.match(document, 2));
		assertNull(matcher.match(document, 4));
		assertNull(matcher.match(document, 6));
		assertNull(matcher.match(document, 8));
		assertNull(matcher.match(document, 24));
		assertNull(matcher.match(document, 48));
	}

	public void testSkipsPairsInComments()
	{
		String source = "(\n# )\n)";
		IDocument document = new Document(source);
		matcher = new CharacterPairMatcher(pairs)
		{
			protected String getScopeAtOffset(IDocument doc, int charOffset) throws BadLocationException
			{
				if (charOffset >= 2 && charOffset <= 5)
					return "source.ruby comment.line.hash";
				return "source.ruby";
			};
		};
		IRegion region = matcher.match(document, 0);
		assertNotNull(region);
		assertEquals("offset", 0, region.getOffset());
		assertEquals("length", 7, region.getLength());
		assertEquals(ICharacterPairMatcher.LEFT, matcher.getAnchor());
		assertNull(matcher.match(document, 4));
	}

	private void assertRawMatch(IDocument document, int leftOffsetToMatch, int rightOffsetToMatch, int offset,
			int length)
	{
		// left
		IRegion region = matcher.match(document, leftOffsetToMatch);
		assertNotNull(region);
		assertEquals("offset", offset, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.LEFT, matcher.getAnchor());
		// right
		region = matcher.match(document, rightOffsetToMatch);
		assertNotNull(region);
		assertEquals("offset", offset, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.RIGHT, matcher.getAnchor());
	}

	private void assertMatch(IDocument document, String source, int i)
	{
		int j = source.length() - i - 1;
		assertMatch(document, source, i, j);
	}

	private void assertMatch(IDocument document, String source, int i, int j)
	{
		int length = (j - i) + 1;
		// left
		IRegion region = matcher.match(document, i + 1);
		assertNotNull(region);
		assertEquals("offset", i, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.LEFT, matcher.getAnchor());
		// right
		region = matcher.match(document, j + 1);
		assertNotNull(region);
		assertEquals("offset", i, region.getOffset());
		assertEquals("length", length, region.getLength());
		assertEquals(ICharacterPairMatcher.RIGHT, matcher.getAnchor());
	}
}
