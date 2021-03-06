/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.git.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.git.core.model.GitIndex;
import com.aptana.git.core.model.GitRepository;
import com.aptana.git.core.model.IGitRepositoryManager;

class GitResourceListener implements IResourceChangeListener
{

	/**
	 * Bit-mask describing interesting changes for IResourceChangeListener events
	 */
	private static int INTERESTING_CHANGES = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM
			| IResourceDelta.MOVED_TO | IResourceDelta.OPEN | IResourceDelta.REPLACED | IResourceDelta.TYPE;

	/**
	 * Callback for IResourceChangeListener events Schedules a refresh of the changed resource If the preference for
	 * computing deep dirty states has been set we walk the ancestor tree of the changed resource and update all parents
	 * as well.
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event)
	{
		if (event == null || event.getDelta() == null)
			return;
		final Set<GitRepository> resourcesToUpdate = new HashSet<GitRepository>();
		final Set<IProject> projectsToAttach = new HashSet<IProject>();

		try
		{ // Compute the changed resources by looking at the delta
			event.getDelta().accept(new IResourceDeltaVisitor()
			{
				public boolean visit(IResourceDelta delta) throws CoreException
				{

					// If the file has changed but not in a way that we care
					// about (e.g. marker changes to files) then ignore
					if (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & INTERESTING_CHANGES) == 0)
					{
						return true;
					}

					// Auto-attach to git if it's a new project being added and there's a repo and it's not already
					// attached
					final IResource resource = delta.getResource();
					if (resource != null && resource instanceof IProject && delta.getKind() == IResourceDelta.ADDED)
					{
						final GitRepository mapping = getRepo(resource);
						IProject project = (IProject) resource;
						if (mapping == null)
						{
							projectsToAttach.add(project);
							return false;
						}
					}

					// If the resource is not part of a project under Git
					// revision control
					final GitRepository mapping = getRepo(resource);
					if (mapping == null)
					{
						// Ignore the change
						return true;
					}

					if (resource.getType() == IResource.ROOT)
					{
						// Continue with the delta
						return true;
					}

					if (resource.getType() == IResource.PROJECT)
					{
						// If the project is not accessible, don't process it
						if (!resource.isAccessible())
							return false;
					}

					// All seems good, schedule the repo for update.
					// TODO We force a refresh of the whole index for this repo. Maybe we should see if there's a way to
					// refresh the status of just this file?
					resourcesToUpdate.add(mapping);

					if (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.OPEN) > 1)
						return false; // Don't recurse when opening projects
					return true;
				}
			}, true /* includePhantoms */);
		}
		catch (CoreException e)
		{
			IdeLog.logError(GitPlugin.getDefault(), e, IDebugScopes.DEBUG);
		}

		if (autoAttachGitRepos() && !projectsToAttach.isEmpty())
		{
			Job job = new Job("Attaching Git repos") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					SubMonitor sub = SubMonitor.convert(monitor, 10 * projectsToAttach.size());
					MultiStatus multi = new MultiStatus(GitPlugin.getPluginId(), 0, null, null);
					multi.add(Status.OK_STATUS);
					for (final IProject project : projectsToAttach)
					{
						try
						{
							if (project.isAccessible())
								getGitRepositoryManager().attachExisting(project, sub.newChild(10));
						}
						catch (CoreException e)
						{
							multi.add(e.getStatus());
						}
					}
					return multi;
				}
			};
			job.setSystem(!EclipseUtil.showSystemJobs());
			job.setPriority(Job.SHORT);
			job.schedule();
		}

		if (resourcesToUpdate.isEmpty())
			return;

		for (final GitRepository repo : resourcesToUpdate)
		{
			if (repo == null)
				continue;
			GitIndex index = repo.index();
			if (index != null)
			{
				// FIXME This causes the whole index to refresh whenever any file/dir is changed in the workspace attached to the repo. 
				// We already listen to changes to the git index file, so when do we need to refresh here? Can we do a refresh of only the diff?
				index.refreshAsync(); // queue up a refresh
			}
		}
	}

	private boolean autoAttachGitRepos()
	{
		return Platform.getPreferencesService().getBoolean(GitPlugin.getPluginId(),
				IPreferenceConstants.AUTO_ATTACH_REPOS, true, null);
	}

	private IGitRepositoryManager getGitRepositoryManager()
	{
		return GitPlugin.getDefault().getGitRepositoryManager();
	}

	private GitRepository getRepo(IResource resource)
	{
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		if (project == null)
			return null;
		return getGitRepositoryManager().getAttached(project);
	}
}
