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

import com.google.gwt.core.client.GWT;

/**
 * @author nick@alcina.cc
 * 
 */
public class MuteablePropertyChangeSupport {
	public static final transient Object UNSPECIFIED_PROPERTY_CHANGE = new Object();

	private static boolean muteAll = false;

	public static boolean isMuteAll() {
		return muteAll;
	}

	public static void setMuteAll(boolean muteAll) {
		setMuteAll(muteAll, false);
	}
	// FIXME - mvcc.4 - get rid of this - and maybe add context muting (for
	// projection) which returns a muted singletong
	//

	// ooh, singletong ... yes
	public static void setMuteAll(boolean muteAll,
			boolean initLifecycleThread) {
		if (!GWT.isClient() && !initLifecycleThread) {
			throw new RuntimeException(
					"Mute all should only be set on a single-threaded VM");
		}
		MuteablePropertyChangeSupport.muteAll = muteAll;
	}

	private PropertyChangeSupport delegate;

	private Object sourceBean;

	public MuteablePropertyChangeSupport(Object sourceBean) {
		this.sourceBean = sourceBean;
	}

	public synchronized void
			addPropertyChangeListener(PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(listener);
	}

	public synchronized void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(propertyName, listener);
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		if (isMuteAll() || delegate == null) {
			return;
		}
		this.delegate.firePropertyChange(evt);
	}

	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		if (isMuteAll() || delegate == null
				|| (oldValue == null && newValue == null)) {
			return;
		}
		delegate.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Taking advantage of propertychangesupport null != null - note that the
	 * old/newvalues of the propertychangeevent should !not! be read. Indicates
	 * "this object has changed - possibly below the child/field level"
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

	private void ensureDelegate() {
		if (delegate == null) {
			delegate = new PropertyChangeSupport(sourceBean);
		}
	}

	private void fireUnspecifiedPropertyChange(String name,
			Object propagationId) {
		if (isMuteAll() || delegate == null) {
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
}
