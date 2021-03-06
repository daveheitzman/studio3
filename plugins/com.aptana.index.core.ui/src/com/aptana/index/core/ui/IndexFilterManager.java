/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.index.core.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;

import com.aptana.core.io.efs.EFSUtils;
import com.aptana.core.util.StringUtil;
import com.aptana.index.core.ui.preferences.IPreferenceConstants;

public class IndexFilterManager
{
	private static final String ITEM_DELIMITER = "\0"; //$NON-NLS-1$
	private static IndexFilterManager INSTANCE;

	/**
	 * getInstance
	 * 
	 * @return
	 */
	public static IndexFilterManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new IndexFilterManager();
		}

		return INSTANCE;
	}

	private Set<IFileStore> _filteredItems;

	/**
	 * IndexFilterManager
	 */
	private IndexFilterManager()
	{
		this._filteredItems = this.loadFilteredItems();
	}

	/**
	 * addFilterItem
	 * 
	 * @param IFileStore
	 */
	public void addFilterItem(IFileStore item)
	{
		if (item != null)
		{
			if (this._filteredItems == null)
			{
				this._filteredItems = new HashSet<IFileStore>();
			}

			// only add if this item isn't being filtered already
			if (this.isFilteredItem(item) == false)
			{
				// remove any pre-existing file stores that are children of the
				// item we're about to add
				Set<IFileStore> toRemove = new HashSet<IFileStore>();

				for (IFileStore candidate : this._filteredItems)
				{
					if (item.isParentOf(candidate))
					{
						toRemove.add(candidate);
					}
				}

				this._filteredItems.removeAll(toRemove);

				// add new item to our list
				this._filteredItems.add(item);
			}
		}
	}

	/**
	 * commitFilteredItems
	 */
	public void commitFilteredItems()
	{
		// determine which file stores were added and deleted
		Set<IFileStore> deletedItems = this.loadFilteredItems();
		Set<IFileStore> addedItems = new HashSet<IFileStore>(this._filteredItems);

		addedItems.removeAll(deletedItems);
		deletedItems.removeAll(this._filteredItems);

		// build a preference value of all file stores we are filtering
		String value;

		if (this._filteredItems != null)
		{
			List<String> uris = new ArrayList<String>();

			for (IFileStore item : this._filteredItems)
			{
				URI uri = item.toURI();

				uris.add(uri.toString());
			}

			value = StringUtil.join(ITEM_DELIMITER, uris);
		}
		else
		{
			value = IPreferenceConstants.NO_ITEMS;
		}

		// now save the file store list
		IPreferenceStore prefs = IndexUiActivator.getDefault().getPreferenceStore();

		prefs.putValue(IPreferenceConstants.FILTERED_INDEX_URIS, value);

		// determine which projects have been affected by the last set of changes
		Set<IProject> projects = new HashSet<IProject>();

		for (IFileStore f : addedItems)
		{
			IResource resource = (IResource) f.getAdapter(IResource.class);

			if (resource != null)
			{
				projects.add(resource.getProject());
			}
		}
		for (IFileStore f : deletedItems)
		{
			IResource resource = (IResource) f.getAdapter(IResource.class);

			if (resource != null)
			{
				projects.add(resource.getProject());
			}
		}

		// update project indexes that were affected by our changes
		for (final IProject p : projects)
		{
			Job job = new Job(MessageFormat.format("Rebuilding {0}", p.getName())) //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					try
					{
						p.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					}
					catch (CoreException e)
					{
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}

			};
			job.schedule();
		}
	}

	/**
	 * applyFilter
	 * 
	 * @param fileStores
	 * @return
	 */
	protected Set<IFileStore> applyFilter(Set<IFileStore> fileStores)
	{
		if (this._filteredItems != null && this._filteredItems.isEmpty() == false && fileStores != null)
		{
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			Set<IFileStore> toRemove = new HashSet<IFileStore>();

			// NOTE: The indexing system creates LocalFiles but filters are based
			// on WorkspaceFiles. The following tries to convert each LocalFile
			// in the set to a WorkspaceFile before testing if the item needs to
			// be filtered.
			for (IFileStore fileStore : fileStores)
			{
				for (IContainer container : workspaceRoot.findContainersForLocationURI(fileStore.toURI()))
				{
					IFileStore workspaceFileStore = EFSUtils.getFileStore(container);

					if (this.isFilteredItem(workspaceFileStore))
					{
						toRemove.add(fileStore);
					}
				}
			}

			fileStores.removeAll(toRemove);
		}

		return fileStores;
	}

	/**
	 * getFilteredItems
	 * 
	 * @return
	 */
	public Set<IFileStore> getFilteredItems()
	{
		Set<IFileStore> result = this._filteredItems;

		if (result == null)
		{
			result = Collections.emptySet();
		}

		return result;
	}

	/**
	 * isFilteredItem
	 * 
	 * @param item
	 * @return
	 */
	public boolean isFilteredItem(IFileStore item)
	{
		boolean result = false;

		if (item != null)
		{
			for (IFileStore candidate : this._filteredItems)
			{
				if (candidate.equals(item) || candidate.isParentOf(item))
				{
					result = true;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * loadFilteredItems
	 */
	private Set<IFileStore> loadFilteredItems()
	{
		Set<IFileStore> filteredItems = new HashSet<IFileStore>();

		IPreferenceStore prefs = IndexUiActivator.getDefault().getPreferenceStore();
		String uris = prefs.getString(IPreferenceConstants.FILTERED_INDEX_URIS);

		if (uris != null && uris.length() != 0)
		{
			for (String uriString : uris.split(ITEM_DELIMITER))
			{
				try
				{
					URI uri = new URI(uriString);
					IFileStore item = EFS.getStore(uri);

					// TODO: Is it possible to have non-local files in projects
					// and will this still work for those?
					if (item.fetchInfo().exists())
					{
						filteredItems.add(item);
					}
				}
				catch (URISyntaxException e)
				{
					IndexUiActivator.logError(e.getMessage(), e);
				}
				catch (CoreException e)
				{
					IndexUiActivator.logError(e);
				}
			}
		}

		return filteredItems;
	}

	/**
	 * removeFilterItem
	 * 
	 * @param item
	 */
	public void removeFilterItem(IFileStore item)
	{
		if (item != null && this._filteredItems != null)
		{
			this._filteredItems.remove(item);
		}
	}
}
