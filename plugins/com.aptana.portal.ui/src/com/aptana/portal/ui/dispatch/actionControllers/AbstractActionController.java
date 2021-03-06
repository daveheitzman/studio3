/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.portal.ui.dispatch.actionControllers;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.aptana.configurations.processor.ConfigurationProcessorsRegistry;
import com.aptana.configurations.processor.IConfigurationProcessor;
import com.aptana.configurations.processor.IConfigurationProcessorListener;
import com.aptana.core.logging.IdeLog;
import com.aptana.portal.ui.PortalUIPlugin;
import com.aptana.portal.ui.dispatch.BrowserNotifier;
import com.aptana.portal.ui.dispatch.IActionController;
import com.aptana.portal.ui.dispatch.IBrowserNotificationConstants;

/**
 * A base class for all the IActionControllers contributed through the browserActionControllers extension point. Mark
 * any 'action' method with the {@link ControllerAction} annotation. <br>
 * By default, the action name will be matching the method name that contains the 'Action' annotation. However, it is
 * possible to re-define the action's name by setting the name attribute when declaring the annotation.<br>
 * For example:<br>
 * 
 * <pre>
 * <i>@Action (name = "myFunctionName")</i>
 * </pre>
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public abstract class AbstractActionController implements IActionController, IConfigurationProcessorListener
{
	private Map<String, Method> actions;
	private String configurationProcessorID;
	private IConfigurationProcessor processor;

	/*
	 * (non-Javadoc)
	 * @see com.aptana.portal.dispatch.IActionController#getActions()
	 */
	public String[] getActions()
	{
		Set<String> actionNames = getActionsMap().keySet();
		return actionNames.toArray(new String[actionNames.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.portal.dispatch.IActionController#hasAction(java.lang.String)
	 */
	public boolean hasAction(String action)
	{
		return getActionsMap().containsKey(action);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.portal.dispatch.IActionController#invokeAction(java.lang.String, java.lang.Object[])
	 */
	public Object invokeAction(String action, Object args)
	{
		Method method = getActionsMap().get(action);
		if (method != null)
		{
			try
			{
				Object[] params = null;
				if (args == null)
				{
					params = new Object[0];
				}
				else
				{
					params = new Object[] { args };
				}
				// Make a generic check for the configuration processor, if exists. In case there is a problem, return
				// an error result even before invoking the method. This ease up the error detection for all the
				// ControllerAction methods that use a processor.
				String processorId = getConfigurationProcessorId();
				if (processorId != null)
				{
					// Get the configuration processor and make sure it's valid before we invoke the method
					final IConfigurationProcessor processor = getProcessor();
					if (processor == null)
					{
						String message = MessageFormat.format(
								"The configuration process for {0} was null", this.getClass().getName()); //$NON-NLS-1$
						IdeLog.logError(PortalUIPlugin.getDefault(), new Exception(message));
						return createInternalErrorNotification();
					}
				}
				// Make the invoke call
				Object result = method.invoke(this, params);
				if (result == null)
				{
					// The invocation was for a void method, so just return
					// a JSON.OK.
					return IBrowserNotificationConstants.JSON_OK;
				}
				return result;
			}
			catch (Exception e)
			{
				IdeLog.logError(PortalUIPlugin.getDefault(), e);
				return BrowserNotifier
						.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR, e.getMessage());
			}
		}
		return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_UNKNOWN_ACTION,
				Messages.AbstractActionController_invocationError + action);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.portal.ui.dispatch.IActionController#getConfigurationProcessorId()
	 */
	public String getConfigurationProcessorId()
	{
		return configurationProcessorID;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.portal.ui.dispatch.IActionController#setConfigurationProcessorId(java.lang.String)
	 */
	public void setConfigurationProcessorId(String id)
	{
		configurationProcessorID = id;
	}

	/**
	 * Returns a Map holding a String-to-Method mapping
	 * 
	 * @return A String-to-Method Map
	 */
	protected Map<String, Method> getActionsMap()
	{
		if (actions == null)
		{
			loadActions();
		}
		return actions;
	}

	/**
	 * Load the actions into the action map
	 */
	protected synchronized void loadActions()
	{
		actions = new HashMap<String, Method>();
		Method[] methods = this.getClass().getMethods();
		for (Method method : methods)
		{
			if (method.isAnnotationPresent(ControllerAction.class))
			{
				ControllerAction annotation = method.getAnnotation(ControllerAction.class);
				String annotationName = annotation.name();
				if (annotationName == null || "".equals(annotationName)) //$NON-NLS-1$
				{
					actions.put(method.getName(), method);
				}
				else
				{
					actions.put(annotationName, method);
				}
			}
		}
	}

	/**
	 * Return a JSON error message indicating an internal error.
	 * 
	 * @return A JSON notification string indicating an internal error.
	 * @see BrowserNotifier#toJSONErrorNotification(String, String)
	 */
	protected Object createInternalErrorNotification()
	{
		return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR,
				Messages.ActionController_internalError);
	}

	/**
	 * Returns the IConfigurationProcessor attached to this action controller. Null, if none was defined in the
	 * extension.
	 * 
	 * @return An IConfigurationProcessor instance, or null.
	 */
	protected IConfigurationProcessor getProcessor()
	{
		if (processor == null)
		{
			processor = ConfigurationProcessorsRegistry.getInstance().getConfigurationProcessor(
					getConfigurationProcessorId());
		}
		return processor;
	}
}
