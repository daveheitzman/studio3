package com.aptana.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.aptana.core.CorePlugin;

public abstract class StringUtil
{
	/**
	 * Create a string by concatenating the elements of a string array using a delimited between each item
	 * 
	 * @param delimiter
	 *            The text to place between each element in the array
	 * @param items
	 *            The array of items to join
	 * @return The resulting string
	 */
	public static String join(String delimiter, String ... items)
	{
		if (items == null)
		{
			return null;
		}

		int length = items.length;
		String result = ""; //$NON-NLS-1$
		if (length > 0)
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length - 1; i++)
			{
				sb.append(items[i]).append(delimiter);
			}
			sb.append(items[length - 1]);

			result = sb.toString();
		}
		return result;
	}
	
	/**
	 * Create a string by concatenating the elements of a string array using a delimited between each item
	 * 
	 * @param delimiter
	 *            The text to place between each element in the array
	 * @param items
	 *            The array of items to join
	 * @return The resulting string
	 */
	public static String join(String delimiter, List<String> items)
	{
		if (items == null)
		{
			return null;
		}
		
		int length = items.size();
		String result = ""; //$NON-NLS-1$
		if (length > 0)
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length - 1; i++)
			{
				sb.append(items.get(i)).append(delimiter);
			}
			sb.append(items.get(length - 1));
			
			result = sb.toString();
		}
		return result;
	}

	/**
	 * Replace one string with another
	 * 
	 * @param str
	 * @param pattern
	 * @param replace
	 * @return String
	 */
	public static String replace(String str, String pattern, String replace)
	{

		int s = 0;
		int e = 0;
		StringBuilder result = new StringBuilder();
		while ((e = str.indexOf(pattern, s)) >= 0)
		{
			result.append(str.substring(s, e));
			result.append(replace);
			s = e + pattern.length();
		}
		result.append(str.substring(s));

		return result.toString();
	}

	/**
	 * Adds a colon to the end of the string, as if making a form label
	 * 
	 * @param message
	 * @return string + colon
	 */
	public static String makeFormLabel(String message)
	{
		return message + ":"; //$NON-NLS-1$
	}

	/**
	 * Adds an ellipsis to the end of a string, generally indicating that this string leads to another choice (like a
	 * dialog)
	 * 
	 * @param message
	 * @return The ellipsif-ied string
	 */
	public static String ellipsify(String message)
	{
		return message + "..."; //$NON-NLS-1$
	}	

	/**
	 * Given a raw input string template, this will do a mass search and replace for the map of variables to values.
	 * Acts like {@link String#replaceAll(String, String)}
	 * 
	 * @param template
	 * @param variables
	 * @return
	 */
	public static String replaceAll(String template, Map<String, String> variables)
	{
		if (variables == null || variables.isEmpty())
			return template;
		for (Map.Entry<String, String> entry : variables.entrySet())
		{
			String value = entry.getValue();
			if (value == null)
				value = ""; //$NON-NLS-1$
			else
				value = value.replace('$', (char) 1); // To avoid illegal group reference issues if the text has
			// dollars!
			template = template.replaceAll(entry.getKey(), value).replace((char) 1, '$');
		}
		return template;
	}

	/**
	 * Runs the input through a StringTokenizer and gathers up all the tokens.
	 * 
	 * @param inputString
	 * @param delim
	 * @return
	 */
	public static List<String> tokenize(String inputString, String delim)
	{
		List<String> tokens = new ArrayList<String>();
		if (inputString == null)
			return tokens;
		StringTokenizer tokenizer = new StringTokenizer(inputString, delim);
		while (tokenizer.hasMoreTokens())
			tokens.add(tokenizer.nextToken());
		return tokens;
	}

	/**
	 * Generate an MD5 hash of a string.
	 * 
	 * @param lowerCase
	 * @return null if an exception occurs.
	 */
	public static String md5(String lowerCase)
	{
		if (lowerCase == null)
			return null;
		try
		{
			byte[] bytesOfMessage = lowerCase.getBytes("UTF-8"); //$NON-NLS-1$
			MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			byte[] thedigest = md.digest(bytesOfMessage);
			BigInteger bigInt = new BigInteger(1, thedigest);
			String hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while (hashtext.length() < 32)
			{
				hashtext = "0" + hashtext; //$NON-NLS-1$
			}
			return hashtext;
		}
		catch (Exception e)
		{
			CorePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, e.getMessage(), e));
		}
		return null;
	}

	/**
	 * Sanitizes raw HTML to escape '&', '<' and '>' so that it is suitable for embedding into HTML.
	 * 
	 * @param raw
	 * @return
	 */
	public static String sanitizeHTML(String raw)
	{
		return raw.replaceAll("&", "&amp;").replaceAll("<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Compares two strings for equality taking into account that none, one, or both may be null
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean areNotEqual(String s1, String s2)
	{
		return (s1 == null) ? (s2 != null) : (s2 == null) ? true : !s1.equals(s2);
	}
	
	/**
	 * Compares two strings for equality taking into account that none, one, or both may be null
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean areEqual(String s1, String s2)
	{
		return (s1 == null) ? (s2 == null) : (s2 != null) ? s1.equals(s2) : false;
	}
}