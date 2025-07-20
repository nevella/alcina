/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;

/**
 * FIXME - dirndl - lowish
 * 
 * <p>
 * Write a propertychangesupport where the properties are backed by a LightMap
 * (actually a lightmap wrapping class that behaves like a list but with
 * constant removal time ...mapping Listener to an integer...wait a sec, this
 * actually gets a little tricky...linked list with lookup? yrrch. wait a sec -
 * no, this is fine. Proxy the binding! So forget all this, but show an example
 * of a proxy binding)- in degenerate cases where there are 1000s of listeners
 * on a single topic, clearing the listeners if they're all from one component
 * is o(n^2) which is potentially ...nasty
 *
 * <p>
 * <ul>
 * <li>Proxy binding -
 * from(proxyObject.proxyProperty).to(many-times-repeated-prop)
 * <li>from(original.property).to(proxyObject.proxyProperty).oneWay()
 * <li>clear proxyObject.proxyProperty bindings before many-times-repeated
 */
public class LazyPropertyChangeSupport {
	public static final transient Object UNSPECIFIED_PROPERTY_CHANGE = new Object();

	private PropertyChangeSupport delegate;

	private Object sourceBean;

	public LazyPropertyChangeSupport(Object sourceBean) {
		this.sourceBean = sourceBean;
	}

	public synchronized void
			addPropertyChangeListener(PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(listener);
	}

	public synchronized void addPropertyChangeListener(
			PropertyEnum propertyName, PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(propertyName.name(), listener);
	}

	public synchronized void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(propertyName, listener);
	}

	private void ensureDelegate() {
		if (delegate == null) {
			delegate = new PropertyChangeSupport(sourceBean);
		}
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		if (delegate == null) {
			return;
		}
		this.delegate.firePropertyChange(evt);
	}

	public void firePropertyChange(PropertyEnum propertyName, Object oldValue,
			Object newValue) {
		firePropertyChange(propertyName.name(), oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		if (delegate == null || (oldValue == null && newValue == null)) {
			return;
		}
		delegate.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Taking advantage of propertychangesupport null != null - note that the
	 * old/newvalues of the propertychangeevent should !not! be read. Indicates
	 * "this object has changed - possibly below the child/field level"
	 *
	 * Normally there are other ways to skin this cat - FIXME - dirndl 1x3 -
	 * possibly remove
	 */
	public void fireUnspecifiedPropertyChange(Object propagationId) {
		fireUnspecifiedPropertyChange(null, propagationId);
	}

	/**
	 * Taking advantage of propertychangesupport null != null - note that the
	 * old/newvalues of the propertychangeevent should !not! be read. For
	 * listeners on collection properties
	 */
	public void fireUnspecifiedPropertyChange(String name) {
		fireUnspecifiedPropertyChange(name, UNSPECIFIED_PROPERTY_CHANGE);
	}

	private void fireUnspecifiedPropertyChange(String name,
			Object propagationId) {
		if (delegate == null) {
			return;
		}
		PropertyChangeEvent changeEvent = new PropertyChangeEvent(sourceBean,
				name, null, null);
		if (propagationId == null) {
			propagationId = UNSPECIFIED_PROPERTY_CHANGE;
		}
		changeEvent.setPropagationId(propagationId);
		delegate.firePropertyChange(changeEvent);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		ensureDelegate();
		return this.delegate.getPropertyChangeListeners();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (delegate == null) {
			return;
		}
		this.delegate.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		if (delegate == null) {
			return;
		}
		this.delegate.removePropertyChangeListener(propertyName, listener);
	}
}
