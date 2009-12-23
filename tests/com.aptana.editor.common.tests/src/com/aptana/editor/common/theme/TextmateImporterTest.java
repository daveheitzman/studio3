package com.aptana.editor.common.theme;

import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

import com.aptana.editor.common.CommonEditorPlugin;

public class TextmateImporterTest extends TestCase
{

	public void testImportOfMidnightTheme() throws Exception
	{
		TextmateImporter importer = new TextmateImporter();
		InputStream stream = FileLocator.openStream(CommonEditorPlugin.getDefault().getBundle(), new Path(
				"Midnight.tmTheme"), false);
		Theme theme = importer.convert(stream);
		assertEquals("Midnight", theme.getName());
		assertEquals(new RGB(248, 248, 248), theme.getForeground());
		assertEquals(new RGB(10, 0, 31), theme.getBackground());
		assertEquals(new RGB(37, 0, 255), theme.getSelection());
		assertEquals(new RGB(255, 255, 255), theme.getCaret());
		assertEquals(new RGB(60, 30, 255), theme.getLineHighlight());

		assertEquals(new RGB(105, 0, 161), theme.getForegroundAsRGB("comment"));
		assertEquals(new RGB(171, 42, 29), theme.getForegroundAsRGB("invalid.deprecated"));
		assertEquals(SWT.ITALIC, theme.getTextAttribute("invalid.deprecated").getStyle());
		assertEquals(new RGB(157, 30, 21), theme.getBackgroundAsRGB("invalid.illegal"));

		assertEquals(new RGB(255, 213, 0), theme.getForegroundAsRGB("constant"));
		assertEquals(new RGB(89, 158, 255), theme.getForegroundAsRGB("keyword"));
		assertEquals(new RGB(117, 175, 255), theme.getForegroundAsRGB("storage"));
		assertEquals(new RGB(0, 241, 58), theme.getForegroundAsRGB("string"));
		assertEquals(new RGB(0, 241, 58), theme.getForegroundAsRGB("meta.verbatim"));

		assertTrue(theme.hasEntry("meta.tag"));
		assertTrue(theme.hasEntry("meta.tag entity"));
	}

}
