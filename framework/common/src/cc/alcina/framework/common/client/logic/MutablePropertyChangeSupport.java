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
public class MutablePropertyChangeSupport {
	private static boolean muteAll = false;

	private PropertyChangeSupport delegate;

	private Object sourceBean;

	public MutablePropertyChangeSupport(Object sourceBean) {
		this.sourceBean = sourceBean;
	}

	public synchronized void addPropertyChangeListener(
			PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(listener);
	}

	private void ensureDelegate() {
		if (delegate == null) {
			delegate = new PropertyChangeSupport(sourceBean);
		}
	}

	public synchronized void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		ensureDelegate();
		delegate.addPropertyChangeListener(propertyName, listener);
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
	 * Sort of a hack - taking advantage of propertychangesupport null!=null -
	 * note that the old/newvalues of the propertychangeevent should !not! be
	 * read. For listeners on collection properties
	 */
	public void fireNullPropertyChange(String name) {
		if (isMuteAll() || delegate == null) {
			return;
		}
		delegate.firePropertyChange(name, null, null);
	}

	public static void setMuteAll(boolean muteAll) {
		setMuteAll(muteAll, false);
	}

	public static void setMuteAll(boolean muteAll, boolean initLifecycleThread) {
		if (!GWT.isClient() && !initLifecycleThread) {
			throw new RuntimeException(
					"Mute all should only be set on a single-threaded VM");
		}
		MutablePropertyChangeSupport.muteAll = muteAll;
	}

	public static boolean isMuteAll() {
		return muteAll;
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if(delegate == null){
			return;
		}
		this.delegate.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		if(delegate == null){
			return;
		}
		this.delegate.removePropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		ensureDelegate();
		return this.delegate.getPropertyChangeListeners();
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		if (isMuteAll() || delegate == null) {
			return;
		}
		this.delegate.firePropertyChange(evt);
	}
}
