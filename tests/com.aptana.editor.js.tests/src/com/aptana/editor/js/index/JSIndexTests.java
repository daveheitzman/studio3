package com.aptana.editor.js.index;

import junit.framework.TestCase;

import com.aptana.editor.js.model.FunctionElement;
import com.aptana.editor.js.model.PropertyElement;
import com.aptana.editor.js.model.TypeElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;

public class JSIndexTests extends TestCase
{
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		IndexManager.getInstance().removeIndex(IndexConstants.METADATA);
		
		super.tearDown();
	}

	/**
	 * getIndex
	 * 
	 * @return
	 */
	private Index getIndex()
	{
		IndexManager manager = IndexManager.getInstance();

		return manager.getIndex(IndexConstants.METADATA);
	}
	
	/**
	 * getType
	 * 
	 * @param typeName
	 * @return
	 */
	private TypeElement getType(String typeName)
	{
		JSIndexReader reader = new JSIndexReader();
		
		return reader.loadType(this.getIndex(), typeName);
	}
	
	/**
	 * writeType
	 * 
	 * @param type
	 */
	private void writeType(TypeElement type)
	{
		JSIndexWriter writer = new JSIndexWriter();
		
		writer.writeType(this.getIndex(), type);
	}
	
	/**
	 * testType
	 */
	public void testType()
	{
		String typeName = "MyClass";
		
		TypeElement type = new TypeElement();
		type.setName(typeName);
		this.writeType(type);
		
		TypeElement retrievedType = this.getType(typeName);
		
		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());
	}
	
	/**
	 * testMethod
	 */
	public void testMethod()
	{
		String typeName = "MyClass";
		String methodName = "myMethod";
		
		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);
		
		// create method within type
		FunctionElement method = new FunctionElement();
		method.setName(methodName);
		type.addProperty(method);
		
		// write type to index
		this.writeType(type);

		// then retrieve it
		TypeElement retrievedType = this.getType(typeName);
		
		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());
		
		// make sure we have one property
		PropertyElement[] properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.length == 1);
		
		// make sure it is a function
		PropertyElement property = properties[0];
		assertTrue(property instanceof FunctionElement);
		
		// make sure it is the function we added earlier
		FunctionElement retrievedMethod = (FunctionElement) property;
		assertEquals(methodName, retrievedMethod.getName());
	}
	
	/**
	 * testProperty
	 */
	public void testProperty()
	{
		String typeName = "MyClass";
		String propertyName = "myProperty";
		
		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);
		
		// create property within type
		PropertyElement property = new PropertyElement();
		property.setName(propertyName);
		type.addProperty(property);
		
		// write type to index
		this.writeType(type);
		
		// then retrieve it
		TypeElement retrievedType = this.getType(typeName);
		
		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());
		
		// make sure we have one property
		PropertyElement[] properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.length == 1);
		
		// make sure the name is correct
		PropertyElement retrievedProperty = properties[0];
		assertEquals(propertyName, retrievedProperty.getName());
	}
}