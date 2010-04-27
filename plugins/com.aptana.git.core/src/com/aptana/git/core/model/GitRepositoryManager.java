package com.aptana.git.core.model;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.GitRepositoryProvider;

public class GitRepositoryManager implements IGitRepositoryManager
{
	private static final String GIT_DIR = GitRepository.GIT_DIR;

	private Set<IGitRepositoriesListener> listeners = new HashSet<IGitRepositoriesListener>();
	private Map<String, SoftReference<GitRepository>> cachedRepos = new HashMap<String, SoftReference<GitRepository>>(3);

	public void addListener(IGitRepositoriesListener listener)
	{
		synchronized (listeners)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(IGitRepositoriesListener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	@Override
	public void cleanup()
	{
		for (SoftReference<GitRepository> reference : cachedRepos.values())
		{
			if (reference == null || reference.get() == null)
				continue;
			GitRepository cachedRepo = reference.get();
			cachedRepo.dispose();
		}
		cachedRepos.clear();
	}

	@Override
	public void create(String path)
	{
		if (path == null)
			return;
		if (path.endsWith(File.separator + GIT_DIR))
		{
			path = path.substring(0, path.length() - GIT_DIR.length());
		}

		File file = new File(path);
		URI existing = gitDirForURL(file.toURI());
		if (existing != null)
			return;

		if (!file.exists())
		{
			file.mkdirs();
		}
		GitExecutable.instance().runInBackground(path, "init"); //$NON-NLS-1$
	}

	@Override
	public void removeRepository(IProject p)
	{
		GitRepository repo = getUnattachedExisting(p.getLocationURI());
		if (repo == null)
			return;

		cachedRepos.remove(p.getLocationURI().getPath());
		boolean dispose = true;
		
		// Only dispose if there's no other projects attached to same repo!
		for (SoftReference<GitRepository> ref : cachedRepos.values())
		{
			if (ref == null || ref.get() == null)
				continue;
			GitRepository other = ref.get();
			if (other.equals(repo))
			{
				dispose = false;
				break;
			}
		}
		
		// Notify listeners
		RepositoryRemovedEvent e = new RepositoryRemovedEvent(repo, p);
		for (IGitRepositoriesListener listener : listeners)
			listener.repositoryRemoved(e);
		
		if (dispose)
		{			
			repo.dispose();
			repo = null;
		}
	}

	@Override
	public GitRepository getAttached(IProject project)
	{
		if (project == null)
			return null;

		RepositoryProvider provider = RepositoryProvider.getProvider(project, GitRepositoryProvider.ID);
		if (provider == null)
			return null;

		return getUnattachedExisting(project.getLocationURI());
	}

	@Override
	public synchronized GitRepository getUnattachedExisting(URI path)
	{
		if (GitExecutable.instance() == null || GitExecutable.instance().path() == null || path == null)
			return null;

		SoftReference<GitRepository> ref = cachedRepos.get(path.getPath());
		if (ref == null || ref.get() == null)
		{
			URI gitDirURL = gitDirForURL(path);
			if (gitDirURL == null)
				return null;
			// Check to see if any cached repo has the same git dir
			for (SoftReference<GitRepository> reference : cachedRepos.values())
			{
				if (reference == null || reference.get() == null)
					continue;
				GitRepository cachedRepo = reference.get();
				if (cachedRepo.getFileURL().getPath().equals(gitDirURL.getPath()))
				{
					// Same git dir, so cache under our new path as well
					cachedRepos.put(path.getPath(), reference);
					return cachedRepo;
				}
			}
			// no cache for this repo or any repo sharing same git dir
			ref = new SoftReference<GitRepository>(new GitRepository(gitDirURL));
			cachedRepos.put(path.getPath(), ref);
		}
		// TODO What if the underlying .git dir was wiped while we still had the object cached?
		return ref.get();
	}

	@Override
	public GitRepository attachExisting(IProject project, IProgressMonitor m) throws CoreException
	{
		if (m == null)
			m = new NullProgressMonitor();
		GitRepository repo = getUnattachedExisting(project.getLocationURI());
		m.worked(40);
		if (repo == null)
			return null;

		try
		{
			RepositoryProvider.map(project, GitRepositoryProvider.ID);
			m.worked(10);
			fireRepositoryAddedEvent(repo, project);
			m.worked(50);
		}
		catch (TeamException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, GitPlugin.getPluginId(), e.getMessage(), e));
		}
		return repo;
	}

	private void fireRepositoryAddedEvent(GitRepository repo, IProject project)
	{
		RepositoryAddedEvent e = new RepositoryAddedEvent(repo, project);
		for (IGitRepositoriesListener listener : listeners)
			listener.repositoryAdded(e);
	}

	@Override
	public URI gitDirForURL(URI repositoryURL)
	{
		if (GitExecutable.instance() == null)
			return null;

		String repositoryPath = repositoryURL.getPath();
		if (repositoryURL.getScheme().equals("file")) //$NON-NLS-1$
		{
			repositoryPath = new File(repositoryURL).getAbsolutePath();
		}
		if (!new File(repositoryPath).exists())
			return null;

		if (isBareRepository(repositoryPath))
			return repositoryURL;

		// Use rev-parse to find the .git dir for the repository being opened
		Map<Integer, String> result = GitExecutable.instance()
				.runInBackground(repositoryPath, "rev-parse", "--git-dir"); //$NON-NLS-1$ //$NON-NLS-2$
		if (result == null || result.isEmpty())
			return null;
		Integer exitCode = result.keySet().iterator().next();
		if (exitCode != 0)
			return null;
		String newPath = result.values().iterator().next();
		if (newPath == null)
			return null;
		if (newPath.equals(GIT_DIR))
			return new File(repositoryPath, GIT_DIR).toURI();
		if (newPath.length() > 0)
			return new File(newPath).toURI();

		return null;
	}

	private boolean isBareRepository(String path)
	{
		String output = GitExecutable.instance().outputForCommand(path, "rev-parse", "--is-bare-repository"); //$NON-NLS-1$ //$NON-NLS-2$
		return "true".equals(output); //$NON-NLS-1$
	}

	@Override
	public void addListenerToEachRepository(IGitRepositoryListener listener)
	{
		if (listener == null)
			return;
		// Go through every project in workspace and see if there's an attached repo for each, if so add listener!
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
		{
			GitRepository repo = getAttached(project);
			if (repo == null)
				continue;
			repo.addListener(listener);
		}
	}

	@Override
	public void removeListenerFromEachRepository(IGitRepositoryListener listener)
	{
		if (listener == null)
			return;
		// Go through every project in workspace and see if there's an attached repo for each, if so add listener!
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
		{
			GitRepository repo = getAttached(project);
			if (repo == null)
				continue;
			repo.removeListener(listener);
		}
	}

}
