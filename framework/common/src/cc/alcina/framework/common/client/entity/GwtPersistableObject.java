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

package cc.alcina.framework.common.client.entity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.permissions.Permissible;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */
@Introspectable
 public class GwtPersistableObject implements SourcesPropertyChangeEvents,HasIdAndLocalId, Permissible {
	long id;
	long localId;
	public long getId() {
		return this.id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getLocalId() {
		return this.localId;
	}
	public void setLocalId(long localId) {
		this.localId = localId;
	}
	protected transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
			this);
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(propertyName,
				listener);
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		this.propertyChangeSupport.firePropertyChange(evt);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.propertyChangeSupport.getPropertyChangeListeners();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}
	/**
	 * Hack - note that the old/newvalues of the propertychangeevent should !not! be read.
	 * For listeners on collection properties 
	 */
	
	public void forceFirePropertyChange(String name) {
		this.propertyChangeSupport.firePropertyChange(name, false, true);
	}
	public AccessLevel accessLevel() {
		return AccessLevel.DEVELOPER;
	}
	public String rule() {
		return null;
	}
}
