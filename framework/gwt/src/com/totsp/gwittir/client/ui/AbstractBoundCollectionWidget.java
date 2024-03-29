/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.totsp.gwittir.client.ui;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author kebernet
 */
public abstract class AbstractBoundCollectionWidget<T, R>
		extends AbstractBoundWidget<Collection<T>>
		implements BoundCollectionWidget<T, R> {
	private Renderer<T, R> renderer;

	/**
	 * Get the value of renderer
	 *
	 * @return the value of renderer
	 */
	@Override
	public Renderer<T, R> getRenderer() {
		return this.renderer;
	}

	/**
	 * Set the value of renderer
	 *
	 * @param newrenderer
	 *            new value of renderer
	 */
	@Override
	public void setRenderer(Renderer<T, R> newrenderer) {
		this.renderer = newrenderer;
	}

	public Collection<T> single(T object) {
		ArrayList<T> ret = new ArrayList<T>();
		ret.add(object);
		return ret;
	}
}
