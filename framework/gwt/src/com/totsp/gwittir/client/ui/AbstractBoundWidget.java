/*
 * AbstractBoundWidget.java
 *
 * Created on June 14, 2007, 9:55 AM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.totsp.gwittir.client.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;

import com.google.gwt.user.client.ui.Composite;
import com.totsp.gwittir.client.action.Action;
import com.totsp.gwittir.client.action.BindingAction;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
@Bean
public abstract class AbstractBoundWidget<T> extends Composite
		implements BoundWidget<T> {
	private Action<BoundWidget<T>> action;

	private Comparator comparator;

	private Object model;

	protected PropertyChangeSupport changes = new PropertyChangeSupport(this);

	private boolean wasDetached;

	/** Creates a new instance of AbstractBoundWidget */
	public AbstractBoundWidget() {
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		changes.addPropertyChangeListener(l);
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
		changes.addPropertyChangeListener(propertyName, l);
	}

	@Override
	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		changes.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public Action<BoundWidget<T>> getAction() {
		return action;
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	@Override
	public Object getModel() {
		return model;
	}

	@Override
	public PropertyChangeListener[] propertyChangeListeners() {
		return changes.getPropertyChangeListeners();
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		changes.removePropertyChangeListener(l);
	}

	@Override
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
		changes.removePropertyChangeListener(propertyName, l);
	}

	/*
	 * Removes any old associated action including the bindings, and also sets
	 * the new action including any bindings.
	 */
	@Override
	public void setAction(Action<BoundWidget<T>> action) {
		if (this.action != null) {
			this.cleanupAction();
		}
		this.action = action;
		// Check to see if action is not null and model has been set
		if (this.action != null && this.model != null) {
			this.setupAction();
			// If attached do the binding
			if (this.isAttached()) {
				this.activateAction();
			}
		}
	}

	@Override
	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	@Override
	public void setModel(Object model) {
		Object old = this.getModel();
		cleanupAction();
		this.model = model;
		setupAction();
		if (this.isAttached() && (this.getModel() != null)) {
			activateAction();
		}
		this.changes.firePropertyChange("model", old, model);
	}

	private void activateAction() {
		if (this.getAction() instanceof BindingAction) {
			((BindingAction<BoundWidget<T>>) getAction()).bind(this);
		}
	}

	/*
	 * Remove the bindings including the keybindings
	 */
	private void cleanupAction() {
		if (this.getAction() instanceof BindingAction
				&& (this.getModel() != null)) {
			((BindingAction<BoundWidget<T>>) getAction()).unbind(this);
		}
	}

	/*
	 * Calls the associated action with this widget to set the bindings.
	 */
	private void setupAction() {
		if (this.getAction() instanceof BindingAction) {
			((BindingAction<BoundWidget<T>>) getAction()).set(this);
		}
	}

	@Override
	protected void onAttach() {
		// Call cleanup action to remove any bindings just in case there are
		// other bindings.
		this.cleanupAction();
		if (!wasDetached) {
			this.setupAction();
		}
		wasDetached = false;
		super.onAttach();
		this.changes.firePropertyChange("attached", false, true);
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		wasDetached = true;
		this.cleanupAction();
		this.changes.firePropertyChange("attached", true, false);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		this.activateAction();
	}
}
