package com.aptana.portal.ui.dispatch.configurationProcessors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

import com.aptana.configurations.processor.ConfigurationStatus;
import com.aptana.core.util.IOUtil;
import com.aptana.portal.ui.PortalUIPlugin;
import com.aptana.portal.ui.dispatch.configurationProcessors.installer.ImportJavaScriptLibraryDialog;

/**
 * An installer (import) processor for JavaScript libraries, such as jQuery and Prototype.<br>
 * This processor download and place the JS library under a custom javascript folder in the selected (or active)
 * project. It also allows the use to select the location manually.
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class JSLibraryInstallProcessor extends InstallerConfigurationProcessor
{

	private static final String JS_LIBRARY = "JS Library"; //$NON-NLS-1$
	private static boolean installationInProgress;
	private String libraryName;

	/**
	 * Returns the JS Library name.
	 */
	@Override
	protected String getApplicationName()
	{
		return libraryName;
	}

	/**
	 * Install a JavaScript library into a user-specified project.<br>
	 * The configuration will grab the name and the location of the library from the given attributes. <br>
	 * The expected structure of attributes array is as follows:<br>
	 * <ul>
	 * <li>The first item in the array should contain a String name of the library we are installing (e.g. Prototype,
	 * jQuery etc.)</li>
	 * <li>The second item in the array should contain a non-empty array with an arbitrary amount of resource URLs that
	 * will be downloaded and placed under the 'javascript' directory (or any other user-selected directory).</li>
	 * </ul>
	 * 
	 * @param attributes
	 *            A non-empty string array, which contains the URLs for the JS library file(s).
	 * @see com.aptana.configurations.processor.AbstractConfigurationProcessor#configure(org.eclipse.core.runtime.IProgressMonitor,
	 *      java.lang.Object)
	 */
	@Override
	public ConfigurationStatus configure(IProgressMonitor progressMonitor, Object attributes)
	{
		// Get a Class lock to avoid multiple installations at the same time even with multiple instances of this
		// RubyInstallProcessor
		synchronized (this.getClass())
		{
			if (installationInProgress)
			{
				return configurationStatus;
			}
			installationInProgress = true;
		}
		try
		{
			configurationStatus.removeAttribute(CONFIG_ATTR);
			clearErrorAttributes();
			if (attributes == null || !(attributes instanceof Object[]))
			{
				String err = NLS.bind(Messages.InstallProcessor_missingInstallURLs, JS_LIBRARY);
				applyErrorAttributes(err);
				PortalUIPlugin.logError(new Exception(err));
				return configurationStatus;
			}
			Object[] attrArray = (Object[]) attributes;
			if (attrArray.length != 2)
			{
				// structure error
				String err = NLS.bind(Messages.InstallProcessor_wrongNumberOfInstallLinks, new Object[] { JS_LIBRARY,
						1, attrArray.length });
				applyErrorAttributes(err);
				PortalUIPlugin.logError(new Exception(err));
				return configurationStatus;
			}
			// Check that the second array element contains a non-empty array
			if (!(attrArray[1] instanceof Object[]) || ((Object[]) attrArray[1]).length == 0)
			{
				String err = NLS.bind(Messages.InstallProcessor_missingInstallURLs, JS_LIBRARY);
				applyErrorAttributes(err);
				PortalUIPlugin.logError("We expected an array of URLs, but got an empty array.", new Exception(err)); //$NON-NLS-1$
				return configurationStatus;
			}
			// Start the installation...
			configurationStatus.setStatus(ConfigurationStatus.PROCESSING);
			libraryName = (String) attrArray[0];
			IStatus status = download((Object[]) attrArray[1], progressMonitor);
			if (status.isOK())
			{
				status = install(progressMonitor);
			}
			switch (status.getSeverity())
			{
				case IStatus.OK:
				case IStatus.INFO:
				case IStatus.WARNING:
					displayMessageInUIThread(MessageDialog.INFORMATION, NLS.bind(
							Messages.InstallProcessor_installerTitle, libraryName), NLS.bind(
							Messages.InstallProcessor_installationSuccessful, libraryName));
					configurationStatus.setStatus(ConfigurationStatus.OK);
					break;
				case IStatus.ERROR:
					configurationStatus.setStatus(ConfigurationStatus.ERROR);
					break;
				case IStatus.CANCEL:
					configurationStatus.setStatus(ConfigurationStatus.INCOMPLETE);
					break;
				default:
					configurationStatus.setStatus(ConfigurationStatus.UNKNOWN);
			}
			return configurationStatus;
		}
		finally
		{
			synchronized (this.getClass())
			{
				installationInProgress = false;
			}
		}
	}

	/**
	 * Install the library.<br>
	 * The installation will display a selection dialog, displaying the projects in the workspace, and selecting the
	 * active project by default. It also takes into account the type of the project (nature) when suggesting the
	 * location to save the JS libraries.
	 * 
	 * @param progressMonitor
	 * @return A status indication of the process success or failure.
	 */
	protected IStatus install(IProgressMonitor progressMonitor)
	{
		Job job = new UIJob(Messages.JSLibraryInstallProcessor_directorySelection)
		{
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				ImportJavaScriptLibraryDialog dialog = new ImportJavaScriptLibraryDialog(Display.getDefault()
						.getActiveShell(), libraryName);
				if (dialog.open() == Window.OK)
				{
					String selectedLocation = dialog.getSelectedLocation();
					IPath path = Path.fromOSString(selectedLocation);
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
					// Making sure that the project is not null, although this should never happen
					if (project != null)
					{
						String fullPath = project.getLocation().append(path.removeFirstSegments(1)).toOSString();
						File targetFolder = new File(fullPath);
						if (!targetFolder.exists() && !targetFolder.mkdirs())
						{
							// could not create the directories needed!
							PortalUIPlugin
									.logError(
											"Failed to create directories when importing JS slibrary!", new Exception("Failed to create '" + fullPath + '\'')); //$NON-NLS-1$ //$NON-NLS-2$
							return new Status(IStatus.ERROR, PortalUIPlugin.PLUGIN_ID,
									Messages.JSLibraryInstallProcessor_directoriesCreationFailed);
						}
						// Copy the downloaded content into the created directory
						List<IStatus> errors = new ArrayList<IStatus>();
						for (String f : downloadedPaths)
						{
							try
							{
								File sourceLocation = new File(f);
								File targetLocation = new File(targetFolder, sourceLocation.getName());
								if (targetLocation.exists())
								{
									if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
											Messages.JSLibraryInstallProcessor_fileConflictTitle,
											Messages.JSLibraryInstallProcessor_fileConflictMessage
													+ sourceLocation.getName()
													+ Messages.JSLibraryInstallProcessor_overwriteQuestion))
									{
										continue;
									}
								}
								IOUtil.copyFile(sourceLocation, targetLocation);
							}
							catch (IOException e)
							{
								errors.add(new Status(IStatus.ERROR, PortalUIPlugin.PLUGIN_ID, e.getMessage(), e));
							}
						}
						if (!errors.isEmpty())
						{
							return new MultiStatus(PortalUIPlugin.PLUGIN_ID, 0, errors.toArray(new IStatus[errors
									.size()]), Messages.JSLibraryInstallProcessor_multipleErrorsWhileImportingJS, null);
						}
						// TODO - Override this method implementation to deal with project-specific caching.
						finalizeInstallation(targetFolder.getAbsolutePath());
					}
					else
					{
						PortalUIPlugin.logError("Unexpected null project when importing JS slibrary!", new Exception()); //$NON-NLS-1$
						return new Status(IStatus.ERROR, PortalUIPlugin.PLUGIN_ID,
								Messages.JSLibraryInstallProcessor_unexpectedNull);
					}
					try
					{
						project.refreshLocal(IResource.DEPTH_INFINITE, SubMonitor.convert(monitor));
					}
					catch (CoreException e)
					{
						PortalUIPlugin.logError("Error while refreshing the project.", e); //$NON-NLS-1$
					}
					return Status.OK_STATUS;
				}
				else
				{
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.setSystem(true);
		job.schedule();
		try
		{
			job.join();
		}
		catch (InterruptedException e)
		{
			PortalUIPlugin.logError(e);
		}
		return job.getResult();
	}
}
