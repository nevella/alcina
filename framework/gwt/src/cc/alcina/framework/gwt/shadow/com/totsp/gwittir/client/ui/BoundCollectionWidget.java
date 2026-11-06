/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui;

import java.util.Collection;

import java.util.function.Function;

/**
 *
 * @author kebernet
 */
public interface BoundCollectionWidget<T, R>
		extends BoundWidget<Collection<T>> {
	public Function<T, R> getRenderer();

	public void setRenderer(Function<T, R> renderer);
}
