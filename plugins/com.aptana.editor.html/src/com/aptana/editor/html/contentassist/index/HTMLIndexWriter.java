package com.aptana.editor.html.contentassist.index;

import java.io.InputStream;

import com.aptana.editor.html.contentassist.model.AttributeElement;
import com.aptana.editor.html.contentassist.model.ElementElement;
import com.aptana.editor.html.contentassist.model.EventElement;
import com.aptana.index.core.Index;
import com.aptana.util.StringUtil;

public class HTMLIndexWriter
{
	private HTMLMetadataReader _reader;

	/**
	 * HTMLIndexWriter
	 */
	public HTMLIndexWriter()
	{
		this._reader = new HTMLMetadataReader();
	}

	/**
	 * getDocumentPath
	 * 
	 * @return
	 */
	protected String getDocumentPath()
	{
		return HTMLIndexConstants.METADATA;
	}
	
	/**
	 * loadXML
	 * 
	 * @param stream
	 * @throws Exception
	 */
	public void loadXML(InputStream stream) throws Exception
	{
		this._reader.loadXML(stream);
	}

	/**
	 * writeAttribute
	 * 
	 * @param index
	 * @param attribute
	 */
	protected void writeAttribute(Index index, AttributeElement attribute)
	{
	}

	/**
	 * writeElement
	 * 
	 * @param index
	 * @param element
	 */
	protected void writeElement(Index index, ElementElement element)
	{
		String[] columns = new String[] {
			element.getName(),
			element.getDisplayName(),
			element.getRelatedClass(),
			StringUtil.join(HTMLIndexConstants.SUB_DELIMITER, element.getAttributes()),
			// specifications,
			// user agents
			element.getDeprecated(),
			element.getDescription(),
			StringUtil.join(HTMLIndexConstants.SUB_DELIMITER, element.getEvents()),
			element.getExample(),
			StringUtil.join(HTMLIndexConstants.SUB_DELIMITER, element.getReferences()),
			element.getRemark()
		};
		String key = StringUtil.join(HTMLIndexConstants.DELIMITER, columns);
		
		index.addEntry(HTMLIndexConstants.ELEMENT, key, this.getDocumentPath());
	}

	/**
	 * writeEvent
	 * 
	 * @param index
	 * @param event
	 */
	protected void writeEvent(Index index, EventElement event)
	{
	}

	/**
	 * writeToIndex
	 * 
	 * @param index
	 */
	public void writeToIndex(Index index)
	{
		for (ElementElement element : this._reader.getElements())
		{
			this.writeElement(index, element);
		}

		for (AttributeElement attribute : this._reader.getAttributes())
		{
			this.writeAttribute(index, attribute);
		}

		for (EventElement event : this._reader.getEvents())
		{
			this.writeEvent(index, event);
		}
	}
}