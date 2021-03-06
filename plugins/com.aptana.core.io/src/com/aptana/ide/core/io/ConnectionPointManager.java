/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
// $codepro.audit.disable staticFieldNamingConvention

package com.aptana.ide.core.io;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

import com.aptana.core.epl.IMemento;
import com.aptana.core.epl.XMLMemento;
import com.aptana.core.logging.IdeLog;
import com.aptana.ide.core.io.events.ConnectionPointEvent;
import com.aptana.ide.core.io.events.IConnectionPointListener;

/**
 * @author Max Stepanov
 *
 */
/* package */ final class ConnectionPointManager extends PlatformObject implements IConnectionPointManager {

	/* package */ static final String STATE_FILENAME = "connections"; //$NON-NLS-1$

	private static final String EXTENSION_POINT_ID = CoreIOPlugin.PLUGIN_ID + ".connectionPoint"; //$NON-NLS-1$
	private static final String TAG_CONNECTION_POINT_TYPE = "connectionPointType"; //$NON-NLS-1$
	private static final String TAG_CONNECTION_POINT_CATEGORY = "connectionPointCategory"; //$NON-NLS-1$
	/* package */ static final String ATT_ID = "id"; //$NON-NLS-1$
	private static final String ATT_NAME = "name"; //$NON-NLS-1$
	private static final String ATT_ORDER = "order"; //$NON-NLS-1$
	private static final String ATT_REMOTE = "remote"; //$NON-NLS-1$
	private static final String ATT_CLASS = "class"; //$NON-NLS-1$
	private static final String ATT_CATEGORY = "category"; //$NON-NLS-1$

	private static final String ELEMENT_ROOT = "connections"; //$NON-NLS-1$
	private static final String ELEMENT_CONNECTION = "connection"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$

	private static ConnectionPointManager instance;

	private List<ConnectionPoint> connections = Collections.synchronizedList(new ArrayList<ConnectionPoint>());
	private Map<String, ConnectionPointCategory> categories = new HashMap<String, ConnectionPointCategory>();
	private List<ConnectionPointType> types = new ArrayList<ConnectionPointType>();
	private Map<String, IConfigurationElement> configurationElements = new HashMap<String, IConfigurationElement>();
	private List<IMemento> unresolvedConnections = Collections.synchronizedList(new ArrayList<IMemento>());
	private boolean dirty = false;
	
	private ListenerList listeners = new ListenerList();

	/**
	 * 
	 */
	private ConnectionPointManager() {
		readExtensionRegistry();
	}
	
	/**
	 * Returns shared instance
	 * @return
	 */
	public static ConnectionPointManager getInstance() {
		if (instance == null) {
			synchronized (ConnectionPointManager.class) {
				if (instance == null) {
					instance = new ConnectionPointManager();
				}
			}
		}
		return instance;
	}

	/**
	 * loadState
	 * 
	 * @param path
	 */
	/* package */ void loadState(IPath path) {
		File file = path.toFile();
		if (file.exists()) {
			connections.clear();
			unresolvedConnections.clear();

			addConnectionsFrom(path);
		}
	}

	/**
	 * saveState
	 * @param path
	 */
	public void saveState(IPath path) {
		XMLMemento memento = XMLMemento.createWriteRoot(ELEMENT_ROOT);
        synchronized (connections) {
            for (ConnectionPoint connectionPoint : connections) {
                if (connectionPoint.isPersistent()) {
                    IMemento child = memento.createChild(ELEMENT_CONNECTION);
                    child.putMemento(storeConnectionPoint(connectionPoint));
                }
            }
        }
        synchronized (unresolvedConnections) {
            for (IMemento child : unresolvedConnections) {
                memento.copyChild(child);
            }
        }
        FileWriter writer = null;
		try {
			writer = new FileWriter(path.toFile());
			memento.save(writer);
			isChanged();
		} catch (IOException e) {
			IdeLog.logError(CoreIOPlugin.getDefault(), e);
		} finally {
		    if (writer != null) {
		        try {
                    writer.close();
                } catch (IOException e) {
                	IdeLog.logError(CoreIOPlugin.getDefault(), e);
                }
		    }
		}
	}

	public List<IConnectionPoint> addConnectionsFrom(IPath path) {
		List<IConnectionPoint> newConnections = new ArrayList<IConnectionPoint>();
		File file = path.toFile();
		if (file.exists()) {
			FileReader reader = null;
			try {
				reader = new FileReader(file);
				XMLMemento memento = XMLMemento.createReadRoot(reader);
				for (IMemento child : memento.getChildren(ELEMENT_CONNECTION)) {
					ConnectionPoint connectionPoint = restoreConnectionPoint(child, null);
					if (connectionPoint != null) {
						connections.add(connectionPoint);
						newConnections.add(connectionPoint);
					} else {
						unresolvedConnections.add(child);
					}
				}
			} catch (IOException e) {
				IdeLog.logError(CoreIOPlugin.getDefault(), e);
			} catch (CoreException e) {
				IdeLog.logError(CoreIOPlugin.getDefault(), e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						IdeLog.logError(CoreIOPlugin.getDefault(), e);
					}
				}
			}
		}
		return newConnections;
	}

	/**
	 * isChanged
	 * @return
	 */
	public boolean isChanged() {
		for (ConnectionPoint connectionPoint : connections) {
			if (connectionPoint.isChanged()) {
				dirty = true;
			}
		}
		try {
			return dirty;
		} finally {
			dirty = false;
		}		
	}
	
	private void broadcastEvent(ConnectionPointEvent event) {
		final Object[] list = listeners.getListeners();
	    for (Object listener : list) {
	        ((IConnectionPointListener) listener).connectionPointChanged(event);
	    }
	}

	/* package */ IConnectionPoint[] getConnectionPointsForType(String type) {
		List<IConnectionPoint> list = new ArrayList<IConnectionPoint>();
		for (ConnectionPoint connectionPoint : connections) {
			if (type.equals(connectionPoint.getType())) {
				list.add(connectionPoint);
			}
		}
		return list.toArray(new IConnectionPoint[list.size()]);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#addConnectionPoint(com.aptana.ide.core.io.IConnectionPoint)
	 */
	public void addConnectionPoint(IConnectionPoint connectionPoint) {
		Assert.isLegal(connectionPoint instanceof ConnectionPoint);
		if (!connections.contains(connectionPoint)) {
			connections.add((ConnectionPoint) connectionPoint);
			dirty = true;
			broadcastEvent(new ConnectionPointEvent(this, ConnectionPointEvent.POST_ADD, connectionPoint));
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#removeConnectionPoint(com.aptana.ide.core.io.IConnectionPoint)
	 */
	public void removeConnectionPoint(IConnectionPoint connectionPoint) {
		if (connections.contains(connectionPoint)) {
			connections.remove(connectionPoint);
			dirty = true;
			broadcastEvent(new ConnectionPointEvent(this, ConnectionPointEvent.POST_DELETE, connectionPoint));
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#connectionPointChanged(com.aptana.ide.core.io.IConnectionPoint)
	 */
    public void connectionPointChanged(IConnectionPoint connectionPoint) {
        if (connections.contains(connectionPoint)) {
            dirty = true;
            broadcastEvent(new ConnectionPointEvent(this, ConnectionPointEvent.POST_CHANGE,
                    connectionPoint));
        }
    }

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#cloneConnectionPoint(com.aptana.ide.core.io.IConnectionPoint)
	 */
	public IConnectionPoint cloneConnectionPoint(IConnectionPoint connectionPoint) throws CoreException {
		Assert.isLegal(connectionPoint instanceof ConnectionPoint);
		IMemento memento;
		try {
			memento = storeConnectionPoint((ConnectionPoint) connectionPoint);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CoreIOPlugin.PLUGIN_ID, Messages.ConnectionPointManager_FailedStoreConnectionProperties, e));
		}
		ConnectionPoint clonedConnectionPoint = restoreConnectionPoint(memento, UUID.randomUUID().toString());
		return clonedConnectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#getTypes()
	 */
	public ConnectionPointType[] getTypes() {
		return types.toArray(new ConnectionPointType[types.size()]);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#getType(java.lang.String)
	 */
	public ConnectionPointType getType(String typeId) {
		for (ConnectionPointType type : types) {
			if (type.getType().equals(typeId)) {
				return type;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#getType(com.aptana.ide.core.io.IConnectionPoint)
	 */
	public ConnectionPointType getType(IConnectionPoint connectionPoint) {
		Assert.isLegal(connectionPoint instanceof ConnectionPoint);
		return getType(((ConnectionPoint) connectionPoint).getType());
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#getConnectionPointCategories()
	 */
	public IConnectionPointCategory[] getConnectionPointCategories() {
		return categories.values().toArray(new IConnectionPointCategory[categories.size()]);
	}

    /* (non-Javadoc)
     * @see com.aptana.ide.core.io.IConnectionPointManager#getConnectionPointCategory(String)
     */
	public IConnectionPointCategory getConnectionPointCategory(String categoryId) {
	    return categories.get(categoryId);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#getConnectionPoints()
	 */
	public IConnectionPoint[] getConnectionPoints() {
		return connections.toArray(new IConnectionPoint[connections.size()]);
	}

	private IMemento storeConnectionPoint(ConnectionPoint connectionPoint) {
		IMemento saveMemento = XMLMemento.createWriteRoot(ELEMENT_ROOT)
									.createChild(ELEMENT_CONNECTION);
		connectionPoint.saveState(saveMemento);
		saveMemento.putString(ATTR_ID, connectionPoint.getId());
		saveMemento.putString(ATTR_TYPE, connectionPoint.getType());
		return saveMemento;
	}
	
	private ConnectionPoint restoreConnectionPoint(IMemento memento, String id) throws CoreException {
		ConnectionPoint connectionPoint  = null;
		String typeId = memento.getString(ATTR_TYPE);
		if (typeId != null) {
			IConfigurationElement element = configurationElements.get(typeId);
			if (element != null) {
				Object object = element.createExecutableExtension(ATT_CLASS);
				if (object instanceof ConnectionPoint) {
					connectionPoint = (ConnectionPoint) object;
					connectionPoint.setId((id != null) ? id : memento.getString(ATTR_ID));
					connectionPoint.loadState(memento);
				}
			}
		}
		return connectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#restore15ConnectionPoint(java.lang.String, java.lang.String)
	 */
	public ConnectionPoint restore15ConnectionPoint(String type, String data) throws CoreException {
	    ConnectionPoint connectionPoint = null;
	    String typeId = null;
	    if (type.equals("com.aptana.ide.core.ui.io.file.LocalFileManager")) { //$NON-NLS-1$
	        typeId = "local"; //$NON-NLS-1$
	    } else if (type.equals("com.aptana.ide.core.ui.io.file.ProjectFileManager")) { //$NON-NLS-1$
	        typeId = "workspace"; //$NON-NLS-1$
	    } else if (type.equals("com.aptana.ide.io.ftp.FtpVirtualFileManager")) { //$NON-NLS-1$
	        typeId = "ftp"; //$NON-NLS-1$
	    } else if (type.equals("com.aptana.ide.io.sftp.SftpVirtualFileManager")) { //$NON-NLS-1$
	        typeId = "sftp"; //$NON-NLS-1$
	    } else if (type.equals("com.aptana.ide.io.ftps.FtpsVirtualFileManager")) { //$NON-NLS-1$
	        typeId = "ftps"; //$NON-NLS-1$
	    }
	    if (typeId != null) {
	        IConfigurationElement element = configurationElements.get(typeId);
            if (element != null) {
                Object object = element.createExecutableExtension(ATT_CLASS);
                if (object instanceof ConnectionPoint) {
                    connectionPoint = (ConnectionPoint) object;
                    if (!connectionPoint.load15Data(data)) {
                        return null;
                    }
                }
            }
	    }
	    return connectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#createConnectionPoint(com.aptana.ide.core.io.ConnectionPointType)
	 */
	public IConnectionPoint createConnectionPoint(ConnectionPointType type) throws CoreException {
		if (type != null) {
			return createConnectionPoint(type.getType());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#createConnectionPoint(java.lang.String)
	 */
	public IConnectionPoint createConnectionPoint(String typeId) throws CoreException {
		ConnectionPoint connectionPoint  = null;		
		IConfigurationElement element = configurationElements.get(typeId);
		if (element != null) {
			Object object = element.createExecutableExtension(ATT_CLASS);
			if (object instanceof ConnectionPoint) {
				connectionPoint = (ConnectionPoint) object;
			}
		}
		return connectionPoint;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.IConnectionPointManager#addConectionPointListener(com.aptana.ide.core.io.IConnectionPointListener)
	 */
    public void addConnectionPointListener(IConnectionPointListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see com.aptana.ide.core.io.IConnectionPointManager#removeConnectionPointListener(com.aptana.ide.core.io.IConnectionPointListener)
     */
    public void removeConnectionPointListener(IConnectionPointListener listener) {
        listeners.remove(listener);
    }

	private void readExtensionRegistry() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
							.getConfigurationElementsFor(EXTENSION_POINT_ID);
		for (int i = 0; i < elements.length; ++i) {
			readElement(elements[i], TAG_CONNECTION_POINT_CATEGORY);
		}
		for (int i = 0; i < elements.length; ++i) {
			readElement(elements[i], TAG_CONNECTION_POINT_TYPE);
		}
	}
	
	private void readElement(IConfigurationElement element, String elementName) {
		if (!elementName.equals(element.getName())) {
			return;
		}
		if (TAG_CONNECTION_POINT_CATEGORY.equals(element.getName())) {
			String id = element.getAttribute(ATT_ID);
			if (id == null || id.length() == 0) {
				return;
			}

			String name = element.getAttribute(ATT_NAME);
			if (name == null || name.length() == 0) {
				return;
			}
			int order = Byte.MAX_VALUE;
			try {
				order = Integer.parseInt(element.getAttribute(ATT_ORDER));
			} catch (NumberFormatException e) {
				e.getCause();
			}
			boolean remote = Boolean.parseBoolean(element.getAttribute(ATT_REMOTE));
			categories.put(id, new ConnectionPointCategory(id, name, order, remote));
		} else if (TAG_CONNECTION_POINT_TYPE.equals(element.getName())) {
			String typeId = element.getAttribute(ATT_ID);
			if (typeId == null || typeId.length() == 0) {
				return;
			}

			String name = element.getAttribute(ATT_NAME);
			if (name == null || name.length() == 0) {
				return;
			}

			String categoryId = element.getAttribute(ATT_CATEGORY);
			if (categoryId == null || categoryId.length() == 0) {
				categoryId = ""; //$NON-NLS-1$
			}

			String clazz = element.getAttribute(ATT_CLASS);
			if (clazz == null || clazz.length() == 0) {
				return;
			}
			configurationElements.put(typeId, element);
			
			ConnectionPointCategory category = categories.get(categoryId);
			if (category == null) {
				String defaultCategoryId = "unknown"; //$NON-NLS-1$
				category = categories.get(defaultCategoryId);
				if (category == null) {
					categories.put(defaultCategoryId, category =
								new ConnectionPointCategory(defaultCategoryId, Messages.ConnectionPointManager_CategoryUnknown, Integer.MAX_VALUE));
				}
			}
			ConnectionPointType type = new ConnectionPointType(typeId, name, category);
            types.add(type);
			category.addType(type);
		}
	}
}
