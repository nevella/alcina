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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author nick@alcina.cc
 * 
 */
public class IgnoreNullsPropertyChangeSupport extends PropertyChangeSupport {
	public static boolean mute_all = false;

	private boolean hasListeners = false;

	public IgnoreNullsPropertyChangeSupport(Object sourceBean) {
		super(sourceBean);
	}

	@Override
	public synchronized void addPropertyChangeListener(
			PropertyChangeListener listener) {
		hasListeners = true;
		super.addPropertyChangeListener(listener);
	}

	@Override
	public synchronized void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		hasListeners = true;
		super.addPropertyChangeListener(propertyName, listener);
	}

	/*
	 * 
	 * 
	 * @see
	 * java.beans.PropertyChangeSupport#firePropertyChange(java.lang.String,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		if (mute_all||!hasListeners || (oldValue == null && newValue == null)) {
			return;
		}
		super.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Hack - note that the old/newvalues of the propertychangeevent should
	 * !not! be read. For listeners on collection properties
	 */
	public void forceFirePropertyChange(String name) {
		this.firePropertyChange(name, false, true);
	}
}
