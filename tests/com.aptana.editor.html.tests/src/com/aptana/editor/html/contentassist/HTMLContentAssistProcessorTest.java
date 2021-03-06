/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.contentassist;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.contentassist.LexemeProvider;
import com.aptana.editor.common.tests.util.AssertUtil;
import com.aptana.editor.common.tests.util.TestProject;
import com.aptana.editor.html.BadDocument;
import com.aptana.editor.html.HTMLMetadataLoader;
import com.aptana.editor.html.HTMLTestUtil;
import com.aptana.editor.html.contentassist.HTMLContentAssistProcessor.LocationType;
import com.aptana.editor.html.parsing.lexer.HTMLTokenType;
import com.aptana.editor.html.tests.HTMLEditorBasedTests;
import com.aptana.projects.WebProjectNature;
import com.aptana.webserver.core.EFSWebServerConfiguration;
import com.aptana.webserver.core.WebServerCorePlugin;

public class HTMLContentAssistProcessorTest extends HTMLEditorBasedTests
{

	private static final int ELEMENT_PROPOSALS_COUNT = 133;
	private static final int DOCTYPE_PROPOSALS_COUNT = 11;
	private static final int CLOSE_TAG_PROPOSALS_COUNT = 120;
	private static final int ENTITY_PROPOSAL_COUNT = 252;

	private HTMLContentAssistProcessor fProcessor;
	private IDocument fDocument;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		HTMLMetadataLoader loader = new HTMLMetadataLoader();
		loader.schedule();
		loader.join();

		fProcessor = new HTMLContentAssistProcessor(null);

	}

	@Override
	protected void tearDown() throws Exception
	{
		fProcessor = null;
		fDocument = null;

		super.tearDown();
	}

	public void testGetContextInformationValidator()
	{
		IContextInformationValidator validator = fProcessor.getContextInformationValidator();
		assertNotNull(validator);
		// should be the same object as we are caching them
		assertEquals(validator, fProcessor.getContextInformationValidator());
	}

	public void testEmptyDocument()
	{
		assertCompletionCorrect("|", '\t', 0, null, StringUtil.EMPTY, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testLinkProposal()
	{
		assertCompletionCorrect("<|", '\t', ELEMENT_PROPOSALS_COUNT, "a", "<a></a>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal1()
	{
		assertCompletionCorrect("<!|", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal2()
	{
		assertCompletionCorrect("<!D|", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal3()
	{
		assertCompletionCorrect(
				"<!D| html>", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE html>", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal4()
	{
		assertCompletionCorrect("<|>", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal5()
	{
		assertCompletionCorrect("<!|>", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal6()
	{
		assertCompletionCorrect(
				"<!D|OCTYP >", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEProposal7()
	{
		assertCompletionCorrect(
				"<!D|OCTYPE >", '\t', ELEMENT_PROPOSALS_COUNT, "!DOCTYPE", "<!DOCTYPE >", new Point(10, 0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEValueReplacement1()
	{
		assertCompletionCorrect("<!DOCTYPE |html>", '\t', DOCTYPE_PROPOSALS_COUNT, "HTML 5", "<!DOCTYPE HTML>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDOCTYPEValueReplacement2()
	{
		assertCompletionCorrect(
				"<!DOCTYPE |html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"\n	\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">", //$NON-NLS-1$
				'\t', DOCTYPE_PROPOSALS_COUNT, "HTML 5", "<!DOCTYPE HTML>", null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testABBRProposal1()
	{
		assertCompletionCorrect("<a|>", '\t', ELEMENT_PROPOSALS_COUNT, "abbr", "<abbr></abbr>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testABBRProposal2()
	{
		assertCompletionCorrect("<A|>", '\t', ELEMENT_PROPOSALS_COUNT, "abbr", "<abbr></abbr>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal()
	{
		assertCompletionCorrect("<|></a>", '\t', ELEMENT_PROPOSALS_COUNT, "a", "<a></a>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal2()
	{
		assertCompletionCorrect("<|></a>", '\t', ELEMENT_PROPOSALS_COUNT, "abbr", "<abbr></abbr></a>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal3()
	{
		assertCompletionCorrect("<|</a>", '\t', ELEMENT_PROPOSALS_COUNT, "abbr", "<abbr></abbr></a>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal4()
	{
		assertCompletionCorrect("<b><a><|</b>", '\t', ELEMENT_PROPOSALS_COUNT + 1, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal5()
	{
		assertCompletionCorrect("<b><a><|></b>", '\t', ELEMENT_PROPOSALS_COUNT + 1, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testElementWhichIsClosedProposal6()
	{
		assertCompletionCorrect("<b><a></|</b>", '\t', 1, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testSuggestOnlyUnclosedTagForCloseTagWithNoElementName()
	{
		assertCompletionCorrect("<b><a></|></b>", '\t', 1, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testSuggestOnlyUnclosedTagForCloseTagWithElementName()
	{
		assertCompletionCorrect("<b><a></a|</b>", '\t', 1, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testSuggestAllPossibleCloseTagsOnExistingCloseTagRegardlessOfPrefix()
	{
		assertCompletionCorrect("<b><a></a|></b>", '\t', CLOSE_TAG_PROPOSALS_COUNT, "/a", "<b><a></a></b>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testCloseTagWithNoUnclosedTagsProposal()
	{
		assertCompletionCorrect("</|>", '\t', CLOSE_TAG_PROPOSALS_COUNT, "/ul", "</ul>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testNoSuggestionsInTextAreaBetweenTags()
	{
		assertCompletionCorrect("<p>|</p>", '\t', 0, null, "<p></p>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testNoSuggestionsInTextAreaWithWhitespaceBetweenTags()
	{
		assertCompletionCorrect("<p>\n  |\n</p>", '\t', 0, null, "<p>\n  \n</p>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testOnlySuggestEntityIfPrecededByAmpersand()
	{
		assertCompletionCorrect("<p>\n  &|\n</p>", '\t', ENTITY_PROPOSAL_COUNT, "&amp;", "<p>\n  &amp;\n</p>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testEntitySuggestionWithNoSurroundingWhitespace()
	{
		assertCompletionCorrect("<div>&|</div>", '\t', ENTITY_PROPOSAL_COUNT, "&amp;", "<div>&amp;</div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testExistingEntityGetsFullyReplaced1()
	{
		assertCompletionCorrect(
				"<body>\n  &a|acute;\n</body>", '\t', ENTITY_PROPOSAL_COUNT, "&acirc;", "<body>\n  &acirc;\n</body>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testExistingEntityGetsFullyReplaced2()
	{
		assertCompletionCorrect("<div>&a|acute;</div>", '\t', ENTITY_PROPOSAL_COUNT, "&amp;", "<div>&amp;</div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testProposalsBadLocation()
	{
		String document = "<body>&|";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createBadDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		// offset is outside document size
		((BadDocument) fDocument).setThrowBadLocation(true);
		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
	}

	public void testIMGProposal()
	{
		assertCompletionCorrect("<|", '\t', ELEMENT_PROPOSALS_COUNT, "img", "<img />", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testHTML5DoctypeProposal()
	{
		assertCompletionCorrect("<!doctype |>", '\t', DOCTYPE_PROPOSALS_COUNT, "HTML 5", "<!doctype HTML>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testHTML401StrictDoctypeProposal()
	{
		assertCompletionCorrect("<!DOCTYPE |", '\t', DOCTYPE_PROPOSALS_COUNT, "HTML 4.01 Strict", //$NON-NLS-1$ //$NON-NLS-2$
				"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\n\"http://www.w3.org/TR/html4/strict.dtd\"", null); //$NON-NLS-1$
	}

	public void testStyleAttributeProposalWithNoPrefix()
	{
		assertCompletionCorrect("<div |></div>", '\t', 64, "style", "<div style=\"\"></div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testStyleAttributeProposalOnSelfClosingTag()
	{
		assertCompletionCorrect("<br |/>", '\t', 8, "style", "<br style=\"\"/>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testStyleAttributeProposalOnSelfClosingTagWithTrailingSpaceAfterCursor()
	{
		assertCompletionCorrect("<br | />", '\t', 8, "style", "<br style=\"\" />", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testStyleAttributeProposalWithPrefix()
	{
		assertCompletionCorrect("<div sty|></div>", '\t', 64, "style", "<div style=\"\"></div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testStyleAttributeProposalWithPrefixAndTrailingEquals()
	{
		assertCompletionCorrect("<div sty|=\"\"></div>", '\t', 64, "style", "<div style=\"\"></div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDontOverwriteTagEnd1()
	{
		assertCompletionCorrect("<div dir=\"|></div>", '\t', 2, "ltr", "<div dir=\"ltr></div>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testDontOverwriteTagEnd2()
	{
		assertCompletionCorrect("<br dir=\"|/>", '\t', 2, "ltr", "<br dir=\"ltr/>", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@SuppressWarnings("rawtypes")
	public void testStyleAttributeProposalHasExitTabstopAfterQuotes()
	{
		assertCompletionCorrect("<div |>", '\t', 64, "style", "<div style=\"\">", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Now test that we have two tabstops. One inside the quotes, one just after
		LinkedModeModel model = LinkedModeModel.getModel(document, 12);
		assertNotNull(model);
		List list = model.getTabStopSequence();
		assertNotNull(list);
		assertEquals(2, list.size());
		Position pos = (Position) list.get(0);
		assertEquals(12, pos.getOffset());
		assertEquals(0, pos.getLength());

		pos = (Position) list.get(1);
		assertEquals(13, pos.getOffset());
		assertEquals(0, pos.getLength());
	}

	@SuppressWarnings("rawtypes")
	public void testEventProposalHasExitTabstopAfterQuotes()
	{
		assertCompletionCorrect("<div |>", '\t', 64, "onmouseenter", "<div onmouseenter=\"\">", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Now test that we have two tabstops. One inside the quotes, one just after
		LinkedModeModel model = LinkedModeModel.getModel(document, 19);
		assertNotNull(model);
		List list = model.getTabStopSequence();
		assertNotNull(list);
		assertEquals(2, list.size());
		Position pos = (Position) list.get(0);
		assertEquals(19, pos.getOffset());
		assertEquals(0, pos.getLength());

		pos = (Position) list.get(1);
		assertEquals(20, pos.getOffset());
		assertEquals(0, pos.getLength());
	}

	// https://aptana.lighthouseapp.com/projects/35272/tickets/1719-html-code-completion-for-tag-attributes-goes-wrong
	public void testIMGSrcContentAssistDoesntAutoInsertCloseTag()
	{
		String document = "<div><img src='|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(0, proposals.length);
	}

	// TODO Add tests for src/href folder/filepath proposals
	public void testLinkHREFFolderProposal() throws Exception
	{
		assertHREFProposal("<link rel=\"stylesheet\" href=\"|\" />", "<link rel=\"stylesheet\" href=\"folder/\" />",
				"folder/");
	}

	public void testLinkHREFFileProposal() throws Exception
	{
		assertHREFProposal("<link rel='stylesheet' href='|' />", "<link rel='stylesheet' href='root.css' />",
				"root.css");

	}

	public void testLinkHREFFileUnderFolderProposal() throws Exception
	{
		assertHREFProposal("<link rel='stylesheet' href='folder/|' />",
				"<link rel='stylesheet' href='folder/inside_folder.css' />", "inside_folder.css");

	}

	public void testLinkHREFFileProposalWithPrefix() throws Exception
	{
		assertHREFProposal("<link rel='stylesheet' href='roo|' />", "<link rel='stylesheet' href='root.css' />",
				"root.css");

	}

	public void testLinkHREFFolderProposalWithPrefix() throws Exception
	{
		assertHREFProposal("<link rel='stylesheet' href='fo|' />", "<link rel='stylesheet' href='folder/' />",
				"folder/");

	}

	public void testLinkHREFFileInsideFolderProposalWithPrefix() throws Exception
	{
		assertHREFProposal("<link rel='stylesheet' href='folder/in|' />",
				"<link rel='stylesheet' href='folder/inside_folder.css' />", "inside_folder.css");
	}

	public void testApstud2959() throws Exception
	{
		String document = "<a href='http://|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		TestProject project = new TestProject("Apstud2959", new String[] { WebProjectNature.ID });
		final IFile file = project.createFile("testApstud2959", "");
		project.createFolder("email");

		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				return file.getRawLocationURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(0, proposals.length);

		project.delete();
	}

	public void testRootFileSystemForHREF() throws Exception
	{
		String document = "<a href='file://|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		IFileStore root = EFS.getLocalFileSystem().getStore(Path.ROOT);
		String[] names = root.childNames(EFS.NONE, null);
		int count = 0;
		for (String name : names)
		{
			if (name.startsWith("."))
			{
				continue;
			}
			count++;
		}
		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				// Shouldn't be necessary...
				return new File(System.getProperty("java.io.tmpdir"), "file.html").toURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(count, proposals.length);

	}

	public void testRootFileSystemForHREFBadPrefix() throws Exception
	{
		// Busted scheme
		String document = "<a href='file:/|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				// Shouldn't be necessary...
				return new File(System.getProperty("java.io.tmpdir"), "file.html").toURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(0, proposals.length);

	}

	public void testServerHREF() throws Exception
	{
		// Generate some folders/files to use as proposals
		TestProject project = createWebProject("href_file_proposal");
		final IFile file = project.createFile("test.html", "<link rel='stylesheet' href='/|' />");
		this.setupTestContext(file);

		EFSWebServerConfiguration server = new EFSWebServerConfiguration();
		server.setDocumentRoot(project.getURI());
		server.setBaseURL(new URL("http://www.test.com/"));
		WebServerCorePlugin.getDefault().getServerConfigurationManager().addServerConfiguration(server);

		int offset = this.cursorOffsets.get(0);
		ITextViewer viewer = AssertUtil.createTextViewer(document);
		ICompletionProposal[] proposals = processor.doComputeCompletionProposals(viewer, offset, '\t', false);

		AssertUtil.assertProposalFound("file.html", proposals);
		AssertUtil.assertProposalFound("root.css", proposals);
		AssertUtil.assertProposalFound("folder/", proposals);
		AssertUtil.assertProposalApplies("<link rel='stylesheet' href='/folder/' />", document, "folder/", proposals,
				offset, new Point(0, 0));

		WebServerCorePlugin.getDefault().getServerConfigurationManager().removeServerConfiguration(server);
		project.delete();

	}

	public void testRailsServerHREF() throws Exception
	{
		// Generate some folders/files to use as proposals
		TestProject project = createWebProject("href_file_proposal");
		project.createFolder("public");
		project.createFile("public/railsfile.html", "");
		final IFile file = project.createFile("test.html", "<link rel='stylesheet' href='/|' />");
		this.setupTestContext(file);

		int offset = this.cursorOffsets.get(0);
		ITextViewer viewer = AssertUtil.createTextViewer(document);
		ICompletionProposal[] proposals = processor.doComputeCompletionProposals(viewer, offset, '\t', false);

		AssertUtil.assertProposalFound("railsfile.html", proposals);
		AssertUtil.assertProposalApplies("<link rel='stylesheet' href='/railsfile.html' />", document,
				"railsfile.html", proposals, offset, new Point(0, 0));

		project.delete();

	}

	public void testRootFileSystemForHREFWithFolderPrefix() throws Exception
	{
		// Generate some folders/files to use as proposals
		TestProject project = new TestProject("href_file_proposal", new String[] { WebProjectNature.ID });
		final IFolder folder = project.createFolder("folder");

		String fileUri = folder.getRawLocationURI().toString();
		fileUri = fileUri.replace("file:/", "file://");
		String document = "<link rel='stylesheet' href='" + fileUri + "/|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		project.createFile("folder/inside_folder.css", "");

		final IFile file = project.createFile("file.html", "");

		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				return file.getRawLocationURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(1, proposals.length);
		AssertUtil.assertProposalFound("inside_folder.css", proposals);
		AssertUtil.assertProposalApplies("<link rel='stylesheet' href='" + fileUri + "/inside_folder.css' />",
				fDocument, "inside_folder.css", proposals, offset, new Point(0, 0));

		project.delete();
	}

	public void testRootFileSystemForHREFWithFolderAndFilePrefix() throws Exception
	{
		// Generate some folders/files to use as proposals
		TestProject project = new TestProject("href_file_proposal", new String[] { WebProjectNature.ID });
		final IFolder folder = project.createFolder("folder");

		String fileUri = folder.getLocationURI().toString();
		fileUri = fileUri.replace("file:/", "file://");
		String document = "<link rel='stylesheet' href='" + fileUri + "/inside|' />";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		project.createFile("folder/inside_folder.css", "");

		final IFile file = project.createFile("file.html", "");

		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				return file.getLocationURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertEquals(1, proposals.length);

		AssertUtil.assertProposalFound("inside_folder.css", proposals);
		AssertUtil.assertProposalApplies("<link rel='stylesheet' href='" + fileUri + "/inside_folder.css' />",
				fDocument, "inside_folder.css", proposals, offset, new Point(0, 0));

		project.delete();

	}

	private void assertHREFProposal(String source, String expected, String proposalToChoose) throws Exception
	{
		// Generate some folders/files to use as proposals
		TestProject project = createWebProject("href_file_proposal");
		final IResource file = project.createFile("test.html", source);
		IFileStore fileStore = EFS.getStore(file.getRawLocationURI());
		this.setupTestContext(fileStore);

		int offset = this.cursorOffsets.get(0);
		ITextViewer viewer = AssertUtil.createTextViewer(document);
		ICompletionProposal[] proposals = processor.doComputeCompletionProposals(viewer, offset, '\t', false);

		AssertUtil.assertProposalFound(proposalToChoose, proposals);
		AssertUtil.assertProposalApplies(expected, document, proposalToChoose, proposals, offset, new Point(0, 0));

		project.delete();
	}

	public void testAttributeAfterElementName()
	{
		String document = "<body s|></body>";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertTrue(proposals.length > 0);
		AssertUtil.assertProposalFound("scroll", proposals);
	}

	public void testAttributeValueProposals()
	{
		String document = "<li><a class=|</li>";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertTrue(proposals.length == 0);
	}

	public void testAttributeValueProposalsBeforeEquals()
	{
		String document = "<li><a clas|s=</li>";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertTrue(proposals.length > 0);
	}

	public void testRelativeHREFFileProposals() throws Exception
	{
		// FIXME I need to set up files on the filesystem and relative to editor to test those!

		String document = "<a href=\"|\"></a>";
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);
		ITextViewer viewer = createTextViewer(fDocument);

		final File tmpFile = File.createTempFile("test", ".html");
		tmpFile.deleteOnExit();

		File sibling = new File(tmpFile.getParentFile(), "sibling.html");
		sibling.createNewFile();
		sibling.deleteOnExit();

		fProcessor = new HTMLContentAssistProcessor(null)
		{
			@Override
			protected URI getURI()
			{
				return tmpFile.toURI();
			}
		};

		ICompletionProposal[] proposals = fProcessor.doComputeCompletionProposals(viewer, offset, '\t', false);
		assertTrue(proposals.length > 0);
		AssertUtil.assertProposalFound("sibling.html", proposals);
	}

	public void testIsValidAutoActivationLocationElement()
	{

		// need to close previous editor
		String source = "<a|>";

		IFileStore fileStore = createFileStore("proposal_tests", "html", source);
		this.setupTestContext(fileStore);
		int offset = this.cursorOffsets.get(0);

		// starting to type an attribute
		assertTrue(processor.isValidAutoActivationLocation(' ', ' ', document, offset));
		assertTrue(processor.isValidAutoActivationLocation('\t', '\t', document, offset));
		assertFalse(processor.isValidAutoActivationLocation('b', 'b', document, offset));
	}

	public void testIsValidAutoActivationLocationAttribute()
	{
		String source = "<a |>";
		IFileStore fileStore = createFileStore("proposal_tests", "html", source);
		this.setupTestContext(fileStore);
		int offset = this.cursorOffsets.get(0);

		// starting to type an attribute
		assertTrue(processor.isValidAutoActivationLocation('b', 'b', document, offset));

	}

	public void testIsValidAutoActivationLocationAttributeValue()
	{
		String source = "<a class=\"|\"|>";
		IFileStore fileStore = createFileStore("proposal_tests", "html", source);
		this.setupTestContext(fileStore);
		int offset = this.cursorOffsets.get(0);

		// starting to type an attribute value
		assertTrue(processor.isValidAutoActivationLocation('f', 'f', document, offset));
	}

	public void testIsValidAutoActivationLocationText()
	{
		// need to close previous editor
		String source = "<a>|";
		IFileStore fileStore = createFileStore("proposal_tests", "html", source);
		this.setupTestContext(fileStore);
		int offset = this.cursorOffsets.get(0);

		assertFalse(processor.isValidAutoActivationLocation('t', 't', document, offset));
	}

	public void testIsValidIdentifier()
	{
		assertTrue(fProcessor.isValidIdentifier('a', 'a'));
		assertTrue(fProcessor.isValidIdentifier('z', 'z'));
		assertTrue(fProcessor.isValidIdentifier('A', 'A'));
		assertTrue(fProcessor.isValidIdentifier('Z', 'Z'));
		assertFalse(fProcessor.isValidIdentifier('_', '_'));
		assertFalse(fProcessor.isValidIdentifier('$', '$'));
		assertFalse(fProcessor.isValidIdentifier(' ', ' '));
	}

	protected ITextViewer createTextViewer(IDocument fDocument)
	{
		ITextViewer viewer = new TextViewer(new Shell(), SWT.NONE);
		viewer.setDocument(fDocument);
		return viewer;
	}

	protected void assertLocation(String document, LocationType location)
	{
		int offset = HTMLTestUtil.findCursorOffset(document);
		fDocument = HTMLTestUtil.createDocument(document, true);

		LexemeProvider<HTMLTokenType> lexemeProvider = HTMLTestUtil.createLexemeProvider(fDocument, offset);
		LocationType l = fProcessor.getOpenTagLocationType(lexemeProvider, offset);

		assertEquals(location, l);
	}

	protected void assertCompletionCorrect(String source, char trigger, int proposalCount, String proposalToChoose,
			String postCompletion, Point point)
	{
		IFileStore fileStore = createFileStore("proposal_tests", "html", source);
		this.setupTestContext(fileStore);

		int offset = this.cursorOffsets.get(0);
		ITextViewer viewer = AssertUtil.createTextViewer(document);
		ICompletionProposal[] proposals = processor.doComputeCompletionProposals(viewer, offset, trigger, false);

		assertEquals(proposalCount, proposals.length);
		if (proposalToChoose != null)
		{
			AssertUtil.assertProposalFound(proposalToChoose, proposals);
			AssertUtil.assertProposalApplies(postCompletion, document, proposalToChoose, proposals, offset, point);
		}
	}
}
