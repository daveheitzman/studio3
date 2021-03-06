/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.text.reconciler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.reconciler.Reconciler;

public class CommonReconciler extends Reconciler {

	private final IReconcilingStrategy defaultStrategy;
	private final Set<IReconcilingStrategy> reconcilingStrategies = new HashSet<IReconcilingStrategy>();
	private BundleChangeReconcileTrigger bundleChangeReconcileTrigger;

	/**
	 * Used for performance testing purposes so we can see if we've finished our
	 * first reconcile!
	 */
	@SuppressWarnings("unused")
	private boolean fIninitalProcessDone = false;


	/**
	 * 
	 */
	public CommonReconciler(IReconcilingStrategy defaultStrategy) {
		super();
		this.defaultStrategy = defaultStrategy;
		setReconcilingStrategy(defaultStrategy, String.valueOf(System.currentTimeMillis()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.Reconciler#setReconcilingStrategy(org.eclipse.jface.text.reconciler.IReconcilingStrategy, java.lang.String)
	 */
	@Override
	public void setReconcilingStrategy(IReconcilingStrategy strategy, String contentType)
	{
		super.setReconcilingStrategy(strategy, contentType);
		reconcilingStrategies.add(strategy);
	}

	/**
	 * 
	 * @param strategy
	 * @param contentTypes
	 */
	public void setReconcilingStrategy(IReconcilingStrategy strategy, Collection<String> contentTypes)
	{
		for (String contentType : contentTypes)
		{
			setReconcilingStrategy(strategy, contentType);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.Reconciler#getReconcilingStrategy(java.lang.String)
	 */
	@Override
	public IReconcilingStrategy getReconcilingStrategy(String contentType) {
		IReconcilingStrategy strategy = super.getReconcilingStrategy(contentType);
		if (strategy != null) {
			return strategy;
		}
		return defaultStrategy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#install(org.eclipse.jface.text.ITextViewer)
	 */
	@Override
	public void install(ITextViewer textViewer) {
		super.install(textViewer);
		bundleChangeReconcileTrigger = new BundleChangeReconcileTrigger(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#uninstall()
	 */
	@Override
	public void uninstall() {
		if (bundleChangeReconcileTrigger != null) {
			bundleChangeReconcileTrigger.dispose();
			bundleChangeReconcileTrigger = null;
		}
		super.uninstall();
	}

	@Override
	protected void initialProcess()
	{
		for (IReconcilingStrategy s : reconcilingStrategies)
		{
			if (s instanceof IReconcilingStrategyExtension)
			{
				((IReconcilingStrategyExtension) s).initialReconcile();
			}
		}
		fIninitalProcessDone = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#forceReconciling()
	 */
	@Override
	public void forceReconciling() {
		super.forceReconciling();
	}
}
